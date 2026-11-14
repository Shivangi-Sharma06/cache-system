package com.cse309.cache.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter producing a clean, single-line log format:
 * [TIMESTAMP] [LEVEL] [THREAD] LOGGER: MESSAGE
 */
public final class SingleLineFormatter extends Formatter {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    @Override
    public String format(LogRecord record) {
        String timestamp = TIME_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
        String level = record.getLevel().getName();
        String threadName = Thread.currentThread().getName();
        String loggerName = record.getLoggerName() != null ? record.getLoggerName() : "App";

        String message = formatMessage(record);
        StringBuilder sb = new StringBuilder(128);
        sb.append('[')
          .append(timestamp)
          .append("] [")
          .append(level)
          .append("] [")
          .append(threadName)
          .append("] ")
          .append(loggerName)
          .append(": ")
          .append(message)
          .append(System.lineSeparator());

        if (record.getThrown() != null) {
            sb.append("  Exception: ")
              .append(record.getThrown().toString())
              .append(System.lineSeparator());
        }
        return sb.toString();
    }
}
