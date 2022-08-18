package net.amzscout.limiter.aspects;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD})
public @interface LimitMeByIp {
    int rate() default 0;

    int time() default 0;
}