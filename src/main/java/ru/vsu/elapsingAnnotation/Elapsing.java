package ru.vsu.elapsingAnnotation;

import java.lang.annotation.*;

/**
 * measures operating time of the method
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Elapsing {
    ElapsingConfig elapsing()default el;
    Class
//    String DEFAULT_MESSAGE ="DEFAULT_MESSAGE";
//    /**
//     * time unit of measure
//     */
//    TimeUnitEnum timeUnit() default TimeUnitEnum.DEFAULT;
//
//    /**
//     * format of logged message about elapsed time
//     * usage String.format(elapsedTime, messageFormat)
//     * should contain  "%s" otherwise elapsed time will not be logged
//     */
//    String messageFormat() default DEFAULT_MESSAGE;
//
//    /**
//     * maximum method operation time
//     * must be greater than 0
//     */
//    long maxElapsed() default -1;
//
//    /**
//     * format of logged message about the excedence of maxElapsed
//     * usage String.format(elapsedTime-maxElapsed, maxElapsedMessageFormat)
//     * should contain  "%s" otherwise  time will not be logged
//     */
//    String maxElapsedMessageFormat() default DEFAULT_MESSAGE;
}
