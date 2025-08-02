package com.openisle.controller;

import com.openisle.model.Post;
import com.openisle.model.User;
import com.openisle.model.Category;
import com.openisle.model.Tag;
import com.openisle.service.PostService;
import com.openisle.service.CommentService;
import com.openisle.service.ReactionService;
import com.openisle.service.CaptchaService;
import com.openisle.service.DraftService;
import com.openisle.service.LevelService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
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

    @Test
    void createAndGetPost() throws Exception {
        User user = new User();
        user.setUsername("alice");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("tech");
        cat.setDescription("d");
        cat.setIcon("i");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        tag.setDescription("td");
        tag.setIcon("ti");
        Post post = new Post();
        post.setId(1L);
        post.setTitle("t");
        post.setContent("c");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(java.util.Set.of(tag));
        Mockito.when(commentService.getParticipants(Mockito.anyLong(), Mockito.anyInt())).thenReturn(java.util.List.of());
        Mockito.when(postService.createPost(eq("alice"), eq(1L), eq("t"), eq("c"), eq(java.util.List.of(1L)))).thenReturn(post);
        Mockito.when(postService.viewPost(eq(1L), Mockito.isNull())).thenReturn(post);

        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"content\":\"c\",\"categoryId\":1,\"tagIds\":[1]}")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("t"));

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void listPosts() throws Exception {
        User user = new User();
        user.setUsername("bob");
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("tech");
        cat.setDescription("d");
        cat.setIcon("i");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        tag.setDescription("td");
        tag.setIcon("ti");
        Post post = new Post();
        post.setId(2L);
        post.setTitle("hello");
        post.setContent("world");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(java.util.Set.of(tag));
        Mockito.when(commentService.getParticipants(Mockito.anyLong(), Mockito.anyInt())).thenReturn(java.util.List.of());
        Mockito.when(postService.listPostsByCategories(Mockito.isNull(), Mockito.isNull(), Mockito.isNull()))
                .thenReturn(List.of(post));

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("hello"));
    }

    @Test
    void createPostRejectsInvalidCaptcha() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(postController, "captchaEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(postController, "postCaptchaEnabled", true);
        Mockito.when(captchaService.verify("bad")).thenReturn(false);

        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"content\":\"c\",\"categoryId\":1,\"tagIds\":[1],\"captcha\":\"bad\"}")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "p")))
                .andExpect(status().isBadRequest());

        verify(postService, never()).createPost(any(), any(), any(), any(), any());
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
        post.setTags(java.util.Set.of(tag));

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

        Mockito.when(postService.viewPost(eq(1L), Mockito.isNull())).thenReturn(post);
        Mockito.when(commentService.getCommentsForPost(eq(1L), any())).thenReturn(List.of(comment));
        Mockito.when(commentService.getReplies(2L)).thenReturn(List.of(reply));
        Mockito.when(commentService.getReplies(3L)).thenReturn(List.of());
        Mockito.when(commentService.getParticipants(Mockito.anyLong(), Mockito.anyInt())).thenReturn(java.util.List.of());
        Mockito.when(reactionService.getReactionsForPost(1L)).thenReturn(List.of(pr));
        Mockito.when(reactionService.getReactionsForComment(2L)).thenReturn(List.of(cr));
        Mockito.when(reactionService.getReactionsForComment(3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactions[0].id").value(10))
                .andExpect(jsonPath("$.comments[0].replies[0].id").value(3))
                .andExpect(jsonPath("$.comments[0].reactions[0].id").value(11));
    }

    @Test
    void listPostsByCategoriesAndTags() throws Exception {
        User user = new User();
        user.setUsername("alice");
        Category cat = new Category();
        cat.setName("tech");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        Post post = new Post();
        post.setId(2L);
        post.setTitle("hello");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(java.util.Set.of(tag));
        Mockito.when(commentService.getParticipants(Mockito.anyLong(), Mockito.anyInt())).thenReturn(java.util.List.of());

        Mockito.when(postService.listPostsByCategoriesAndTags(eq(java.util.List.of(1L)), eq(java.util.List.of(1L, 2L)), eq(0), eq(5)))
                .thenReturn(List.of(post));

        mockMvc.perform(get("/api/posts")
                        .param("tagIds", "1,2")
                        .param("page", "0")
                        .param("pageSize", "5")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));

        verify(postService).listPostsByCategoriesAndTags(eq(java.util.List.of(1L)), eq(java.util.List.of(1L, 2L)), eq(0), eq(5));
        verify(postService, never()).listPostsByCategories(any(), any(), any());
    }

    @Test
    void rankingPostsFiltered() throws Exception {
        User user = new User();
        user.setUsername("alice");
        Category cat = new Category();
        cat.setName("tech");
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");
        Post post = new Post();
        post.setId(3L);
        post.setTitle("rank");
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(user);
        post.setCategory(cat);
        post.setTags(java.util.Set.of(tag));
        Mockito.when(commentService.getParticipants(Mockito.anyLong(), Mockito.anyInt())).thenReturn(java.util.List.of());

        Mockito.when(postService.listPostsByViews(eq(java.util.List.of(1L)), eq(java.util.List.of(1L, 2L)), eq(0), eq(5)))
                .thenReturn(List.of(post));

        mockMvc.perform(get("/api/posts/ranking")
                        .param("tagIds", "1,2")
                        .param("page", "0")
                        .param("pageSize", "5")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));

        verify(postService).listPostsByViews(eq(java.util.List.of(1L)), eq(java.util.List.of(1L, 2L)), eq(0), eq(5));
    }
}
