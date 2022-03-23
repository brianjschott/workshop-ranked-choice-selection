public class WorkshopFactory {

    public WorkshopFactory() { }

    public Workshop makeWorkshop(int id, String name, String description, String url, String moderators, String presenters, int maxAttendance, String sessions, boolean isFreeTalk) {
        if (sessions.equals("A") || sessions.equals("B")) {
            return new SingleSessionWorkshop(id, name, description, url, moderators, presenters, maxAttendance, sessions.charAt(0), isFreeTalk);
        }
        else if (sessions.equals("AB")){
            return new DoubleSessionWorkshop(id, name, description, url, moderators, presenters, maxAttendance, isFreeTalk);
        }
        return null;
    }

}
