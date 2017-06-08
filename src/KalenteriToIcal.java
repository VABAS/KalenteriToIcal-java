import java.util.ArrayList;
import java.util.Arrays;
import java.io.PrintWriter;

class KalenteriToIcal {
    // Usage string.
    static String usage = "KalenteriToIcal osoite tiedosto.ics [eimon] [eikys]";

    public static void main (String args[]) {
        if (args.length < 2) {
            System.out.println("Ei tarpeeksi argumentteja!");
            System.out.println("Käyttö: " + usage);
            System.exit(0);
        }

        // Saving address and file to their own variables.
        String address = args[0];
        String file = args[1];
        // Privacy string. Defaults to confidential.
        String veventPrivacy = "CONFIDENTIAL";

        // Initializing eimon and eikys statuses to false here.
        boolean eimon = false;
        boolean eikys = false;

        // Checking for eimon and eikys statuses from args.
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "eimon":
                    eimon = true;
                    break;
                case "eikys":
                    eikys = true;
                    break;
                case "--privacy":
                    String privacy = "";
                    try {
                        privacy = args[i + 1].toUpperCase();
                    }
                    catch (IndexOutOfBoundsException e) {
                        System.out.println("Yksityisyysasetusta ei ole annettu.");
                        System.exit(0);
                    }
                    if (privacy.equals("PUBLIC") ||
                        privacy.equals("CONFIDENTIAL") ||
                        privacy.equals("PRIVATE")) {
                        veventPrivacy = privacy;
                        }
                    else {
                        System.out.println("Yksityisyysasetus ei kelpaa.");
                        System.exit(0);
                    }
                    break;
            }
        }
        
        // Finding out the individual course timetable links.
        String[] links = AsioParse.extractLinks(address, eikys);
        
        // Contructing vcalendar object. Now vevents here, adding them later.
        Vcalendar vcalendar = new Vcalendar("2.0", "sikkela");
        
        // Fetching course timetables and adding fetched vevents to vcalendar
        // object.
        for (String link : links) {
            Vevent[] vevents = AsioParse.fetchCourseTimetable(link, eimon, veventPrivacy);
            for (Vevent vevent : vevents) {
                vcalendar.addEvent(vevent);
            }
        }
        
        // Write result to file.
        try {
            PrintWriter out = new PrintWriter(file);
            out.write(vcalendar.toString());
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
}
