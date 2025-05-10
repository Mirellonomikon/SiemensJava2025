package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private List<Item> processedItems = new ArrayList<>();
    private int processedCount = 0;


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     * <p>
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     * <p>
     * <p>
     * <p>
     * <p>
     * Issues fixed from original implementation:
     * Method returning immediately without waiting for tasks to complete
     * No thread safety for shared lists and counter
     * Better exception handling (thread interruption info was lost before)
     * No coordination between the tasks
     * Processed items are now being tracked properly
     */

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        // AtomicInteger instead of int for thread safety
        AtomicInteger processedCounter = new AtomicInteger(0);

        // Synchronised list for thread safe processing of items
        List<Item> syncProcessedItems = Collections.synchronizedList(new ArrayList<>());

        // Array of CompletableFutures for each task
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Long id : itemIds) {
            //Create a CompletableFuture for each task
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);
                    //Uses optional to avoid a NullPointerException and for easier maintenance
                    Optional<Item> optionalItem = itemRepository.findById(id);
                    if (optionalItem.isPresent()) {
                        Item item = optionalItem.get();
                        item.setStatus("PROCESSED");
                        Item savedItem = itemRepository.save(item);
                        //add to synchronised list to maintain thread compatibility
                        syncProcessedItems.add(savedItem);
                        // Print processed item id
                        System.out.println("Processed item ID: " + savedItem.getId() +
                                "\nThread: " + Thread.currentThread().getName());
                        //update class field
                        processedCount = processedCounter.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    //Make sure the interrupt status is not lost
                    Thread.currentThread().interrupt();
                    throw new CompletionException("Thread interrupted", e);
                } catch (Exception e) {
                    // Catch the rest of the errors and display the item id where the error occurred
                    throw new CompletionException("Error on item: " + id, e);
                }
            }, executor);

            futures.add(future);
        }

        //Join all threads and create a CompletableFuture that completes when all tasks are done
        CompletableFuture<Void> done = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        //Return the result and update class variable
        return done.thenApply(v -> {
            processedItems = new ArrayList<>(syncProcessedItems);
            return syncProcessedItems;
        });
    }

}

