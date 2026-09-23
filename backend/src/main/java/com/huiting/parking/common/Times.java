package com.huiting.parking.common;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class Times {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Times() {}

    public static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static String fmtBj(LocalDateTime utc) {
        if (utc == null) return "--";
        return utc.plusHours(8).format(FMT);
    }
}
