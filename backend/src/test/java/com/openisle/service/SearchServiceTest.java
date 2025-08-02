package com.openisle.service;

import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.UserRepository;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchServiceTest {

    @Test
    void globalSearchDeduplicatesPosts() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        PostRepository postRepo = Mockito.mock(PostRepository.class);
        CommentRepository commentRepo = Mockito.mock(CommentRepository.class);
        CategoryRepository categoryRepo = Mockito.mock(CategoryRepository.class);
        TagRepository tagRepo = Mockito.mock(TagRepository.class);
        SearchService service = new SearchService(userRepo, postRepo, commentRepo, categoryRepo, tagRepo);

        Post post1 = new Post();
        post1.setId(1L);
        post1.setTitle("hello");
        Post post2 = new Post();
        post2.setId(2L);
        post2.setTitle("world");

        Mockito.when(postRepo.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseAndStatus(
                Mockito.anyString(), Mockito.anyString(), Mockito.eq(PostStatus.PUBLISHED)))
                .thenReturn(List.of(post1));
        Mockito.when(postRepo.findByTitleContainingIgnoreCaseAndStatus(Mockito.anyString(), Mockito.eq(PostStatus.PUBLISHED)))
                .thenReturn(List.of(post1, post2));
        Mockito.when(commentRepo.findByContentContainingIgnoreCase(Mockito.anyString()))
                .thenReturn(List.of());
        Mockito.when(userRepo.findByUsernameContainingIgnoreCase(Mockito.anyString()))
                .thenReturn(List.of());

        List<SearchService.SearchResult> results = service.globalSearch("h");

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).id());
        assertEquals(2L, results.get(1).id());
    }
}
