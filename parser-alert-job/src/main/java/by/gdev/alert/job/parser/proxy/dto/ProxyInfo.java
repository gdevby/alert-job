package by.gdev.alert.job.parser.proxy.dto;

import by.gdev.alert.job.parser.proxy.db.ProxyType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxyInfo {
    @JsonProperty("ip")
    private String ip;

    @JsonProperty("port")
    private int port;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("type")
    private ProxyType type;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("errorCount")
    private int errorCount;

    @JsonProperty("lastChecked")
    private long lastChecked;
}

