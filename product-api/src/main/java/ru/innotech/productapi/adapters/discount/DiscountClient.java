package ru.innotech.productapi.adapters.discount;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import ru.innotech.productapi.adapters.discount.dto.DiscountResponse;

@FeignClient(name = "gateway-api")
public interface DiscountClient {
    @GetMapping("/discount/api/v1/discounts")
    List<DiscountResponse> getDiscounts();
}
