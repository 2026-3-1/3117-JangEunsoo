package com.jes.devlearn.domain.category.controller;

import com.jes.devlearn.domain.category.dto.request.CategoryCreateRequestDTO;
import com.jes.devlearn.domain.category.dto.response.CategoryResponseDTO;
import com.jes.devlearn.domain.category.service.CategoryService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<CategoryResponseDTO>>> getCategories() {
        return ResponseEntity.ok(GlobalApiResponse.success(categoryService.getCategories()));
    }

    @PostMapping
    public ResponseEntity<GlobalApiResponse<CategoryResponseDTO>> createCategory(
            @Valid @RequestBody CategoryCreateRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.success(categoryService.createCategory(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<CategoryResponseDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryCreateRequestDTO dto
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(categoryService.updateCategory(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteCategory(
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(GlobalApiResponse.success("카테고리가 삭제되었습니다."));
    }
}
