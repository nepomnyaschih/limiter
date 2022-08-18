package net.amzscout.limiter.rest;

import net.amzscout.limiter.aspects.LimitMeByIp;
import net.amzscout.limiter.aspects.LimiterException;
import net.amzscout.limiter.services.MyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class Controller {

    @Autowired
    private MyService myService;

    @LimitMeByIp
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String main(HttpServletRequest request) {
        return "";
    }

    @GetMapping(value = "/service", produces = MediaType.TEXT_HTML_VALUE)
    public void service() {
         myService.test();
    }

    @ExceptionHandler({ LimiterException.class })
    public ResponseEntity<Object> handleLimiterException() {
        return new ResponseEntity<Object>("The number of calls exceeds the allowable value.", new HttpHeaders(), HttpStatus.BAD_GATEWAY);
    }

}
