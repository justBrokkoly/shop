package ru.innotech.productapi.adapters.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.innotech.productapi.ProductApiApplication;
import ru.innotech.productapi.adapters.repository.ProductRepository;

@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureWireMock(
        port = 0
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.openfeign.client.config.discount-api.url=http://localhost:${wiremock.server.port}"})
@ActiveProfiles("test")
@ContextConfiguration(classes = {ProductApiApplication.class})
public abstract class AbstractIntegrationTest {
    private static final String DATABASE_NAME = "product";
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    protected BulkheadRegistry bulkheadRegistry;

    @Container
    public static PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:12.3")
            .withReuse(true)
            .withUsername("sa")
            .withPassword("sa")
            .withDatabaseName(DATABASE_NAME);

    @DynamicPropertySource
    private static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
    }

    @BeforeEach
    public void init() {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("myServiceClient");
        bulkhead.releasePermission();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("getDiscountsCircuitBreaker");
        circuitBreaker.reset();
        productRepository.deleteAll();
    }
}
