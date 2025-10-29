package ru.innotech.productapi.adapters.discount;

import java.util.List;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import ru.innotech.productapi.adapters.discount.dto.DiscountResponse;

@FeignClient(name = "discount-api")
public interface DiscountClient {

    @Bulkhead(name = "getDiscountsBulkhead")
    @CircuitBreaker(name = "getDiscountsCircuitBreaker")
    @Retry(name = "getDiscountsRetry")
    @GetMapping("/api/v1/discounts")
    List<DiscountResponse> getDiscounts();
}
