package ru.innotech.discountapi.core.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.innotech.discountapi.DiscountTestUtil;
import ru.innotech.discountapi.adapters.controller.dto.response.DiscountResponse;
import ru.innotech.discountapi.adapters.repository.DiscountRepository;
import ru.innotech.discountapi.core.mapper.DiscountMapper;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountMapper discountMapper;
    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountService discountService;

    @Test
    void whenGetAllProductsThenSuccess() {
        var discount1 = DiscountTestUtil.discountMock(1L);
        var discount2 = DiscountTestUtil.discountMock(2L);
        var discountResponse1 = DiscountTestUtil.discountResponseMock(1L);
        var discountResponse2 = DiscountTestUtil.discountResponseMock(2L);
        Mockito.when(discountRepository.findAll()).thenReturn(List.of(discount1, discount2));
        Mockito.when(discountMapper.toDto(discount1)).thenReturn(discountResponse1);
        Mockito.when(discountMapper.toDto(discount2)).thenReturn(discountResponse2);
        List<DiscountResponse> expected = List.of(discountResponse1, discountResponse2);
        List<DiscountResponse> actual = discountService.getDiscounts();
        Assertions.assertEquals(expected, actual);
    }
}
