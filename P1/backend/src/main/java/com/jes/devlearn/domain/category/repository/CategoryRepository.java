package com.jes.devlearn.domain.category.repository;

import com.jes.devlearn.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
