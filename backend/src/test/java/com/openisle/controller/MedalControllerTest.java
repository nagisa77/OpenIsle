package com.openisle.controller;

import com.openisle.dto.CommentMedalDto;
import com.openisle.model.MedalType;
import com.openisle.service.MedalService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedalController.class)
@AutoConfigureMockMvc(addFilters = false)
class MedalControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedalService medalService;

    @Test
    void listMedals() throws Exception {
        CommentMedalDto medal = new CommentMedalDto();
        medal.setTitle("评论达人");
        medal.setType(MedalType.COMMENT);
        Mockito.when(medalService.getMedals(null)).thenReturn(List.of(medal));

        mockMvc.perform(get("/api/medals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("评论达人"));
    }

    @Test
    void listMedalsWithUser() throws Exception {
        CommentMedalDto medal = new CommentMedalDto();
        medal.setCompleted(true);
        medal.setType(MedalType.COMMENT);
        Mockito.when(medalService.getMedals(1L)).thenReturn(List.of(medal));

        mockMvc.perform(get("/api/medals").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completed").value(true));
    }

    @Test
    void selectMedal() throws Exception {
        mockMvc.perform(post("/api/medals/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"COMMENT\"}")
                .principal(() -> "user"))
                .andExpect(status().isOk());
    }

    @Test
    void selectMedalBadRequest() throws Exception {
        Mockito.doThrow(new IllegalArgumentException()).when(medalService)
                .selectMedal("user", MedalType.COMMENT);
        mockMvc.perform(post("/api/medals/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"COMMENT\"}")
                .principal(() -> "user"))
                .andExpect(status().isBadRequest());
    }
}
