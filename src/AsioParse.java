import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.util.ArrayList;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

class AsioParse {
    private static String doHttpRequest (String address) {
        String result = "";
        try {
            HttpURLConnection connection = null;
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();
            System.out.println("Connecting to server...");
            connection.setRequestMethod("GET");
            System.out.println("Reading response...");
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "ISO-8859-1"));
            String line;
            while ((line = rd.readLine()) != null) {
                result += line + "\n";
            }
            rd.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // Fetches links from page provided and returns them as array.
    public static ArrayList<String[]> extractLinks (String address) {
        ArrayList<String[]> links = new ArrayList<String[]>();
        String html = doHttpRequest(address);
        Document doc = Jsoup.parse(html);
        Elements k = doc.select("a[href=javascript:void(null);]");
        // Splitting and extracting the link part only.
        for (Element kk : k) {
          links.add(
            new String[]{
              kk.text(),
              "https://amp.jamk.fi/asio_v16/" + kk.attr("onclick").split("'")[1].replace("../", "")
            }
          );
        }
        return links;
    }

    // Fetches course timetable and returns it as array of vevents.
    public static Vevent[] fetchCourseTimetable (String address, boolean dontDuplicate, String veventPrivacy) {
        ArrayList<ArrayList<String>> properties = new ArrayList<>();
        String html = doHttpRequest(address);
        Document doc = Jsoup.parse(html.replace("\n","\\n"));
        Elements k = doc.select("tr[bgcolor=#e7e7e7]");
        System.out.println("Found " + k.size() + " occurences.");
        for (Element kk : k) {
            ArrayList<String> temp = new ArrayList<String>();
            for (Element kkk : kk.select("td")) {
                temp.add(kkk.text().replace("\\n ","\\n"));
            }

            // Removing last item which is always empty.
            temp.remove(temp.size() - 1);
            properties.add(temp);
        }
        ArrayList<Vevent> vevents = new ArrayList<>();
        ArrayList<String> veventIds = new ArrayList<>();

        for (ArrayList<String> property : properties) {
            String[] date = property.toArray()[0].toString().split("\\xa0")[1].split("\\.");
            String startTime = (String)property.toArray()[1].toString().split(" - ")[0].replace(":", "");
            String endTime = (String)property.toArray()[1].toString().split(" - ")[1].replace(":", "");
            String room = (String)property.toArray()[2];
            String targetGroup = (String)property.toArray()[3];
            String teacher = (String)property.toArray()[4];
            String courseName = (String)property.toArray()[5];

            // Get and format current datetime to ical format. (YYYYMMDDTHHMMSS)
            Date dateNow = new Date();
            SimpleDateFormat ymd = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat hms = new SimpleDateFormat("HHmmss");
            String dateNowymd = ymd.format(dateNow);
            String dateNowhms = hms.format(dateNow);

            // Processing additional.
            String additional = "";
            if (!dontDuplicate) {
                // Additional is size of vevents-arraylist. Added with one to
                // get rid of zero.
                additional = "-" + Integer.toString(vevents.size() + 1);
            }

            // Constructing veventid.
            String veventId = courseName.replace(" ", "_") + "@" + date[2]
                              + date[1] + date[0] + "T" + startTime
                              + additional;

            // Not doing add if identical id already exists.
            if (!veventIds.contains(veventId)) {
                // Adding veventId to id-array for identification later.
                veventIds.add(veventId);
                // Contructin vevent object and adding it to arraylist.
                vevents.add(new Vevent(veventId,
                                       dateNowymd + "T" + dateNowhms,
                                       date[2] + date[1] + date[0] + "T" + startTime + "00",
                                       date[2] + date[1] + date[0] + "T" + endTime + "00",
                                       courseName,
                                       room,
                                       veventPrivacy,
                                       teacher));
            }
            else {
                System.out.println("Event '" + courseName + "' was not "
                                   + "imported. Reason: duplicate ID.");
            }
        }

        return vevents.toArray(new Vevent[vevents.size()]);
    }
}
