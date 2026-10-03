package com.ridelink.driver.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /** Short timeouts so a slow Account Service fails fast with 503 instead of hanging the request. */
    @Bean
    public RestClient accountRestClient(RestClient.Builder builder,
            @Value("${ridelink.clients.account-service}") String baseUrl,
            @Value("${ridelink.clients.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${ridelink.clients.read-timeout-ms:5000}") long readTimeoutMs) {
        // JDK HttpClient: supports every HTTP method (HttpURLConnection cannot send PATCH).
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
