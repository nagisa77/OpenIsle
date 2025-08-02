package com.openisle.service;

import com.openisle.model.Category;
import com.openisle.model.Draft;
import com.openisle.model.Tag;
import com.openisle.model.User;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.DraftRepository;
import com.openisle.repository.TagRepository;
import com.openisle.repository.UserRepository;
import com.openisle.service.ImageUploader;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DraftService {
    private final DraftRepository draftRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ImageUploader imageUploader;

    @Transactional
    public Draft saveDraft(String username, Long categoryId, String title, String content, List<Long> tagIds) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Draft draft = draftRepository.findByAuthor(user).orElse(new Draft());
        String oldContent = draft.getContent();
        boolean existing = draft.getId() != null;
        draft.setAuthor(user);
        draft.setTitle(title);
        draft.setContent(content);
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            draft.setCategory(category);
        } else {
            draft.setCategory(null);
        }
        Set<Tag> tags = new HashSet<>();
        if (tagIds != null && !tagIds.isEmpty()) {
            tags.addAll(tagRepository.findAllById(tagIds));
        }
        draft.setTags(tags);
        Draft saved = draftRepository.save(draft);
        if (existing) {
            imageUploader.adjustReferences(oldContent == null ? "" : oldContent, content);
        } else {
            imageUploader.addReferences(imageUploader.extractUrls(content));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Draft> getDraft(String username) {
        return userRepository.findByUsername(username)
                .flatMap(draftRepository::findByAuthor);
    }

    @Transactional
    public void deleteDraft(String username) {
        userRepository.findByUsername(username).ifPresent(user ->
                draftRepository.findByAuthor(user).ifPresent(draft -> {
                    imageUploader.removeReferences(imageUploader.extractUrls(draft.getContent()));
                    draftRepository.delete(draft);
                })
        );
    }
}
