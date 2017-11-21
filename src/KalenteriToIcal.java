import java.util.ArrayList;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

class KalenteriToIcal {
    // Usage string.
    static String usage = "KalenteriToIcal address|file.txt file.ics"
                          + "\nInput address/file and output file must always "
                          + "be present and in same order (input first)."
                          + "\n\nAdditional parameters:"
                          + "\n--nodup"
                          + "\n  Do not import events with exactly same name "
                          + "and start time (basically the same event)."
                          + "\n--noask"
                          + "\n  Import all courses found without asking."
                          + "\n--privacy [PUBLIC|CONFIDENTIAL|PRIVATE]"
                          + "\n  Specify the value of vevent CLASS-field. If "
                          + "not specified defaults to CONFIDENTIAL."
                          + "\n--fromfile"
                          + "\n  Get course urls from newline separated text "
                          + "file instead. File location is provided in place "
                          + "of the address. All courses specified in this "
                          + "file are imported without asking for "
                          + "confirmation.";

    public static void main (String args[]) {
        if (args.length < 2) {
            System.out.println("Not enough arguments!");
            System.out.println("Usage: " + usage);
            System.exit(0);
        }

        // Saving address and file to their own variables.
        String address = args[0];
        String file = args[1];
        // Privacy string. Defaults to confidential.
        String veventPrivacy = "CONFIDENTIAL";

        // Fromfile status.
        boolean fromFile = false;

        // Initializing nodup and noask statuses to false here.
        boolean nodup = false;
        boolean noask = false;

        // Checking for nodup and noask statuses from args.
        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "eimon":
                case "--nodup":
                    nodup = true;
                    break;
                case "eikys":
                case "--noask":
                    noask = true;
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
                        System.out.println("Privacy field value was not specified.");
                        System.exit(0);
                    }
                    if (privacy.equals("PUBLIC") ||
                        privacy.equals("CONFIDENTIAL") ||
                        privacy.equals("PRIVATE")) {
                        veventPrivacy = privacy;
                        }
                    else {
                        System.out.println("Privacy field value was invalid."
                                           + "Valid values are PUBLIC, "
                                           + "CONFIDENTIAL and PRIVATE");
                        System.exit(0);
                    }
                    break;
                default:
                    System.out.println("Unknown argument " + args[i] + ". Ignoring.");
                    break;
            }
        }

        //String[] links;
        ArrayList<String[]> links = new ArrayList<String[]>();

        // Deciding whether to fetch links from asio or from local file.
        if (fromFile) {
            System.out.println("Fetching links from file '" + address + "'");
            BufferedReader inFile = null;
            try {
                inFile = new BufferedReader(new FileReader(address));
                String line;
                while ((line = inFile.readLine()) != null) {
                    if (line.length() <= 0) {
                        continue;
                    }
                    links.add(new String[]{"", line});
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
            System.out.println("Fetching links from asio.");
            // Finding out the individual course timetable links.
            links = AsioParse.extractLinks(address);
        }
        // Contructing vcalendar object. No vevents here, adding them later.
        Vcalendar vcalendar = new Vcalendar("2.0", "sikkela");

        // Fetching course timetables and adding fetched vevents to vcalendar
        // object.
        for (String[] link : links) {
          // Asking for confirmation before importing if noask is not set and if
          // not getting links from file.
          if (!noask && !fromFile) {
            boolean importCourse;
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Do you want to import course \""
                                 + link[0] + "\"? [Y/n]: ");
                String confirmation = scanner.next().toLowerCase();
                if (confirmation.equals("y") || confirmation.equals("k")) {
                  importCourse = true;
                  break;
                }
                else if (confirmation.equals("n") || confirmation.equals("e")) {
                  importCourse = false;
                  break;
                }
                else {
                    System.out.println("Answer Y/n!");
                }
            }
            if (!importCourse) {
              continue;
            }
          }

          Vevent[] vevents = AsioParse.fetchCourseTimetable(link[1], nodup, veventPrivacy);
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
