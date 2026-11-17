package com.barangay.bdms.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateFormatUtils {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.US);

    private DateFormatUtils() {
    }

    @NonNull
    public static String formatDateTime(@Nullable OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        return dateTime.atZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY_FORMATTER);
    }
}
