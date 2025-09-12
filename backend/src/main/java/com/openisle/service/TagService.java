package com.openisle.service;

import com.openisle.config.CachingConfig;
import com.openisle.model.Tag;
import com.openisle.model.User;
import com.openisle.repository.TagRepository;
import com.openisle.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagValidator tagValidator;
    private final UserRepository userRepository;

    @CacheEvict(value = CachingConfig.TAG_CACHE_NAME, allEntries = true)
    public Tag createTag(String name, String description, String icon, String smallIcon, boolean approved, String creatorUsername) {
        tagValidator.validate(name);
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        tag.setIcon(icon);
        tag.setSmallIcon(smallIcon);
        tag.setApproved(approved);
        if (creatorUsername != null) {
            User creator = userRepository.findByUsername(creatorUsername)
                    .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
            tag.setCreator(creator);
        }
        return tagRepository.save(tag);
    }

    public Tag createTag(String name, String description, String icon, String smallIcon, boolean approved) {
        return createTag(name, description, icon, smallIcon, approved, null);
    }

    public Tag createTag(String name, String description, String icon, String smallIcon) {
        return createTag(name, description, icon, smallIcon, true, null);
    }

    @CacheEvict(value = CachingConfig.TAG_CACHE_NAME, allEntries = true)
    public Tag updateTag(Long id, String name, String description, String icon, String smallIcon) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        if (name != null) {
            tagValidator.validate(name);
            tag.setName(name);
        }
        if (description != null) {
            tag.setDescription(description);
        }
        if (icon != null) {
            tag.setIcon(icon);
        }
        if (smallIcon != null) {
            tag.setSmallIcon(smallIcon);
        }
        return tagRepository.save(tag);
    }

    @CacheEvict(value = CachingConfig.TAG_CACHE_NAME, allEntries = true)
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }

    public Tag approveTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        tag.setApproved(true);
        return tagRepository.save(tag);
    }

    public List<Tag> listPendingTags() {
        return tagRepository.findByApproved(false);
    }

    public Tag getTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    public List<Tag> listTags() {
        return tagRepository.findByApprovedTrue();
    }

    /**
     * 该方法每次首页加载都会访问，加入缓存
     * @param keyword
     * @return
     */
    @Cacheable(
            value = CachingConfig.TAG_CACHE_NAME,
            key = "'searchTags:' + (#keyword ?: '')"//keyword为null的场合返回空
    )
    public List<Tag> searchTags(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return tagRepository.findByApprovedTrue();
        }

        return tagRepository.findByNameContainingIgnoreCaseAndApprovedTrue(keyword);
    }

    public List<Tag> searchTags(String keyword, Integer page, Integer pageSize) {
        if (page == null || pageSize == null) {
            return searchTags(keyword);
        }
        Pageable pageable = PageRequest.of(page, pageSize);
        if (keyword == null || keyword.isBlank()) {
            return tagRepository.findByApprovedTrue(pageable).getContent();
        }
        return tagRepository.findByNameContainingIgnoreCaseAndApprovedTrue(keyword, pageable).getContent();
    }

    public List<Tag> getRecentTagsByUser(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Pageable pageable = PageRequest.of(0, limit);
        return tagRepository.findByCreatorOrderByCreatedAtDesc(user, pageable);
    }

    public List<Tag> getTagsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        return tagRepository.findByCreator(user);
    }
}
