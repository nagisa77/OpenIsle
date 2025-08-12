package com.openisle.mapper;

import com.openisle.dto.*;
import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.User;
import com.openisle.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Mapper for user related DTOs. */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final SubscriptionService subscriptionService;
    private final PostService postService;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final UserVisitService userVisitService;
    private final PostReadService postReadService;
    private final LevelService levelService;
    private final MedalService medalService;

    @Value("${app.snippet-length:50}")
    private int snippetLength;

    public AuthorDto toAuthorDto(User user) {
        medalService.ensureDisplayMedal(user);
        AuthorDto dto = new AuthorDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        dto.setDisplayMedal(user.getDisplayMedal());
        return dto;
    }

    public UserDto toDto(User user, Authentication viewer) {
        medalService.ensureDisplayMedal(user);
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole().name());
        dto.setIntroduction(user.getIntroduction());
        dto.setFollowers(subscriptionService.countSubscribers(user.getUsername()));
        dto.setFollowing(subscriptionService.countSubscribed(user.getUsername()));
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastPostTime(postService.getLastPostTime(user.getUsername()));
        dto.setLastCommentTime(commentService.getLastCommentTimeOfUserByUserId(user.getId()));
        dto.setTotalViews(postService.getTotalViews(user.getUsername()));
        dto.setVisitedDays(userVisitService.countVisits(user.getUsername()));
        dto.setReadPosts(postReadService.countReads(user.getUsername()));
        dto.setLikesSent(reactionService.countLikesSent(user.getUsername()));
        dto.setLikesReceived(reactionService.countLikesReceived(user.getUsername()));
        dto.setExperience(user.getExperience());
        dto.setPoint(user.getPoint());
        dto.setCurrentLevel(levelService.getLevel(user.getExperience()));
        dto.setNextLevelExp(levelService.nextLevelExp(user.getExperience()));
        if (viewer != null) {
            dto.setSubscribed(subscriptionService.isSubscribed(viewer.getName(), user.getUsername()));
        } else {
            dto.setSubscribed(false);
        }
        return dto;
    }

    public UserDto toDto(User user) {
        return toDto(user, null);
    }

    public PostMetaDto toMetaDto(Post post) {
        PostMetaDto dto = new PostMetaDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        String content = post.getContent();
        if (content == null) {
            content = "";
        }
        if (snippetLength >= 0) {
            dto.setSnippet(content.length() > snippetLength ? content.substring(0, snippetLength) : content);
        } else {
            dto.setSnippet(content);
        }
        dto.setCreatedAt(post.getCreatedAt());
        dto.setCategory(post.getCategory().getName());
        dto.setViews(post.getViews());
        return dto;
    }

    public CommentInfoDto toCommentInfoDto(Comment comment) {
        CommentInfoDto dto = new CommentInfoDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setPost(toMetaDto(comment.getPost()));
        if (comment.getParent() != null) {
            ParentCommentDto pc = new ParentCommentDto();
            pc.setId(comment.getParent().getId());
            pc.setAuthor(comment.getParent().getAuthor().getUsername());
            pc.setContent(comment.getParent().getContent());
            dto.setParentComment(pc);
        }
        return dto;
    }
}
