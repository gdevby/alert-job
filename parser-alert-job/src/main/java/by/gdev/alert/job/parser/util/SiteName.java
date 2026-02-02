package by.gdev.alert.job.parser.util;

public enum SiteName {
    FLRU(1),
    //HABR(2), not in use anymore
    FREELANCERU(3),
    WEBLANCER(4),
    FREELANCEHUNT(5),
    YOUDO(6),
    KWORK(7),
    FREELANCER(8),
    TRUELANCER(9),
    PEOPLEPERHOUR(10),
    WORKSPACE(11),
    WORKANA(12),
    KWORKCOM(13);

    private long id;

    SiteName(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
