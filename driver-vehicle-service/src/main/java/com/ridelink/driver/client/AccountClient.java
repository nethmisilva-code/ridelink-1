package com.ridelink.driver.client;

import com.ridelink.driver.exception.DownstreamServiceException;
import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Outbound interservice call to the Account Service:
 * GET /api/v1/accounts/{id}, forwarding the caller's own bearer token.
 *
 * Synchronous REST is used because profile creation cannot proceed until we
 * know the account exists, holds the DRIVER role and has been approved, so
 * the caller needs an immediate answer.
 */
@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    public AccountSummary fetchAccount(String accountId, String bearerToken) {
        AccountSummary account;
        try {
            account = restClient.get()
                    .uri("/api/v1/accounts/{id}", accountId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(AccountSummary.class);

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Account not found: " + accountId);

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new ForbiddenOperationException("Not authorised to read account " + accountId);

        } catch (RestClientException ex) {
            // Graceful degradation: Account Service is down, slow or returned 5xx.
            throw new DownstreamServiceException(
                    "Account Service is unavailable, so account " + accountId + " cannot be verified. Try again shortly.");
        }
        if (account == null) {
            throw new DownstreamServiceException("Account Service returned an empty response for account " + accountId);
        }
        return account;
    }
}
