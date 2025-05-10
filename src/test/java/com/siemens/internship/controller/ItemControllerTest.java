package com.siemens.internship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private Item testItem;
    private List<Item> testItems;

    @BeforeEach
    void setUp() {
        testItem = new Item(1L, "Test Item 1", "Description1", "NEW", "test1@example.com");
        Item secondItem = new Item(2L, "Test Item 2", "Description2", "NEW", "test2@example.com");
        testItems = Arrays.asList(testItem, secondItem);
    }

    @Test
    void getAllItems_shouldReturnAllItems() throws Exception {
        when(itemService.findAll()).thenReturn(testItems);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test Item 1")))
                .andExpect(jsonPath("$[1].name", is("Test Item 2")));

        verify(itemService).findAll();
    }

    @Test
    void createItem_withValidData_shouldReturnCreatedItem() throws Exception {
        when(itemService.save(any(Item.class))).thenReturn(testItem);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Item 1")));

        verify(itemService).save(any(Item.class));
    }

    @Test
    void createItem_withInvalidEmail_shouldReturnBadRequest() throws Exception {
        Item invalidItem = new Item(null, "Test Item", "Description", "NEW", "invalid-email");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).save(any(Item.class));
    }

    @Test
    void getItemById_withExistingId_shouldReturnItem() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(testItem));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Item 1")));

        verify(itemService).findById(1L);
    }

    @Test
    void getItemById_withNonExistingId_shouldReturnNotFound() throws Exception {
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound());

        verify(itemService).findById(99L);
    }

    @Test
    void updateItem_withExistingId_shouldReturnUpdatedItem() throws Exception {
        Item updatedItem = new Item(1L, "Updated Item", "Updated Description", "UPDATED", "updated@example.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemService.save(any(Item.class))).thenReturn(updatedItem);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Item")))
                .andExpect(jsonPath("$.status", is("UPDATED")));

        verify(itemService).findById(1L);
        verify(itemService).save(any(Item.class));
    }

    @Test
    void updateItem_withNonExistingId_shouldReturnNotFound() throws Exception {
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isNotFound());

        verify(itemService).findById(99L);
        verify(itemService, never()).save(any(Item.class));
    }

    @Test
    void deleteItem_shouldReturnNoContent() throws Exception {
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());

        verify(itemService).deleteById(1L);
    }

    @Test
    void processItems_shouldReturnCompletableFuture() throws Exception {
        CompletableFuture<List<Item>> future = CompletableFuture.completedFuture(testItems);
        when(itemService.processItemsAsync()).thenReturn(future);

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk());

        verify(itemService).processItemsAsync();
    }
}