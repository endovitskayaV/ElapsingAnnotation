package ru.vsu.elapsingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Elapsing {
    enum TimeInterval { MILLISECOND, NANOSECOND }

    TimeInterval interval() default TimeInterval.MILLISECOND;
    String format() default "Elapsed %s";
}
