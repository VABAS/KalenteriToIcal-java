class Vevent {
    // Member variables.
    private String uid;
    private String dtStamp;
    private String dtStart;
    private String dtEnd;
    private String summary;
    private String location;
    private String description;

    // Properties.
    public String getUid () {
        return uid;
    }
    // Detailed constructor. All items assigned.
    public Vevent (String uid, String dtStamp, String dtStart,  String dtEnd,
                   String summary, String location, String description) {
        this.uid = uid;
        this.dtStamp = dtStamp;
        this.dtStart = dtStart;
        this.dtEnd = dtEnd;
        this.summary = summary;
        this.location = location;
        this.description = description;
    }
    // Constructor without description.
    public Vevent (String uid, String dtStamp, String dtStart,  String dtEnd,
                   String summary, String location) {
        this(uid, dtStamp, dtStart, dtEnd, summary, location, "");
    }
    // Constructor without location and description.
    public Vevent (String uid, String dtStamp, String dtStart,  String dtEnd,
                   String summary) {
        this(uid, dtStamp, dtStart, dtEnd, summary, "");
    }

    // Method getString() returns vevent as string.
    public String toString () {
        return "BEGIN:VEVENT\r\n" +
               "UID:" + uid + "\r\n" +
               "DTSTAMP:" + dtStamp + "\r\n" +
               "DTSTART:" + dtStart + "\r\n" +
               "DTEND:" + dtEnd + "\r\n" +
               "SUMMARY:" + summary + "\r\n" +
               "LOCATION:" + location + "\r\n" +
               "DESCRIPTION:" + description + "\r\n" +
               "END:VEVENT\r\n";
    }
}
