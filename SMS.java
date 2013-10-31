
public class SMS {

    String from;
    String text;
    String time;

    public SMS(String from, String text, String time) {
        this.from = from;
        this.text = text;
        this.time = time;
    }

    public SMS(String raw) {
        String[] lines = raw.split("\n");
        if (!lines[0].equals("<div class=\"gc-message-sms-row\">") && !lines[0].contains("gc-message")) {
            System.err.println(raw);
            throw new RuntimeException("Invalid Raw SMS Data");
        }
        String f = retrieve(raw, "span", "gc-message-sms-from");
        String t = retrieve(raw, "span", "gc-message-sms-text");
        String ti =retrieve(raw, "span", "gc-message-sms-time");
        this.from = f;
        this.text = t;
        this.time = ti;
    }

    private String retrieve(String data, String tag, String category) {
        String[] lines = data.split("\n");
        boolean wait = false;
        int begin = -1;
        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];
            if (s.contains("<" + tag)) {
                if (s.contains(category)) {
                    wait = true;
                }
            }
            if (wait) {
                if (s.contains(">")) {
                    wait = false;
                    begin = count + s.indexOf(">");
                }
            }
            if (begin != -1) {
                return trimSpace(data.substring(begin + 1, data.indexOf("</" + tag + ">", begin + 1)));
            }
            count += s.length() + 1;
        }
        throw new RuntimeException("Tag: " + tag + " or Class: " + category + " Not Found.");
    }

    public String trimSpace(String value) {
        String[] lines = value.split("\n");
        String build = "";
        for (int i = 0; i < lines.length; i++) {
            String s = lines[i];
            if (!s.matches("\\s+") && !s.equals("")) {
                int spi = 0;
                while (spi != s.length() && (s.charAt(spi) + "").matches("\\s")) {
                    spi++;
                }
                if (spi != s.length()) {
                    build += s.substring(spi);
                }
                if (i < lines.length - 1) {
                    build += "\n";
                }
            }
        }

        return build;
    }
    public String cleanRawTime(String value){
        return value.substring(2);
    }
    @Override
    public String toString() {
        String s = "";
        s += "FROM:\n" + this.from + "\n\n";
        s += "TEXT:\n" + this.text + "\n\n";
        s += "TIME:\n" + this.time + "\n\n";
        return s;
    }
	public boolean equals(Object b) {
		return this.text.equals(((SMS)b).text) && this.time.equals(((SMS)b).time);
	}
}
