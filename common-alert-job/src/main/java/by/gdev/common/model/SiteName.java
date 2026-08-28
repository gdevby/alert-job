package by.gdev.common.model;

import java.util.Arrays;

public enum SiteName {
    FLRU(1, "fl.ru"),
    FREELANCERU(3, "freelance.ru"),
    WEBLANCER(4, "weblancer.net"),
    FREELANCEHUNT(5, "freelancehunt.com"),
    YOUDO(6, "youdo.com"),
    KWORK(7, "kwork.ru"),
    FREELANCER(8, "freelancer.com"),
    TRUELANCER(9, "truelancer.com"),
    PEOPLEPERHOUR(10, "peopleperhour.com"),
    WORKSPACE(11, "workspace.com"),
    WORKANA(12, "workana.com");

    private long id;
    private String domain;

    SiteName(long id, String domain) {
        this.id = id;
        this.domain = domain;
    }

    public long getId() { return id; }
    public String getDomain() { return domain; }

    public static SiteName fromId(Long id) {
        return Arrays.stream(values())
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown site id: " + id));
    }
}