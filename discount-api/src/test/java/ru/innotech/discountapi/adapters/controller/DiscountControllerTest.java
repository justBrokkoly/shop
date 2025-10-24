package ru.innotech.discountapi.adapters.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.innotech.discountapi.DiscountTestUtil;
import ru.innotech.discountapi.core.model.DiscountEntity;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;

class DiscountControllerTest extends AbstractIntegrationTest {


    @Test
    void whenGetAllProductsThenSuccess() throws Exception {
        DiscountEntity discount1 = DiscountTestUtil.discountMock(1L);
        DiscountEntity discount2= DiscountTestUtil.discountMock(2L);
        discountRepository.saveAll(List.of(discount1,discount2));
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpectAll(MockMvcResultMatchers.status().isOk(),
                MockMvcResultMatchers.jsonPath("$", hasSize(2))
        );
    }

}
