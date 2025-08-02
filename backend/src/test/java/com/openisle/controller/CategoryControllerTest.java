package com.openisle.controller;

import com.openisle.model.Category;
import com.openisle.service.CategoryService;
import com.openisle.service.PostService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private PostService postService;

    @Test
    void createAndGetCategory() throws Exception {
        Category c = new Category();
        c.setId(1L);
        c.setName("tech");
        c.setDescription("d");
        c.setIcon("i");
        c.setSmallIcon("s1");
        Mockito.when(categoryService.createCategory(eq("tech"), eq("d"), eq("i"), eq("s1"))).thenReturn(c);
        Mockito.when(categoryService.getCategory(1L)).thenReturn(c);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"tech\",\"description\":\"d\",\"icon\":\"i\",\"smallIcon\":\"s1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("tech"))
                .andExpect(jsonPath("$.description").value("d"))
                .andExpect(jsonPath("$.icon").value("i"))
                .andExpect(jsonPath("$.smallIcon").value("s1"));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void listCategories() throws Exception {
        Category c = new Category();
        c.setId(2L);
        c.setName("life");
        c.setDescription("d2");
        c.setIcon("i2");
        c.setSmallIcon("s2");
        Mockito.when(categoryService.listCategories()).thenReturn(List.of(c));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("life"))
                .andExpect(jsonPath("$[0].description").value("d2"))
                .andExpect(jsonPath("$[0].icon").value("i2"))
                .andExpect(jsonPath("$[0].smallIcon").value("s2"));
    }

    @Test
    void updateCategory() throws Exception {
        Category c = new Category();
        c.setId(3L);
        c.setName("tech");
        c.setDescription("d3");
        c.setIcon("i3");
        c.setSmallIcon("s3");
        Mockito.when(categoryService.updateCategory(eq(3L), eq("tech"), eq("d3"), eq("i3"), eq("s3"))).thenReturn(c);

        mockMvc.perform(put("/api/categories/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"tech\",\"description\":\"d3\",\"icon\":\"i3\",\"smallIcon\":\"s3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("tech"))
                .andExpect(jsonPath("$.description").value("d3"))
                .andExpect(jsonPath("$.icon").value("i3"))
                .andExpect(jsonPath("$.smallIcon").value("s3"));
    }
}
