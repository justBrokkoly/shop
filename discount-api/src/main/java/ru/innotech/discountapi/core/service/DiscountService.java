package ru.innotech.discountapi.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.innotech.discountapi.adapters.repository.DiscountRepository;
import ru.innotech.discountapi.core.mapper.DiscountMapper;
import ru.innotech.discountapi.adapters.controller.dto.response.DiscountResponse;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {
    private final DiscountMapper discountMapper;
    private final DiscountRepository discountRepository;


    @Cacheable(value = "discounts", key = "'allDiscounts'")
    @Transactional(readOnly = true)
    public List<DiscountResponse> getDiscounts() {
        try (var op = MDC.putCloseable("op", "getDiscounts")) {
            log.debug("Get discounts: fetching all");
            List<DiscountResponse> result = discountRepository.findAll().stream()
                    .map(discountMapper::toDto)
                    .collect(Collectors.toList());
            try (var size = MDC.putCloseable("batchSize", String.valueOf(result.size()))) {
                log.info("Get discounts: returned {}", result.size());
            }
            return result;
        } catch (Exception e) {
            log.error("Get discounts: failed - {}", e.getMessage());
            throw e;
        }
    }

}
