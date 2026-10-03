package com.learning.spring.aop;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Pure business code. No logging or timing code in here: the aspect adds it.
 */
@Service
public class InventoryService {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of("SKU-1", 10, "SKU-2", 3));

    @LogExecutionTime
    public int reserve(String sku, int qty) {
        int available = stock.getOrDefault(sku, 0);
        if (qty > available) throw new IllegalStateException("Only " + available + " left of " + sku);
        stock.put(sku, available - qty);
        return available - qty;
    }

    // SELF-INVOCATION: the inner reserve() calls use 'this', NOT the proxy,
    // so they are NOT timed or logged. Only reserveAll itself is intercepted.
    public int reserveAll(List<String> skus) {
        int reserved = 0;
        for (String sku : skus) {
            this.reserve(sku, 1);
            reserved++;
        }
        return reserved;
    }
}
