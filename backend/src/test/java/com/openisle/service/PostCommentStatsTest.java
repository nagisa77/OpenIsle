package com.openisle.service;

import com.openisle.model.*;
import com.openisle.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
public class PostCommentStatsTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CommentService commentService;

    @Test
    public void testPostCommentStatsUpdate() {
        // Create test user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hash");
        user = userRepository.save(user);

        // Create test category
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Test Category Description");
        category.setIcon("test-icon");
        category = categoryRepository.save(category);

        // Create test tag
        Tag tag = new Tag();
        tag.setName("Test Tag");
        tag.setDescription("Test Tag Description");
        tag.setIcon("test-tag-icon");
        tag = tagRepository.save(tag);

        // Create test post
        Post post = new Post();
        post.setTitle("Test Post");
        post.setContent("Test content");
        post.setAuthor(user);
        post.setCategory(category);
        post.getTags().add(tag);
        post.setStatus(PostStatus.PUBLISHED);
        post.setCommentCount(0L);
        post = postRepository.save(post);

        // Verify initial state
        assertEquals(0L, post.getCommentCount());
        assertNull(post.getLastReplyAt());

        // Add a comment
        commentService.addComment("testuser", post.getId(), "Test comment");

        // Refresh post from database
        post = postRepository.findById(post.getId()).orElseThrow();

        // Verify comment count and last reply time are updated
        assertEquals(1L, post.getCommentCount());
        assertNotNull(post.getLastReplyAt());

        // Add another comment
        commentService.addComment("testuser", post.getId(), "Another comment");

        // Refresh post again
        post = postRepository.findById(post.getId()).orElseThrow();

        // Verify comment count is updated
        assertEquals(2L, post.getCommentCount());
    }
}
