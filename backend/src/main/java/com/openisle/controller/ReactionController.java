package com.openisle.controller;

import com.openisle.model.Reaction;
import com.openisle.model.ReactionType;
import com.openisle.service.ReactionService;
import com.openisle.service.LevelService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;
    private final LevelService levelService;

    /**
     * Get all available reaction types.
     */
    @GetMapping("/reaction-types")
    public ReactionType[] listReactionTypes() {
        return ReactionType.values();
    }

    @PostMapping("/posts/{postId}/reactions")
    public ResponseEntity<ReactionDto> reactToPost(@PathVariable Long postId,
                                                  @RequestBody ReactionRequest req,
                                                  Authentication auth) {
        Reaction reaction = reactionService.reactToPost(auth.getName(), postId, req.getType());
        if (reaction == null) {
            return ResponseEntity.noContent().build();
        }
        ReactionDto dto = toDto(reaction);
        dto.setReward(levelService.awardForReaction(auth.getName()));
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/comments/{commentId}/reactions")
    public ResponseEntity<ReactionDto> reactToComment(@PathVariable Long commentId,
                                                     @RequestBody ReactionRequest req,
                                                     Authentication auth) {
        Reaction reaction = reactionService.reactToComment(auth.getName(), commentId, req.getType());
        if (reaction == null) {
            return ResponseEntity.noContent().build();
        }
        ReactionDto dto = toDto(reaction);
        dto.setReward(levelService.awardForReaction(auth.getName()));
        return ResponseEntity.ok(dto);
    }

    private ReactionDto toDto(Reaction reaction) {
        ReactionDto dto = new ReactionDto();
        dto.setId(reaction.getId());
        dto.setType(reaction.getType());
        dto.setUser(reaction.getUser().getUsername());
        if (reaction.getPost() != null) {
            dto.setPostId(reaction.getPost().getId());
        }
        if (reaction.getComment() != null) {
            dto.setCommentId(reaction.getComment().getId());
        }
        dto.setReward(0);
        return dto;
    }

    @Data
    private static class ReactionRequest {
        private ReactionType type;
    }

    @Data
    private static class ReactionDto {
        private Long id;
        private ReactionType type;
        private String user;
        private Long postId;
        private Long commentId;
        private int reward;
    }
}
