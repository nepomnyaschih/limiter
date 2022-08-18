package net.amzscout.limiter.aspects;

import net.amzscout.limiter.circularbuffer.CircularBuffer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.amzscout.limiter.utils.IpUtils.getClientIpAddressIfServletRequestExist;

@Aspect
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class LimiterByIpAspect {

    Logger logger = LoggerFactory.getLogger(LimiterByIpAspect.class);

    @Value("${limiter.rate:0}")
    int settingRate;

    @Value("${limiter.time:0}")
    int settingTime;

    private final ConcurrentMap<String, CircularBuffer> cache = new ConcurrentHashMap<>();

    @Around("@annotation(net.amzscout.limiter.aspects.LimitMeByIp)")
    public Object limitMe(ProceedingJoinPoint joinPoint) throws Throwable {

        var ip = getClientIpAddressIfServletRequestExist();

        if (StringUtils.hasText(ip)) {

            var methodSignature = (MethodSignature) joinPoint.getSignature();
            var className = methodSignature.getDeclaringType().getSimpleName();
            var methodName = methodSignature.getName();

            var annotation = methodSignature.getMethod().getAnnotation(LimitMeByIp.class);

            var rate = annotation.rate() != 0 ? annotation.rate() : settingRate;
            var time = annotation.time() != 0 ? annotation.time() : settingTime;

            if (rate == 0 || time == 0) {
                logger.warn("Limiter doesn't work for method " + className + "." + methodName);
                return joinPoint.proceed();
            }

            var cacheKey = ip + "_" + className + "_" + methodName;
            var buffer = cache.computeIfAbsent(cacheKey, k -> new CircularBuffer(rate));
            var currentTime = System.currentTimeMillis();

            synchronized (buffer) {
                if (buffer.isFull()) {
                    var firstTime = buffer.peek();
                    buffer.replaceFirst(currentTime);
                    if (currentTime - firstTime <= time * 1000L * 60L) {
                        throw new LimiterException(className, methodName, rate, time);
                    }
                } else {
                    buffer.add(currentTime);
                }
            }

        }

        return joinPoint.proceed();
    }

}
