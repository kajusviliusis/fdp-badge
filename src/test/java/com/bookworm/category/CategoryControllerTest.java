package com.bookworm.category;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void listReturnsCategories() throws Exception {
        given(categoryService.findAll()).willReturn(List.of(new CategoryResponse(1L, "Fiction")));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fiction"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        given(categoryService.findById(9L)).willThrow(ResourceNotFoundException.of("Category", 9L));

        mockMvc.perform(get("/categories/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturns201() throws Exception {
        given(categoryService.create(new CategoryRequest("Science"))).willReturn(new CategoryResponse(2L, "Science"));

        mockMvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Science\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void createReturns422ForShortName() throws Exception {
        mockMvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"A\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").value("A"));
    }

    @Test
    void createReturns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns409WhenDuplicate() throws Exception {
        given(categoryService.create(new CategoryRequest("Fiction"))).willThrow(new ConflictException("exists"));

        mockMvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Fiction\"}"))
                .andExpect(status().isConflict());
    }
}
