package com.openisle.controller;

import com.openisle.model.Draft;
import com.openisle.service.DraftService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftController {
    private final DraftService draftService;

    @PostMapping
    public ResponseEntity<DraftDto> saveDraft(@RequestBody DraftRequest req, Authentication auth) {
        Draft draft = draftService.saveDraft(auth.getName(), req.getCategoryId(), req.getTitle(), req.getContent(), req.getTagIds());
        return ResponseEntity.ok(toDto(draft));
    }

    @GetMapping("/me")
    public ResponseEntity<DraftDto> getMyDraft(Authentication auth) {
        return draftService.getDraft(auth.getName())
                .map(d -> ResponseEntity.ok(toDto(d)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyDraft(Authentication auth) {
        draftService.deleteDraft(auth.getName());
        return ResponseEntity.ok().build();
    }

    private DraftDto toDto(Draft draft) {
        DraftDto dto = new DraftDto();
        dto.setId(draft.getId());
        dto.setTitle(draft.getTitle());
        dto.setContent(draft.getContent());
        if (draft.getCategory() != null) {
            dto.setCategoryId(draft.getCategory().getId());
        }
        dto.setTagIds(draft.getTags().stream().map(com.openisle.model.Tag::getId).collect(Collectors.toList()));
        return dto;
    }

    @Data
    private static class DraftRequest {
        private String title;
        private String content;
        private Long categoryId;
        private List<Long> tagIds;
    }

    @Data
    private static class DraftDto {
        private Long id;
        private String title;
        private String content;
        private Long categoryId;
        private List<Long> tagIds;
    }
}
