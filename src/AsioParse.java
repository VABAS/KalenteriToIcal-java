import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.util.ArrayList;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

class AsioParse {
    private static final String ENCODING = "ISO-8859-1";
    public static boolean authentication (String userid, String password) {
      return authentication(userid, password, true);
    }
    public static boolean authentication (String userid, String password, boolean printInfos) {
      try {
        if (printInfos) {
          System.out.println("Authenticating...");
        }
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        byte[] postData = ("asiolg[u]=" + userid + "&asiolg[p]=" + password).getBytes();
        HttpURLConnection connection = null;
        URL url = new URL("https://amp.jamk.fi/asio/kalenterit2/index.php");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
           wr.write(postData);
           wr.close();
        }
        if (connection.getResponseCode() != 200) {
          throw new Exception ();
        }
        String html = "";
        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), ENCODING));
        String line;
        while ((line = rd.readLine()) != null) {
            html += line + "\n";
        }
        rd.close();
        Document doc = Jsoup.parse(html.replace("\n","\\n"));
        if (doc.title().equals("Asio® -- Login")) {
          return false;
        }
      }
      catch (Exception e) {
        if (printInfos) e.printStackTrace();
      }
      return true;
    }
    private static String doHttpRequest (String address) {
        return doHttpRequest(address, true);
    }
    private static String doHttpRequest (String address, boolean printInfos) {
        String result = "";
        try {
            HttpURLConnection connection = null;
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();
            if (printInfos) System.out.println("Connecting to server...");
            connection.setRequestMethod("GET");
            if (printInfos) System.out.println("Reading response...");
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), ENCODING));
            String line;
            while ((line = rd.readLine()) != null) {
                result += line + "\n";
            }
            rd.close();
        }
        catch (Exception e) {
            if (printInfos) e.printStackTrace();
        }
        return result;
    }

    // Fetches links from page provided and returns them as array.
    public static ArrayList<String[]> extractLinks (String address) {
        return extractLinks(address, true);
    }
    public static ArrayList<String[]> extractLinks (String address, boolean printInfos) {
        ArrayList<String[]> links = new ArrayList<String[]>();
        String html = doHttpRequest(address, printInfos);
        Document doc = Jsoup.parse(html);
        Elements k = doc.select("a[href=javascript:void(null);]");
        // Splitting and extracting the link part only.
        for (Element kk : k) {
          // Checing that links is valid reservation timetable link (should
          // start with "kokvar").
          if (!kk.attr("onclick").split("'")[1].replace("../", "").split("/")[0].equals("kokvar")) {
            continue;
          }
          links.add(
            new String[]{
              kk.text(),
              "https://amp.jamk.fi/asio/" + kk.attr("onclick").split("'")[1].replace("../", "")
            }
          );
        }
        return links;
    }

    // Fetches course timetable and returns it as array of vevents.
    public static Vevent[] fetchCourseTimetable (String address, boolean dontDuplicate, String veventPrivacy) {
      return fetchCourseTimetable(address, dontDuplicate, veventPrivacy, true);
    }
    public static Vevent[] fetchCourseTimetable (String address, boolean dontDuplicate, String veventPrivacy, boolean printInfos) {
        ArrayList<ArrayList<String>> properties = new ArrayList<>();
        String html = doHttpRequest(address, printInfos);
        Document doc = Jsoup.parse(html.replace("\n","\\n"));
        Elements k = doc.select("tr[bgcolor=#e7e7e7]");
        if (printInfos) System.out.println("Found " + k.size() + " occurences.");
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
            String[] date = property.toArray()[0].toString().split(" ")[1].split("\\.");
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
                if (printInfos) {
                  System.out.println("Event '" + courseName + "' was not "
                                     + "imported. Reason: duplicate ID.");
                }
            }
        }

        return vevents.toArray(new Vevent[vevents.size()]);
    }
}
