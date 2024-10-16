package by.gdev.alert.job.parser.util;

public enum SiteName {
    FLRU(1),
    HABR(2),
    FREELANCERU(3),
    WEBLANCER(4),
    FREELANCEHUNT(5),
    YOUDO(6),
    KWORK(7),
    FREELANCER(8);

    private long id;

    SiteName(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
