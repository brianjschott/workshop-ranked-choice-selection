public abstract class Workshop {
    private int id;
    private String name;
    private String description;
    private String url;
    private String[] moderators;
    private String[] presenters;
    private enum type { TALK, SEMINAR  }

    public Workshop(int id, String name, String description, String url, String[] moderators, String[] presenters) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.url = url;
        this.moderators = moderators;
        this.presenters = presenters;
    }
}
