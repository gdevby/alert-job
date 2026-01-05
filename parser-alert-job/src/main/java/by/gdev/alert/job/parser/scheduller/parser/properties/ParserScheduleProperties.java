package by.gdev.alert.job.parser.scheduller.parser.properties;

public class ParserScheduleProperties {
    private long initialDelaySeconds;
    private long fixedDelaySeconds;

    public ParserScheduleProperties(long initialDelaySeconds, long fixedDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
        this.fixedDelaySeconds = fixedDelaySeconds;
    }

    public long getInitialDelayMillis() {
        return initialDelaySeconds * 1000;
    }

    public long getFixedDelayMillis() {
        return fixedDelaySeconds * 1000;
    }

    public long getInitialDelaySeconds() { return initialDelaySeconds; }

    public long getFixedDelaySeconds() { return fixedDelaySeconds; }
}



