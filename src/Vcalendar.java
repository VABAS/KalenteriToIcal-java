import java.util.ArrayList;

class Vcalendar {
    private String version;
    private String prodId;
    private ArrayList<Vevent> vevents = new ArrayList<>();

    // Constructor with all items assigned.
    public Vcalendar (String version, String prodId, ArrayList<Vevent> vevents) {
        this.version = version;
        this.prodId = prodId;
        this.vevents = vevents;
    }
    // Constructor without vevents.
    public Vcalendar (String version, String prodId) {
        this.version = version;
        this.prodId = prodId;
    }

    // Method to add new vevent. Returns false if add fails.
    public boolean addEvent (Vevent newEvent) {
        if (vevents.size() > 0) {
            for (Vevent event : vevents) {
                if (event.getUid() == newEvent.getUid()) {
                    return false;
                }
            }
        }
        vevents.add(newEvent);
        return true;
    }
    // Method to delete event by uid. Returns false if event cannot be found.
    public boolean deleteEvent (String uid) {
        for (Vevent event : vevents) {
            if (event.getUid() == uid) {
                vevents.remove(event);
                return true;
            }
        }
        return false;
    }
    // Method to return vcalendar as string.
    public String toString () {
        String events = "";
        if (vevents.size() > 0) {
            for (Vevent event : vevents) {
                events += event.toString();
            }
        }
        return "BEGIN:VCALENDAR\r\n"
               + "VERSION:" + version + "\r\n"
               + "PRODID:" + prodId + "\r\n"
               + events + "END:VCALENDAR\r\n";
    }
}
