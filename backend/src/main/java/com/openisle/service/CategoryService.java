package com.openisle.service;

import com.openisle.model.Category;
import com.openisle.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Category createCategory(String name, String description, String icon, String smallIcon) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);
        category.setSmallIcon(smallIcon);
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, String name, String description, String icon, String smallIcon) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (name != null) {
            category.setName(name);
        }
        if (description != null) {
            category.setDescription(description);
        }
        if (icon != null) {
            category.setIcon(icon);
        }
        if (smallIcon != null) {
            category.setSmallIcon(smallIcon);
        }
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }
}
