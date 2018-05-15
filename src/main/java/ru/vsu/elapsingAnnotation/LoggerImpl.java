package ru.vsu.elapsingAnnotation;


public class LoggerImpl  extends java.util.logging.Logger implements ru.vsu.elapsingAnnotation.Logger {
    public LoggerImpl(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }
}
