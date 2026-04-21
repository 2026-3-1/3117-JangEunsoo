package com.jes.devlearn.domain.category.service;

import com.jes.devlearn.domain.category.dto.request.CategoryCreateRequestDTO;
import com.jes.devlearn.domain.category.dto.response.CategoryResponseDTO;
import com.jes.devlearn.domain.category.entity.Category;
import com.jes.devlearn.domain.category.error.CategoryErrorCode;
import com.jes.devlearn.domain.category.repository.CategoryRepository;
import com.jes.devlearn.global.exception.CustomException;
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

    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryCreateRequestDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        category.update(dto.name());
        return CategoryResponseDTO.from(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }
}
