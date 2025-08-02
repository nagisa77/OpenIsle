package com.openisle.controller;

import com.openisle.model.Notification;
import com.openisle.model.NotificationType;
import com.openisle.model.Post;
import com.openisle.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void listNotifications() throws Exception {
        Notification n = new Notification();
        n.setId(1L);
        n.setType(NotificationType.POST_VIEWED);
        Post p = new Post();
        p.setId(2L);
        n.setPost(p);
        n.setCreatedAt(LocalDateTime.now());
        Mockito.when(notificationService.listNotifications("alice", null))
                .thenReturn(List.of(n));

        mockMvc.perform(get("/api/notifications")
                        .principal(new UsernamePasswordAuthenticationToken("alice","p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].post.id").value(2));
    }

    @Test
    void markReadEndpoint() throws Exception {
        mockMvc.perform(post("/api/notifications/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2]}" )
                        .principal(new UsernamePasswordAuthenticationToken("alice","p")))
                .andExpect(status().isOk());

        verify(notificationService).markRead("alice", List.of(1L,2L));
    }

    @Test
    void unreadCountEndpoint() throws Exception {
        Mockito.when(notificationService.countUnread("alice")).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .principal(new UsernamePasswordAuthenticationToken("alice","p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }
}
