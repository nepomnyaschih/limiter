package net.amzscout.limiter.aspects;


public class LimiterException extends Exception {
    public LimiterException(String className, String methodName, long rate, long time) {
        super("The number of " + className + "." + methodName + "() calls must not exceed " + rate + " request(s) per " + time + " minute(s)!");
    }
}
