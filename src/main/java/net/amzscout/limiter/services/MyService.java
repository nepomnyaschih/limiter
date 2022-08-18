package net.amzscout.limiter.services;

import net.amzscout.limiter.aspects.LimitMeByIp;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    @LimitMeByIp(rate = 5, time = 2)
    public void test() {
        System.out.println("test method called");
    }
}

