package com.openisle.controller;

import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.User;
import com.openisle.service.CommentService;
import com.openisle.service.CaptchaService;
import com.openisle.service.LevelService;
import com.openisle.service.ReactionService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;
    @MockBean
    private CaptchaService captchaService;
    @MockBean
    private LevelService levelService;
    @MockBean
    private ReactionService reactionService;

    private Comment createComment(Long id, String content, String authorName) {
        User user = new User();
        user.setUsername(authorName);
        Comment c = new Comment();
        c.setId(id);
        c.setContent(content);
        c.setCreatedAt(LocalDateTime.now());
        c.setAuthor(user);
        c.setPost(new Post());
        return c;
    }

    @Test
    void createAndListComments() throws Exception {
        Comment comment = createComment(1L, "hi", "bob");
        Mockito.when(commentService.addComment(eq("bob"), eq(1L), eq("hi"))).thenReturn(comment);
        Mockito.when(commentService.getCommentsForPost(eq(1L), any())).thenReturn(List.of(comment));
        Mockito.when(commentService.getReplies(1L)).thenReturn(List.of());
        Mockito.when(reactionService.getReactionsForComment(1L)).thenReturn(List.of());

        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType("application/json")
                        .content("{\"content\":\"hi\"}")
                        .principal(new UsernamePasswordAuthenticationToken("bob", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("hi"));

        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void replyComment() throws Exception {
        Comment reply = createComment(2L, "re", "alice");
        Mockito.when(commentService.addReply(eq("alice"), eq(1L), eq("re"))).thenReturn(reply);

        mockMvc.perform(post("/api/comments/1/replies")
                        .contentType("application/json")
                        .content("{\"content\":\"re\"}")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }
}
