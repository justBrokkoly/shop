package ru.innotech.discountapi.adapters.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.innotech.discountapi.DiscountApiApplication;
import ru.innotech.discountapi.adapters.repository.DiscountRepository;

@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureWireMock(
        port = 0
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.openfeign.client.config.discout-api.url=http://localhost:${wiremock.server.port}"})
@ActiveProfiles("test")
@ContextConfiguration(classes = {DiscountApiApplication.class})
public abstract class AbstractIntegrationTest {
    private static final String DATABASE_NAME = "discount";
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected DiscountRepository discountRepository;
    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Container
    public static PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:12.3")
            .withReuse(true)
            .withUsername("sa")
            .withPassword("sa")
            .withDatabaseName(DATABASE_NAME);

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    private static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");
    }

    @BeforeEach
    public void init() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        discountRepository.deleteAll();
    }
}
