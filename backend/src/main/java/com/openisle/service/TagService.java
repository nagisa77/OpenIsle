package com.openisle.service;

import com.openisle.model.Tag;
import com.openisle.model.User;
import com.openisle.repository.TagRepository;
import com.openisle.repository.UserRepository;
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

    public List<Tag> searchTags(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return tagRepository.findByApprovedTrue();
        }
        return tagRepository.findByNameContainingIgnoreCaseAndApprovedTrue(keyword);
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
