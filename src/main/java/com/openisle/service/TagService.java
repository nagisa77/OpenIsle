package com.openisle.service;

import com.openisle.model.Tag;
import com.openisle.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagValidator tagValidator;

    public Tag createTag(String name, String description, String icon, String smallIcon) {
        tagValidator.validate(name);
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        tag.setIcon(icon);
        tag.setSmallIcon(smallIcon);
        return tagRepository.save(tag);
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

    public Tag getTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    public List<Tag> listTags() {
        return tagRepository.findAll();
    }
}
