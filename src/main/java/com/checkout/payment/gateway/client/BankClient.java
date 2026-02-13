package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

  private static final Logger LOG = LoggerFactory.getLogger(BankClient.class);

  private final RestTemplate restTemplate;
  private final String bankBaseUrl;

  public BankClient(RestTemplate restTemplate,
      @Value("${bank.simulator.url:http://localhost:8080}") String bankBaseUrl) {
    this.restTemplate = restTemplate;
    this.bankBaseUrl = bankBaseUrl;
  }

  public Optional<BankPaymentResponse> processPayment(BankPaymentRequest request) {
    String url = bankBaseUrl + "/payments";
    try {
      ResponseEntity<BankPaymentResponse> response =
          restTemplate.postForEntity(url, request, BankPaymentResponse.class);
      return Optional.ofNullable(response.getBody());
    } catch (HttpServerErrorException e) {
      LOG.error("Bank returned server error: {} {}", e.getStatusCode(), e.getMessage());
      return Optional.empty();
    } catch (RestClientException e) {
      LOG.error("Failed to communicate with the bank: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
