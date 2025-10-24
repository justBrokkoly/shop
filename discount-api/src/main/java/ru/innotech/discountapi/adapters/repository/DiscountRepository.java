package ru.innotech.discountapi.adapters.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.innotech.discountapi.core.model.DiscountEntity;

public interface DiscountRepository extends JpaRepository<DiscountEntity, Long> {

}
