package ru.innotech.productapi.adapters.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.innotech.productapi.ProductTestUtil;
import ru.innotech.productapi.adapters.controller.dto.request.ProductRequest;
import ru.innotech.productapi.adapters.controller.dto.response.ProductResponse;
import ru.innotech.productapi.core.model.Product;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.hamcrest.Matchers.hasSize;

class ProductControllerTest extends AbstractIntegrationTest {

    @Test
    void whenGetProductByIdThenEntityNotFound() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void whenGetProductByIdThenSuccess() throws Exception {
        Product product = ProductTestUtil.product1Mock();
        productRepository.save(product);
        ProductResponse expected = ProductTestUtil.product1ResponseMock(product.getId());
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpectAll(MockMvcResultMatchers.status().isOk(),
                MockMvcResultMatchers.jsonPath("$.name").value(expected.name()),
                MockMvcResultMatchers.jsonPath("$.description").value(expected.description()),
                MockMvcResultMatchers.jsonPath("$.price").value(expected.price().intValue())
        );
    }

    @Test
    void whenDeleteProductByIdThenSuccess() throws Exception {
        Product product = ProductTestUtil.product1Mock();
        productRepository.save(product);
        mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/v1/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNoContent());

        Product actual = productRepository.findById(product.getId()).orElse(null);
        Assertions.assertNull(actual);
    }

    @Test
    void whenGetAllProductsThenSuccess() throws Exception {
        Product product1 = ProductTestUtil.product1Mock();
        Product product2 = ProductTestUtil.product2Mock();
        productRepository.save(product1);
        productRepository.save(product2);

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpectAll(MockMvcResultMatchers.status().isOk(),
                MockMvcResultMatchers.jsonPath("$", hasSize(2))
        );
    }

    @Test
    void whenCreateProductThenSuccess() throws Exception {
        ProductRequest productRequest = ProductTestUtil.productCreateRequest();
        String request = objectMapper.writeValueAsString(productRequest);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        ).andExpect(MockMvcResultMatchers.status().isCreated());

        List<Product> actual = productRepository.findAll();
        Assertions.assertEquals(actual.size(), 1);
    }

    @Test
    void whenUpdateProductThenSuccess() throws Exception {
        ProductRequest productRequest = ProductTestUtil.productUpdateRequest();
        String request = objectMapper.writeValueAsString(productRequest);
        Product product = ProductTestUtil.product1Mock();
        productRepository.save(product);
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/v1/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        ).andExpect(MockMvcResultMatchers.status().isOk());
        Product actual = productRepository.findById(product.getId()).orElse(null);
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(actual.getName(), productRequest.name());
        Assertions.assertEquals(actual.getDescription(), productRequest.description());
        Assertions.assertEquals(actual.getPrice().intValue(), productRequest.price().intValue());
        Assertions.assertEquals(actual.getCurrency(), productRequest.currency());
    }

    @Test
    void whenUpdateProductDiscountsThenSuccess() throws Exception {
        Product product1 = ProductTestUtil.product1Mock();
        Product product2 = ProductTestUtil.product1Mock();
        productRepository.save(product1);
        productRepository.save(product2);
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/discount/api/v1/discounts"))
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
        mockMvc.perform(
                MockMvcRequestBuilders.patch("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk());

        Product actual1 = productRepository.findById(product1.getId()).orElse(null);
        Product actual2 = productRepository.findById(product2.getId()).orElse(null);
        Assertions.assertNotNull(actual1);
        Assertions.assertNotNull(actual2);
        Assertions.assertEquals(BigDecimal.valueOf(0.15), actual1.getDiscount().stripTrailingZeros());
        Assertions.assertEquals(BigDecimal.valueOf(0.2), actual2.getDiscount().stripTrailingZeros());
    }


}
