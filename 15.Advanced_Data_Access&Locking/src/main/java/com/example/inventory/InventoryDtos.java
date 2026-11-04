package com.example.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class InventoryDtos {
    private InventoryDtos() {}

    public record StockInRequest(@NotNull @Positive Long quantity) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String productName,
            @NotNull @PositiveOrZero Long version) {}

    public record InventoryResponse(
            Long productId, String productName, long quantity, Long version) {
        public static InventoryResponse from(Inventory inventory) {
            return new InventoryResponse(inventory.getProductId(), inventory.getProductName(),
                    inventory.getQuantity(), inventory.getVersion());
        }
    }

    public record HistoryResponse(
            long revision, Instant changedAt, String revisionType,
            Long productId, String productName, long quantity, Long version) {}
}
