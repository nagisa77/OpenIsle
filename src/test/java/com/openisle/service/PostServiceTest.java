package com.openisle.service;

import com.openisle.model.*;
import com.openisle.repository.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.mockito.Mockito.*;

class PostServiceTest {
    @Test
    void deletePostRemovesReads() {
        PostRepository postRepo = mock(PostRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        CategoryRepository catRepo = mock(CategoryRepository.class);
        TagRepository tagRepo = mock(TagRepository.class);
        NotificationService notifService = mock(NotificationService.class);
        SubscriptionService subService = mock(SubscriptionService.class);
        CommentService commentService = mock(CommentService.class);
        CommentRepository commentRepo = mock(CommentRepository.class);
        ReactionRepository reactionRepo = mock(ReactionRepository.class);
        PostSubscriptionRepository subRepo = mock(PostSubscriptionRepository.class);
        NotificationRepository notificationRepo = mock(NotificationRepository.class);
        PostReadService postReadService = mock(PostReadService.class);

        PostService service = new PostService(postRepo, userRepo, catRepo, tagRepo,
                notifService, subService, commentService, commentRepo,
                reactionRepo, subRepo, notificationRepo, postReadService,
                PublishMode.DIRECT);

        Post post = new Post();
        post.setId(1L);
        User author = new User();
        author.setId(1L);
        author.setRole(Role.USER);
        post.setAuthor(author);

        when(postRepo.findById(1L)).thenReturn(Optional.of(post));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(author));
        when(commentRepo.findByPostAndParentIsNullOrderByCreatedAtAsc(post)).thenReturn(List.of());
        when(reactionRepo.findByPost(post)).thenReturn(List.of());
        when(subRepo.findByPost(post)).thenReturn(List.of());
        when(notificationRepo.findByPost(post)).thenReturn(List.of());

        service.deletePost(1L, "alice");

        verify(postReadService).deleteByPost(post);
        verify(postRepo).delete(post);
    }
}
