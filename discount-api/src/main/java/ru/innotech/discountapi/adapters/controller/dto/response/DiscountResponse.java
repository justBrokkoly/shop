package ru.innotech.discountapi.adapters.controller.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;


public record DiscountResponse (
        Long productId,
        BigDecimal discount
) implements Serializable {
}
