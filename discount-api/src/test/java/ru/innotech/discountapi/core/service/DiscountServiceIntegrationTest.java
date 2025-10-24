package ru.innotech.discountapi.core.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import ru.innotech.discountapi.DiscountTestUtil;
import ru.innotech.discountapi.adapters.controller.AbstractIntegrationTest;
import ru.innotech.discountapi.adapters.controller.dto.response.DiscountResponse;
import ru.innotech.discountapi.core.model.DiscountEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DiscountServiceIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    protected DiscountService discountService;

    @Test
    void whenGetAllProductsThenSuccess() {
        var discountResponse1 = DiscountTestUtil.discountResponseMock(1L);
        var discountResponse2 = DiscountTestUtil.discountResponseMock(2L);
        DiscountEntity discount1 = DiscountTestUtil.discountMock(1L);
        DiscountEntity discount2 = DiscountTestUtil.discountMock(2L);
        discountRepository.saveAll(List.of(discount1, discount2));
        List<DiscountResponse> expected = List.of(discountResponse1, discountResponse2);
        List<DiscountResponse> actual = discountService.getDiscounts();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenGetAllProductsShouldSaveInCacheThenSuccess() {
        var discountResponse1 = DiscountTestUtil.discountResponseMock(1L);
        var discountResponse2 = DiscountTestUtil.discountResponseMock(2L);
        DiscountEntity discount1 = DiscountTestUtil.discountMock(1L);
        DiscountEntity discount2 = DiscountTestUtil.discountMock(2L);
        discountRepository.saveAll(List.of(discount1, discount2));
        var result = discountService.getDiscounts();
        assertTrue(redisTemplate.hasKey("discounts::allDiscounts"));
        var cacheValue = (List<DiscountResponse>) redisTemplate.opsForValue().get("discounts::allDiscounts");
        assertEquals(2, result.size());
        assertEquals(List.of(discountResponse1, discountResponse2), cacheValue);
    }

    @Test
    void whenGetAllProductsShouldVerifyCacheHit() {
        var discountResponse1 = DiscountTestUtil.discountResponseMock(1L);
        var discountResponse2 = DiscountTestUtil.discountResponseMock(2L);
        DiscountEntity discount1 = DiscountTestUtil.discountMock(1L);
        DiscountEntity discount2 = DiscountTestUtil.discountMock(2L);
        var expectedResult = List.of(discountResponse1, discountResponse2);
        discountRepository.saveAll(List.of(discount1, discount2));
        var resultWithoutCache = discountService.getDiscounts();
        var cachedResult = discountService.getDiscounts();
        verify(discountService, times(1)).getDiscounts();
        assertEquals(expectedResult, resultWithoutCache);
        assertEquals(expectedResult, cachedResult);
    }
}
