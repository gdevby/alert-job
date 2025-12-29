package by.gdev.alert.job.parser.util.cleanup;

import java.time.Duration;

public class RetentionParser {
    public static Duration parse(String value) {
        if (value.endsWith("d")) {
            long days = Long.parseLong(value.replace("d", ""));
            return Duration.ofDays(days);
        } else if (value.endsWith("m")) {
            long months = Long.parseLong(value.replace("m", ""));
            // месяцев нет в Duration, можно перевести в дни условно (30 * n)
            return Duration.ofDays(months * 30);
        } else if (value.endsWith("s")) {
            long seconds = Long.parseLong(value.replace("s", ""));
            return Duration.ofSeconds(seconds);
        } else if (value.endsWith("h")) {
            long hours = Long.parseLong(value.replace("h", ""));
            return Duration.ofHours(hours);
        }
        throw new IllegalArgumentException("Unknown retention format: " + value);
    }
}

