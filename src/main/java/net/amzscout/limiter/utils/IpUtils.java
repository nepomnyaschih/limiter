package net.amzscout.limiter.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class IpUtils {
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR"
    };

    public static String getClientIpAddressIfServletRequestExist() {

        if (RequestContextHolder.getRequestAttributes() != null) {

            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            for (var header : IP_HEADER_CANDIDATES) {
                var ipHeader = request.getHeader(header);
                if (ipHeader != null && ipHeader.length() != 0 && !"unknown".equalsIgnoreCase(ipHeader)) {
                    return ipHeader.split(",")[0];
                }
            }

            return request.getRemoteAddr();
        }

        return null;
    }
}
