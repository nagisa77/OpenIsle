package com.openisle.controller;

import com.openisle.mapper.CategoryMapper;
import com.openisle.mapper.CommentMapper;
import com.openisle.mapper.PostMapper;
import com.openisle.mapper.ReactionMapper;
import com.openisle.mapper.TagMapper;
import com.openisle.mapper.UserMapper;
import com.openisle.model.*;
import com.openisle.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PostMapper.class, CommentMapper.class, ReactionMapper.class,
        UserMapper.class, TagMapper.class, CategoryMapper.class})
class PostControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostController postController;

    @MockBean
    private PostService postService;
    @MockBean
    private CommentService commentService;
    @MockBean
    private ReactionService reactionService;
    @MockBean
    private CaptchaService captchaService;
    @MockBean
    private DraftService draftService;
    @MockBean
    private LevelService levelService;
    @MockBean
    private SubscriptionService subscriptionService;
    @MockBean
    private UserVisitService userVisitService;
    @MockBean
    private PostReadService postReadService;

    @Test
    void createAndGetPost() throws Exception {
        User user = new User();
        user.setUsername("alice");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("tech");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        Post post = new Post();
        post.setId(1L);
        post.setTitle("t");
        post.setContent("c");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(Set.of(tag));

        when(postService.createPost(eq("alice"), eq(1L), eq("t"), eq("c"), eq(List.of(1L)),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(post);
        when(postService.viewPost(eq(1L), any())).thenReturn(post);
        when(commentService.getCommentsForPost(eq(1L), any())).thenReturn(List.of());
        when(commentService.getParticipants(anyLong(), anyInt())).thenReturn(List.of());
        when(reactionService.getReactionsForPost(1L)).thenReturn(List.of());
        when(commentService.getLastCommentTime(1L)).thenReturn(null);

        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"content\":\"c\",\"categoryId\":1,\"tagIds\":[1]}")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("t"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty())
                .andExpect(jsonPath("$.author.username").value("alice"))
                .andExpect(jsonPath("$.category.name").value("tech"))
                .andExpect(jsonPath("$.tags[0].name").value("java"))
                .andExpect(jsonPath("$.subscribed").value(false));

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty())
                .andExpect(jsonPath("$.subscribed").value(false));
    }

    @Test
    void updatePostReturnsDetailDto() throws Exception {
        User user = new User();
        user.setUsername("alice");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("tech");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        Post post = new Post();
        post.setId(1L);
        post.setTitle("t2");
        post.setContent("c2");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(Set.of(tag));

        when(postService.updatePost(eq(1L), eq("alice"), eq(1L), eq("t2"), eq("c2"), eq(List.of(1L)))).thenReturn(post);
        when(commentService.getCommentsForPost(eq(1L), any())).thenReturn(List.of());
        when(commentService.getParticipants(anyLong(), anyInt())).thenReturn(List.of());
        when(reactionService.getReactionsForPost(1L)).thenReturn(List.of());
        when(commentService.getLastCommentTime(1L)).thenReturn(null);

        mockMvc.perform(put("/api/posts/1")
                        .contentType("application/json")
                        .content("{\"title\":\"t2\",\"content\":\"c2\",\"categoryId\":1,\"tagIds\":[1]}")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("t2"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty())
                .andExpect(jsonPath("$.author.username").value("alice"));
    }

    @Test
    void listPosts() throws Exception {
        User user = new User();
        user.setUsername("bob");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("tech");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        Post post = new Post();
        post.setId(2L);
        post.setTitle("hello");
        post.setContent("world");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(Set.of(tag));

        when(postService.listPostsByCategories(null, null, null)).thenReturn(List.of(post));
        when(commentService.getParticipants(anyLong(), anyInt())).thenReturn(List.of());
        when(reactionService.getReactionsForPost(anyLong())).thenReturn(List.of());
        when(commentService.getLastCommentTime(anyLong())).thenReturn(null);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("hello"))
                .andExpect(jsonPath("$[0].comments").doesNotExist())
                .andExpect(jsonPath("$[0].author.username").value("bob"))
                .andExpect(jsonPath("$[0].category.name").value("tech"))
                .andExpect(jsonPath("$[0].tags[0].name").value("java"))
                .andExpect(jsonPath("$[0].subscribed").value(false));
    }

    @Test
    void createPostRejectsInvalidCaptcha() throws Exception {
        ReflectionTestUtils.setField(postController, "captchaEnabled", true);
        ReflectionTestUtils.setField(postController, "postCaptchaEnabled", true);
        when(captchaService.verify("bad")).thenReturn(false);

        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"content\":\"c\",\"categoryId\":1,\"tagIds\":[1],\"captcha\":\"bad\"}")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "p")))
                .andExpect(status().isBadRequest());

        verify(postService, never()).createPost(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void getPostWithNestedData() throws Exception {
        User user = new User();
        user.setUsername("alice");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("tech");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        Post post = new Post();
        post.setId(1L);
        post.setTitle("t");
        post.setContent("c");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(Set.of(tag));

        com.openisle.model.Comment comment = new com.openisle.model.Comment();
        comment.setId(2L);
        comment.setContent("hi");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setAuthor(user);
        comment.setPost(post);

        com.openisle.model.Comment reply = new com.openisle.model.Comment();
        reply.setId(3L);
        reply.setContent("reply");
        reply.setCreatedAt(LocalDateTime.now());
        reply.setAuthor(user);
        reply.setPost(post);

        com.openisle.model.Reaction pr = new com.openisle.model.Reaction();
        pr.setId(10L);
        pr.setUser(user);
        pr.setPost(post);
        pr.setType(com.openisle.model.ReactionType.LIKE);

        com.openisle.model.Reaction cr = new com.openisle.model.Reaction();
        cr.setId(11L);
        cr.setUser(user);
        cr.setComment(comment);
        cr.setType(com.openisle.model.ReactionType.LIKE);

        when(postService.viewPost(eq(1L), any())).thenReturn(post);
        when(commentService.getCommentsForPost(eq(1L), any())).thenReturn(List.of(comment));
        when(commentService.getReplies(2L)).thenReturn(List.of(reply));
        when(commentService.getReplies(3L)).thenReturn(List.of());
        when(commentService.getParticipants(anyLong(), anyInt())).thenReturn(List.of());
        when(commentService.getLastCommentTime(1L)).thenReturn(null);
        when(reactionService.getReactionsForPost(1L)).thenReturn(List.of(pr));
        when(reactionService.getReactionsForComment(2L)).thenReturn(List.of(cr));
        when(reactionService.getReactionsForComment(3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions[0].id").value(10))
                .andExpect(jsonPath("$.comments[0].replies[0].id").value(3))
                .andExpect(jsonPath("$.comments[0].reactions[0].id").value(11))
                .andExpect(jsonPath("$.author.username").value("alice"))
                .andExpect(jsonPath("$.category.name").value("tech"))
                .andExpect(jsonPath("$.tags[0].name").value("java"));
    }

    @Test
    void getPostSubscriptionStatus() throws Exception {
        User user = new User();
        user.setUsername("alice");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("tech");
        Post post = new Post();
        post.setId(1L);
        post.setTitle("t");
        post.setContent("c");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(Set.of());

        when(postService.viewPost(eq(1L), any())).thenReturn(post);
        when(commentService.getCommentsForPost(eq(1L), any())).thenReturn(List.of());
        when(commentService.getParticipants(anyLong(), anyInt())).thenReturn(List.of());
        when(reactionService.getReactionsForPost(1L)).thenReturn(List.of());
        when(commentService.getLastCommentTime(1L)).thenReturn(null);
        when(subscriptionService.isPostSubscribed("alice", 1L)).thenReturn(true);

        mockMvc.perform(get("/api/posts/1").principal(new UsernamePasswordAuthenticationToken("alice", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true));

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false));
    }
}
