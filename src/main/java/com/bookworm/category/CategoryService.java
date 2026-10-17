package com.bookworm.category;

import java.util.List;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ResourceNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().strip();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category '%s' already exists.".formatted(name));
        }
        return CategoryResponse.from(categoryRepository.save(new Category(name)));
    }

    private Category getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }
}
