import java.util.ArrayList;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;

class KalenteriToIcal {
    // Usage string.
    static String usage = "KalenteriToIcal osoite tiedosto.ics [eimon] [eikys]"
                          + "\nPakolliset parametrit ovat syöttö-"
                          + "osoite/tiedosto sekä tulostustiedosto jotka ovat "
                          + "aina ensimmäinen ja toinen parametri."
                          + "\n\nLisäasetukset:"
                          + "\n--privacy [PUBLIC|CONFIDENTIAL|PRIVATE]"
                          + "\n  Määrittää kaikkien tapahtumien "
                          + "yksityisyysarvon (ical CLASS-kenttä)."
                          + "\n--fromfile"
                          + "\n  Määrittää, että kurssiosoitteet haetaan "
                          + "tiedostosta. Tiedoston sijainti kirjoitetaan "
                          + "osoitteen paikalle.";

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
        
        // Fromfile status.
        boolean fromFile = false;

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
                case "--fromfile":
                    fromFile = true;
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
        
        //String[] links;
        ArrayList<String> links = new ArrayList<String>();
        
        // Deciding whether to fetch links from asio or from local file.
        if (fromFile) {
            System.out.println("Haetaan linkit tiedostosta '" + address + "'");
            BufferedReader inFile = null;
            try {
                inFile = new BufferedReader(new FileReader(address));
                String line;
                while ((line = inFile.readLine()) != null) {
                    if (line.length() <= 0) {
                        continue;
                    }
                    links.add(line);
                }
            }
            catch (java.io.IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    if (inFile != null) {
                        inFile.close();
                    }
                }
                catch (java.io.IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        else {
            System.out.println("Haetaan linkit asiosta.");
            // Finding out the individual course timetable links.
            links = new ArrayList<>(Arrays.asList(AsioParse.extractLinks(address, eikys)));
        }
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
