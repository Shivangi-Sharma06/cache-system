package com.cse309.cache.threading;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedBufferTest {

    @Test
    void testBasicPutTakeAndClose() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(2);
        buffer.put("A");
        buffer.put("B");

        assertEquals(2, buffer.size());
        assertEquals("A", buffer.take());

        buffer.close();
        assertTrue(buffer.isClosed());

        // Can still take remaining elements
        assertEquals("B", buffer.take());

        // Buffer empty & closed returns null
        assertNull(buffer.take());

        // Put into closed buffer throws
        assertThrows(IllegalStateException.class, () -> buffer.put("C"));
    }

    @Test
    void testMultiProducerMultiConsumerNoLoss() throws InterruptedException {
        int itemsPerProducer = 1000;
        int numProducers = 4;
        int numConsumers = 4;
        int totalItems = numProducers * itemsPerProducer;

        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(50);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(numConsumers);

        // Start consumers
        List<Thread> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            Thread c = new Thread(() -> {
                try {
                    startGate.await();
                    while (true) {
                        Integer val = buffer.take();
                        if (val == null) break;
                        consumed.add(val);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishGate.countDown();
                }
            });
            c.start();
            consumers.add(c);
        }

        // Start producers
        List<Thread> producers = new ArrayList<>();
        for (int p = 0; p < numProducers; p++) {
            final int base = p * itemsPerProducer;
            Thread prod = new Thread(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < itemsPerProducer; i++) {
                        buffer.put(base + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            prod.start();
            producers.add(prod);
        }

        startGate.countDown();

        for (Thread p : producers) {
            p.join(5000);
        }
        buffer.close();

        finishGate.await();
        for (Thread c : consumers) {
            c.join(5000);
        }

        assertEquals(totalItems, consumed.size());
    }
}
