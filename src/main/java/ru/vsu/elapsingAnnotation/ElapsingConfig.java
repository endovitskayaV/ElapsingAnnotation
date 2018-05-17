package ru.vsu.elapsingAnnotation;

public class ElapsingConfig {
    private long maxElapsed;
    private String messageFormat;
    private String overtimeMessageFormat;
    private Loggable loggable;

    private static ElapsingConfig instance;

    private ElapsingConfig() {
        maxElapsed = Long.MAX_VALUE;
        messageFormat = "%s";
        overtimeMessageFormat = "%s";
        loggable = new LoggableImpl();
    }

    public static ElapsingConfig getInstance() {
        if (instance == null) {
            instance = new ElapsingConfig();
        }
        return instance;
    }

    public ElapsingConfig setMaxElapsed(long maxElapsed) {
        if (maxElapsed <= 0) throw new IllegalArgumentException("Invalid maxElapsed");
        this.maxElapsed = maxElapsed;
        return this;
    }


    public ElapsingConfig setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
        return this;
    }

    public ElapsingConfig setOvertimeMessageFormat(String overtimeMessageFormat) {
        this.overtimeMessageFormat = overtimeMessageFormat;
        return this;
    }

    public ElapsingConfig setLoggable(Loggable loggable) {
        this.loggable = loggable;
        return this;
    }

    public long getMaxElapsed() {
        return maxElapsed;
    }

    public String getMessageFormat() {
        return messageFormat;
    }

    public String getOvertimeMessageFormat() {
        return overtimeMessageFormat;
    }

    public Loggable getLoggable() {
        return loggable;
    }
}