package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private List<Item> testItems;

    @BeforeEach
    void setUp() {
        testItem = new Item(1L, "Test Item 1", "Description1", "NEW", "test1@example.com");
        Item secondItem = new Item(2L, "Test Item 2", "Description2", "NEW", "test2@example.com");
        testItems = Arrays.asList(testItem, secondItem);

        ReflectionTestUtils.setField(itemService, "executor", Executors.newFixedThreadPool(2));
    }

    @Test
    void findAll() {
        when(itemRepository.findAll()).thenReturn(testItems);

        List<Item> result = itemService.findAll();

        assertEquals(2, result.size());
        assertEquals("Test Item 1", result.get(0).getName());
        assertEquals("Test Item 2", result.get(1).getName());
        verify(itemRepository).findAll();
    }

    @Test
    void findById_withExistingId() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        Optional<Item> result = itemService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Item 1", result.get().getName());
        verify(itemRepository).findById(1L);
    }

    @Test
    void findById_withNonExistingId() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(99L);

        assertFalse(result.isPresent());
        verify(itemRepository).findById(99L);
    }

    @Test
    void saveItem() {
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        Item result = itemService.save(testItem);

        assertEquals("Test Item 1", result.getName());
        verify(itemRepository).save(testItem);
    }

    @Test
    void deleteItemById() {
        doNothing().when(itemRepository).deleteById(1L);

        itemService.deleteById(1L);

        verify(itemRepository).deleteById(1L);
    }

    @Test
    void processItemsAsync_allItems() throws ExecutionException, InterruptedException {
        List<Long> itemIds = Arrays.asList(1L, 2L);
        when(itemRepository.findAllIds()).thenReturn(itemIds);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(testItems.get(1)));

        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setStatus("PROCESSED");
            return item;
        });

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertEquals(2, processedItems.size());
        assertEquals("PROCESSED", processedItems.get(0).getStatus());
        assertEquals("PROCESSED", processedItems.get(1).getStatus());
        verify(itemRepository).findAllIds();
        verify(itemRepository, times(2)).findById(any(Long.class));
        verify(itemRepository, times(2)).save(any(Item.class));
    }

    @Test
    void processItemsAsync_nonExistingItems() throws ExecutionException, InterruptedException {
        List<Long> itemIds = Arrays.asList(1L, 99L);
        when(itemRepository.findAllIds()).thenReturn(itemIds);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setStatus("PROCESSED");
            return item;
        });

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertEquals(1, processedItems.size());
        assertEquals("PROCESSED", processedItems.get(0).getStatus());
        verify(itemRepository).findAllIds();
        verify(itemRepository, times(2)).findById(any(Long.class));
        verify(itemRepository, times(1)).save(any(Item.class));
    }
}