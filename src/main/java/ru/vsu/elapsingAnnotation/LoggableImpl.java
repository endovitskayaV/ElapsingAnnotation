package ru.vsu.elapsingAnnotation;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggableImpl implements Loggable {

    @Override
    public void log(Level level, String sourceClassName, String sourceMethodName,String msg) {
        Logger.getGlobal().logp(level,sourceClassName, sourceMethodName, msg);
    }
}
