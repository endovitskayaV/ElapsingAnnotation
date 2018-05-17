package ru.vsu.elapsingAnnotation;

import java.util.logging.Level;

public interface Loggable {
    void log(Level level, String msg, String sourceClassName, String sourceMethodName);
}
