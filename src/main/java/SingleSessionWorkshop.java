import java.util.ArrayList;

public class SingleSessionWorkshop extends Workshop {

    private ArrayList<Attendee> attendees;
    private char session; //A or B


    public SingleSessionWorkshop(String name, String description, String location, String moderators, String presenters, int maxAttendance, char session, boolean isFreeTalk) {
        super(name, description, location, moderators, presenters, maxAttendance, isFreeTalk);
        this.session = session;
        attendees = new ArrayList<Attendee>();
    }

    public char getSession() {
        return session;
    }

    public void addAttendee(Attendee a) {
        if (a != null) {
            attendees.add(a);
        }
    }

    public ArrayList<Attendee> getAttendees() {
        return attendees;
    }

    public String toString() {
        String s = super.toString();
        s += "Session: ";
        s += session;
        s += "\nAttendees: \n";
        if (getNumberOfAttendees()>0) {
            for (Attendee a : attendees) {
                s += a.toString();
                s += "\n";
            }
        }
        else {
            s += "None";
        }
        s += "\n";
        return s;
    }

    public int getNumberOfAttendees() {
        return attendees.size();
    }
}
