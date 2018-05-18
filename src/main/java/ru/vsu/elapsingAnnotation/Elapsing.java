package ru.vsu.elapsingAnnotation;

import java.lang.annotation.*;

/**
 * measures operating time of the method
 * default values are taken from ElapsingConfig
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)

public @interface Elapsing {

    String DEFAULT_MESSAGE = "DEFAULT_MESSAGE";

    /**
     * format of logged message about elapsed time
     * usage String.format(elapsedTime, messageFormat)
     * should contain  "%s" otherwise elapsed time will not be logged
     */
    String messageFormat() default DEFAULT_MESSAGE;

    /**
     * format of logged message about over elapsed time
     * usage String.format(elapsedTime, messageFormat)
     * should contain  "%s" otherwise elapsed time will not be logged
     */
    String overtimeMessageFormat() default DEFAULT_MESSAGE;

    /**
     * maximum elapsed time, milliseconds
     */
    long maxElapsed() default 0;

}
