package ru.innotech.productapi.adapters.discount;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.innotech.productapi.ProductTestUtil;
import ru.innotech.productapi.adapters.controller.AbstractIntegrationTest;
import ru.innotech.productapi.core.model.Product;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscountClientTest extends AbstractIntegrationTest {

    @Autowired
    private DiscountClient discountClient;

    @Test
    void getDiscountsCheckRetry() {
        Product product1 = ProductTestUtil.product1Mock();
        Product product2 = ProductTestUtil.product1Mock();

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/discounts"))
                .inScenario("Timing Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("Second Attempt")
                .willReturn(aResponse().withStatus(500)));

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/discounts"))
                .inScenario("Timing Scenario")
                .whenScenarioStateIs("Second Attempt")
                .willSetStateTo("Third Attempt")
                .willReturn(aResponse().withStatus(500)));

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/discounts"))
                .inScenario("Timing Scenario")
                .whenScenarioStateIs("Third Attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  { "productId": %d, "discount": 0.15 },
                                  { "productId": %d, "discount": 0.2  }
                                ]
                                """.formatted(product1.getId(), product2.getId()))
                ));

        long testStart = System.nanoTime();
        discountClient.getDiscounts();
        long totalTestTime = System.nanoTime() - testStart;
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(totalTestTime) >= 6);
//        WireMock.verify(3, getRequestedFor(WireMock.urlPathEqualTo("/api/v1/discounts")));
    }

    @Test
    void getDiscountsCheckCircuitBreaker() throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("getDiscountsCircuitBreaker");

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/discounts"))
                .willReturn(aResponse().withStatus(500)));


        try {
            //3fail(retries)
            discountClient.getDiscounts();
        } catch (Exception e) {

        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        Thread.sleep(2500);

        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/discounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  { "productId": %d, "discount": 0.15 },
                                  { "productId": %d, "discount": 0.2  }
                                ]
                                """.formatted(1L, 2L))
                ));

        for (int i = 0; i < 3; i++) {
            discountClient.getDiscounts();

        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

}
