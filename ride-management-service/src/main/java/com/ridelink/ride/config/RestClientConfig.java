package com.ridelink.ride.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** One RestClient per downstream service, with short timeouts so failures surface quickly as 503. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient driverRestClient(RestClient.Builder builder,
            @Value("${ridelink.clients.driver-service}") String baseUrl,
            @Value("${ridelink.clients.connect-timeout-ms:2000}") long connectMs,
            @Value("${ridelink.clients.read-timeout-ms:5000}") long readMs) {
        return builder.baseUrl(baseUrl).requestFactory(requestFactory(connectMs, readMs)).build();
    }

    @Bean
    public RestClient fareRestClient(RestClient.Builder builder,
            @Value("${ridelink.clients.fare-service}") String baseUrl,
            @Value("${ridelink.clients.connect-timeout-ms:2000}") long connectMs,
            @Value("${ridelink.clients.read-timeout-ms:5000}") long readMs) {
        return builder.baseUrl(baseUrl).requestFactory(requestFactory(connectMs, readMs)).build();
    }

    /** JDK HttpClient: supports PATCH (needed for driver availability), unlike HttpURLConnection. */
    private static JdkClientHttpRequestFactory requestFactory(long connectMs, long readMs) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readMs));
        return factory;
    }
}
