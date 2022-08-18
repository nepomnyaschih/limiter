package net.amzscout.limiter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"limiter.rate = 5", "limiter.time = 1"}
)
class LimiterApplicationTests {

    private static final int NUMBER_OF_THREADS = 10;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void timeTest() throws InterruptedException {
        var reqTimeLine = new ArrayList<Long>();
        var resArray = new ArrayList<HttpStatus>();

        for (var i = 0; i < 5; i++) {
            reqTimeLine.add(System.currentTimeMillis());
            resArray.add(sendRequest());
            Thread.sleep(5000);
        }
        var successCount = resArray.stream().filter(r -> r.equals(HttpStatus.OK)).count();
        assertEquals(5, successCount);

        reqTimeLine.add(System.currentTimeMillis());
        assertEquals(HttpStatus.BAD_GATEWAY, sendRequest());

        var time5thFromEnd = reqTimeLine.get(reqTimeLine.size() - 5);
        Thread.sleep(60000 - (System.currentTimeMillis() - time5thFromEnd) + 1000);

        assertEquals(HttpStatus.OK, sendRequest());
        assertEquals(HttpStatus.BAD_GATEWAY, sendRequest());

        for (var i = 0; i < 61; i++) {
            assertEquals(HttpStatus.BAD_GATEWAY, sendRequest());
            Thread.sleep(1000);
        }
    }

    @Test
    void singleIpMultiThreadTest() throws InterruptedException {
        var executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        var latch = new CountDownLatch(NUMBER_OF_THREADS);

        Queue<HttpStatus> resArray = new ConcurrentLinkedQueue<>();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("X-Forwarded-For", "192.168.0.1");

        for (var i = 0; i < NUMBER_OF_THREADS; i++) {

            executorService.execute(() -> {
                ResponseEntity<String> result = restTemplate.exchange(
                        "http://localhost:" + port + "/",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
                resArray.add(result.getStatusCode());
                latch.countDown();
            });

        }

        assertTrue(latch.await(60, TimeUnit.SECONDS));

        var successResponseCount = resArray.stream().filter(r -> r.equals(HttpStatus.OK)).count();
        var failedResponseCount = resArray.stream().filter(r -> r.equals(HttpStatus.BAD_GATEWAY)).count();

        assertEquals(resArray.size(), NUMBER_OF_THREADS);
        assertEquals(5, successResponseCount);
        assertEquals(5, failedResponseCount);
    }

    @Test
    void randomIpMultiThreadTest() throws InterruptedException {
        var executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        var latch = new CountDownLatch(NUMBER_OF_THREADS);

        Queue<HttpStatus> resArray = new ConcurrentLinkedQueue<>();

        for (var i = 0; i < NUMBER_OF_THREADS; i++) {
            executorService.execute(() -> {

                MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
                headers.add("X-Forwarded-For", getRandomIp());
                ResponseEntity<String> result = restTemplate.exchange(
                        "http://localhost:" + port + "/",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);

                resArray.add(result.getStatusCode());
                latch.countDown();
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS));

        var successResponseCount = resArray.stream().filter(r -> r.equals(HttpStatus.OK)).count();
        var failedResponseCount = resArray.stream().filter(r -> r.equals(HttpStatus.BAD_GATEWAY)).count();

        assertEquals(resArray.size(), NUMBER_OF_THREADS);
        assertEquals(10, successResponseCount);
        assertEquals(0, failedResponseCount);
    }

    @Test
    void twoIpMultiThreadTest() throws InterruptedException {
        var executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        var latch = new CountDownLatch(NUMBER_OF_THREADS);

        var ip1 = getRandomIp();
        var ip2 = getRandomIp();

        Queue<HttpStatus> resArrayIp1 = new ConcurrentLinkedQueue<>();
        Queue<HttpStatus> resArrayIp2 = new ConcurrentLinkedQueue<>();

        for (var i = 0; i < NUMBER_OF_THREADS; i++) {
            int threadNum = i;
            executorService.execute(() -> {
                MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
                headers.add("X-Forwarded-For", threadNum % 2 == 0 ? ip1 : ip2);
                ResponseEntity<String> result = restTemplate.exchange(
                        "http://localhost:" + port + "/", HttpMethod.GET, new HttpEntity<>(headers),
                        String.class);
                if (threadNum % 2 == 0) {
                    resArrayIp1.add(result.getStatusCode());
                } else {
                    resArrayIp2.add(result.getStatusCode());
                }

                latch.countDown();
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS));

        var successResponseCountIp1 = resArrayIp1.stream().filter(r -> r.equals(HttpStatus.OK)).count();
        var successResponseCountIp2 = resArrayIp2.stream().filter(r -> r.equals(HttpStatus.OK)).count();

        assertEquals(resArrayIp1.size() + resArrayIp2.size(), NUMBER_OF_THREADS);
        assertEquals(5, successResponseCountIp1);
        assertEquals(5, successResponseCountIp2);
    }

    private HttpStatus sendRequest() {
        ResponseEntity<String> result = restTemplate.getForEntity("http://localhost:" + port + "/", String.class);
        return result.getStatusCode();
    }

    private String getRandomIp() {
        var rnd = new Random();
        return rnd.nextInt(256) + "." + rnd.nextInt(256) + "." + rnd.nextInt(256) + "." + rnd.nextInt(256);
    }
}
