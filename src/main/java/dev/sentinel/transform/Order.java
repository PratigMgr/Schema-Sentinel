package dev.sentinel.transform;

import java.math.BigDecimal;
import java.util.List;

public record Order(long id, String customer, BigDecimal total, String currency, List<Item> items) {
    public record Item(String sku, int qty) {}
}
