package com.jes.devlearn.domain.category.service;

import com.jes.devlearn.domain.category.dto.request.CategoryCreateRequestDTO;
import com.jes.devlearn.domain.category.dto.response.CategoryResponseDTO;
import com.jes.devlearn.domain.category.entity.Category;
import com.jes.devlearn.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponseDTO::from)
                .toList();
    }

    @Transactional
    public CategoryResponseDTO createCategory(CategoryCreateRequestDTO dto) {
        Category category = new Category(dto.name());
        categoryRepository.save(category);

        return CategoryResponseDTO.from(category);
    }
}
