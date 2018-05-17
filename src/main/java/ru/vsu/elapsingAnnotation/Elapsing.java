package ru.vsu.elapsingAnnotation;

import java.lang.annotation.*;

/**
 * measures operating time of the method
 * default values are taken from ElapsingConfig
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Elapsing {

    String DEFAULT_MESSAGE = "DEFAULT_MESSAGE";

    /**
     * specifies whether custom annotation params should be applied
     * if customMessage is set true and params are not set
     * default params from ElapsingConfig will be used
     */
    boolean customParams() default false;

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
