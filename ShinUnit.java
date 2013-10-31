/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Michael
 */
public class ShinUnit {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       SMStest();
    }

    public static void SMStest() {
        SMS a = new SMS("8177142343", "Hello World", "8:41 PM");
        System.out.println(a);
        SMS b = new SMS("<div class=\"gc-message-sms-row\">" + "\n"
                + "<span class=\"gc-message-sms-from\">" + "\n"
                + "+18179170224:" + "\n"
                + "</span>" + "\n"
                + "<span class=\"gc-message-sms-text\">shinvoice" + "\n"
                + "Open :)</span>" + "\n"
                + "<span class=\"gc-message-sms-time\">" + "\n"
                + "7:58 PM" + "\n"
                + "</span>" + "\n"
                + "</div>");
        System.out.println(b);
    }
}
