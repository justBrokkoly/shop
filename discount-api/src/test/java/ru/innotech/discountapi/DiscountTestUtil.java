package ru.innotech.discountapi;

import lombok.experimental.UtilityClass;
import ru.innotech.discountapi.adapters.controller.dto.response.DiscountResponse;
import ru.innotech.discountapi.core.model.DiscountEntity;

import java.math.BigDecimal;

@UtilityClass
public class DiscountTestUtil {

    public static DiscountEntity discountMock(Long productId) {
        return DiscountEntity.builder()
                .name("Discount1")
                .description("d1")
                .productId(productId)
                .discount(new BigDecimal("15.00"))
                .build();
    }

    public static DiscountResponse discountResponseMock(Long productId){
        return new DiscountResponse(productId, new BigDecimal("15.00"));
    }
}
