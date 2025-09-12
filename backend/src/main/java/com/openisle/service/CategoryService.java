package com.openisle.service;

import com.openisle.config.CachingConfig;
import com.openisle.model.Category;
import com.openisle.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public Category createCategory(String name, String description, String icon, String smallIcon) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);
        category.setSmallIcon(smallIcon);
        return categoryRepository.save(category);
    }
    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
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
    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    /**
     * 该方法每次首页加载都会访问，加入缓存
     * @return
     */
    @Cacheable(
            value = CachingConfig.CATEGORY_CACHE_NAME,
            key = "'listCategories:'"
    )
    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    /**
     * 获取检索用的分类Id列表
     * @param categoryIds
     * @param categoryId
     * @return
     */
    public List<Long> getSearchCategoryIds(List<Long> categoryIds, Long categoryId){
        List<Long> ids = categoryIds;
        if (categoryId != null) {
            ids = List.of(categoryId);
        }
        return ids;
    }
}
