package by.gdev.common.model;

import java.util.Arrays;

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
    WORKANA(12);

    private long id;

    SiteName(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static SiteName fromId(Long id) {
        return Arrays.stream(values())
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown site id: " + id));
    }

}
