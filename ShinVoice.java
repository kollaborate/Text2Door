
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinDirection;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.trigger.GpioCallbackTrigger;
import com.pi4j.io.gpio.trigger.GpioPulseStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSetStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSyncStateTrigger;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.event.PinEventType;
import com.techventus.server.voice.Voice;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ShinVoice {

    public static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + "/config.sv";
    public static final String LOG_FILE_PATH = System.getProperty("user.dir") + "/log.sv";
    public static final String OUTPUT_LOG_FILE_PATH = System.getProperty("user.dir") + "/olog.sv";
    public static final Calendar time = Calendar.getInstance();
    public static final Set<String> LOGS = new TreeSet<String>();
    public static final GpioController gpio = GpioFactory.getInstance();
    public static GpioPinDigitalOutput pinout = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Output Pin", PinState.LOW);
    public static PrintStream out;
    public static Voice voice;
    public static FileWriter w;
    public static Map<String, String> cfg = new TreeMap<String, String>();
    public static File cfgfile;
    public static File logfile;
    public static PinThread thread;

    public static void switchOut() throws Exception {
        out = System.out;
        System.setOut(new PrintStream(OUTPUT_LOG_FILE_PATH));
    }

    public static void main(String[] args) {

        try {
            cfgfile = new File(CONFIG_FILE_PATH);
            logfile = new File(LOG_FILE_PATH);
            makeLog();
            parseFile();
            switchOut();
            String user = Security.decode(cfg.get("#KEY"), cfg.get("#USER"));
            String pass = Security.encode(cfg.get("#KEY"), cfg.get("#PASS"));
            voice = new Voice(user, pass);
            mainLoop();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<SMS> getSMS(String data) {
        List<String> list = retrieve(data, "div", "gc-message-sms-row");
        List<SMS> SMSlist = new ArrayList<SMS>();
        for (String s : list) {
            SMSlist.add(new SMS(s));
        }
        return SMSlist;
    }

    public static int lineIndex(String[] lines, String line, int index) {
        for (int i = index; i < lines.length; i++) {
            if (lines[i].contains(line)) {
                return i;
            }
        }
        return -1;
    }

    public static int lineIndex(String[] lines, String line) {
        return lineIndex(lines, line, 0);
    }

    private static List<String> retrieve(String data, String tag, String category) {
        int index = 0;
        String[] lines = data.split("\n");
        List<String> list = new ArrayList<String>();
        _while:
        while (lineIndex(lines, "<" + tag + " class=\"" + category + "\">", index) != -1) {
            for (int i = index; i < lines.length; i++) {
                String s = lines[i];
                if (s.equals("<" + tag + " class=\"gc-message-sms-row\">") && lines[i + 1].equals("<span class=\"gc-message-sms-from\">")) {
                    String build = "";
                    for (int j = i; j < lines.length; j++) {
                        if (!lines[j].equals("</" + tag + ">")) {
                            build += lines[j] + "\n";
                        } else {
                            break;
                        }
                    }
                    list.add(build);
                    index = i + 1;
                }
            }
        }
        if (!list.isEmpty()) {
            return list;
        }
        throw new RuntimeException("Tag: " + tag + " or Class: " + category + " Not Found.");
    }

    public static void mainLoop() throws Exception {
        int cycles = 0;
        List<SMS> messages = getSMS(trimSpace(voice.getUnreadSMS()));
        while (true) {
            cycles++;
            out.println("Cycle: " + cycles);
            List<SMS> nmessages = getSMS(trimSpace(voice.getUnreadSMS()));
	    List<SMS> newmessages = new ArrayList<SMS>();
            if (!nmessages.equals(messages)) {
		for(int i = 0; i<nmessages.size(); i++){
			if(!messages.contains(nmessages.get(i))){
				newmessages.add(nmessages.get(i));
			}
		}
                for (int i = 0; i < newmessages.size(); i++) {
			out.println("Message Received: " + newmessages.get(i).text);
                    parseSMS(newmessages.get(i));
                }
                messages = nmessages;
            }
            Thread.sleep(5000);
            if (cycles == 120) {
                voice.login();
                out.println("Relogged at " + getFullTime());
            }
        }
    }

    public static void parseSMS(SMS sms) {
        if (sms.text.contains(cfg.get("#ENTRY_PASS"))) {
            if (sms.text.contains(cfg.get("#UNLOCK_CODE"))) {
                writeToPin(0, 1, Integer.parseInt(cfg.get("#UNLOCK_TIME")));
				log("Entry by " + cfg.get("#UNLOCK_CODE"));
            }
            if (sms.text.contains(cfg.get("#LOCK_CODE"))) {
                writeToPin(0, 0, Integer.parseInt(cfg.get("#UNLOCK_TIME")));
            }
        }
    }

    public static void writeToPin(int pin, int state, int time) {
	out.println("Code Received: " + pin + " " + state + " " + time);
	try{
        if(pin == 0){
		if(state == 1){
			pinout.high();
			Thread.sleep(time * 1000);
		}
		if(state == 0){
			pinout.low();
			Thread.sleep(time * 1000);
		}
	}
	}catch(Exception e){
	}
	/*
	switch (pin) {
            case 0:
                switch (state) {
                    case 0:
                        if (thread != null) {
                            thread.go = false;
                            try {
                                Thread.sleep(3000);
                            } catch (Exception e) {
                            }
                            thread = null;
                        }
                    case 1:
                        if (thread == null) {
                            thread = new PinThread(pinout, time);
                        } else {
                            thread.remaining += time;
                        }
                }
        }
	*/
    }

    public static void makeLog() throws Exception {
        String currentlog = "";
        if (!logfile.exists()) {
            logfile.createNewFile();
            currentlog = "";
        } else {
            BufferedReader r = new BufferedReader(new FileReader(logfile));
            String s;
            currentlog = "";
            while ((s = r.readLine()) != null) {
                currentlog += s + "\n";
            }
        }
        w = new FileWriter(logfile);
        log(currentlog);
        log("-----------------------------");
        log("NEW SESSION AT :" + getFullTime());

    }

    public static void parseFile() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(cfgfile));
        ArrayList<String> params = new ArrayList<String>();
        String line;
        for (line = reader.readLine(); line != null; line = reader.readLine()) {
            params.add(line);
        }
        for (int i = 0; i < params.size(); i++) {
            String[] tmp = params.get(i).split(" ");
            String key = tmp[0];
            String value = tmp[1].substring(1, tmp[1].length() - 1);
            cfg.put(key, value);
        }
    }

    public static boolean processTimeLine(String s) {
        s = s.replaceAll(" {2,}", "");
        String[] params = s.split(":");
        int hour = Integer.parseInt(params[0]);
        if (hour == getHour()) {
            int minute = Integer.parseInt(params[1].substring(0, 2), 10);
            if (minute >= getMinute() - 2 && minute <= getMinute() + 2) {
                return true;
            }
        }
        return false;
    }

    public static void log(String s) throws IOException {
        w.write(s);
        w.write("\n");
        w.flush();
    }

    public static String trimSpace(String value) {
        String[] lines = value.split("\n");
        String build = "";
        for (String s : lines) {
            if (!s.matches("\\s+")) {
                int spi = 0;
                while (spi != s.length() && (s.charAt(spi) + "").matches("\\s")) {
                    spi++;
                }
                if (spi != s.length()) {
                    build += s.substring(spi) + "\n";
                }
            }
        }

        return build;
    }

    public static String clearTags(String value) {
        int oc = 0;
        int si = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') {
                oc++;
                if (oc == 1) {
                    si = i;
                }
            }
            if (c == '>') {
                oc--;
                if (oc == 0) {
                    value = value.substring(0, si) + value.substring(i + 1);
                    i = si;
                } else if (oc == -1) {
                    oc = 0;
                }
            }
        }
        return value;
    }

    public static String getFullTime() {
        String t = (time.get(Calendar.MONTH) + 1) + "/" + time.get(Calendar.DAY_OF_MONTH) + " " + getHourMinuteTime();
        return t;
    }

    public static String getHourMinuteTime() {
        String t = String.format("%d:%02d %s", time.get(Calendar.HOUR), time.get(Calendar.MINUTE), (time.get(Calendar.AM_PM) == 1) ? "PM" : "AM");
        return t;
    }

    public static int getHour() {
        return time.get(Calendar.HOUR);
    }

    public static int getMinute() {
        return time.get(Calendar.MINUTE);


    }
}

class PinThread implements Runnable {

    boolean go;
    int remaining;
    GpioPinDigitalOutput pin;

    public PinThread(GpioPinDigitalOutput pin, int delay) {
        Thread t = new Thread(this);
        this.pin = pin;
        this.remaining = delay;
        go = true;
        t.start();
    }

    @Override
    public void run() {
        pin.high();
        while (go && remaining > 0) {
            try {
                Thread.sleep(5000);
                remaining -= 5;
            } catch (Exception e) {
            }
        }
        pin.low();
    }
}
