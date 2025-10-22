package ru.innotech.discountapi.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.innotech.discountapi.core.model.DiscountEntity;
import ru.innotech.discountapi.adapters.controller.dto.response.DiscountResponse;

@Mapper(componentModel = "spring",
        //        imports = {BigDecimal.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiscountMapper {
    DiscountResponse toDto(DiscountEntity product);
}
