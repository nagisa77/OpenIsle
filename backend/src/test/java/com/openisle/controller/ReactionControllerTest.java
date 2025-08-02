package com.openisle.controller;

import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.Reaction;
import com.openisle.model.ReactionType;
import com.openisle.model.User;
import com.openisle.service.ReactionService;
import com.openisle.service.LevelService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReactionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReactionService reactionService;
    @MockBean
    private LevelService levelService;

    @Test
    void reactToPost() throws Exception {
        User user = new User();
        user.setUsername("u1");
        Post post = new Post();
        post.setId(1L);
        Reaction reaction = new Reaction();
        reaction.setId(1L);
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setType(ReactionType.LIKE);
        Mockito.when(reactionService.reactToPost(eq("u1"), eq(1L), eq(ReactionType.LIKE))).thenReturn(reaction);

        mockMvc.perform(post("/api/posts/1/reactions")
                        .contentType("application/json")
                        .content("{\"type\":\"LIKE\"}")
                        .principal(new UsernamePasswordAuthenticationToken("u1", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(1));
    }

    @Test
    void reactToComment() throws Exception {
        User user = new User();
        user.setUsername("u2");
        Comment comment = new Comment();
        comment.setId(2L);
        Reaction reaction = new Reaction();
        reaction.setId(2L);
        reaction.setUser(user);
        reaction.setComment(comment);
        reaction.setType(ReactionType.RECOMMEND);
        Mockito.when(reactionService.reactToComment(eq("u2"), eq(2L), eq(ReactionType.RECOMMEND))).thenReturn(reaction);

        mockMvc.perform(post("/api/comments/2/reactions")
                        .contentType("application/json")
                        .content("{\"type\":\"RECOMMEND\"}")
                        .principal(new UsernamePasswordAuthenticationToken("u2", "p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(2));
    }

    @Test
    void listReactionTypes() throws Exception {
        mockMvc.perform(get("/api/reaction-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("LIKE"));
    }
}
