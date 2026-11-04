package com.example.inventory;

import com.example.inventory.InventoryDtos.HistoryResponse;
import com.example.inventory.InventoryDtos.InventoryResponse;
import com.example.inventory.InventoryDtos.StockInRequest;
import com.example.inventory.InventoryDtos.UpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventories")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable("productId") @Min(1) long productId) {
        return service.get(productId);
    }

    @PostMapping("/{productId}/stock-in")
    public InventoryResponse stockIn(
            @PathVariable("productId") @Min(1) long productId,
            @Valid @RequestBody StockInRequest request) {
        return service.stockIn(productId, request.quantity());
    }

    @PutMapping("/{productId}")
    public InventoryResponse update(
            @PathVariable("productId") @Min(1) long productId,
            @Valid @RequestBody UpdateRequest request) {
        return service.update(productId, request.productName(), request.version());
    }

    @GetMapping("/{productId}/history")
    public List<HistoryResponse> history(
            @PathVariable("productId") @Min(1) long productId,
            @RequestParam(name = "offset", defaultValue = "0") @Min(0) int offset,
            @RequestParam(name = "limit", defaultValue = "50") @Min(1) @Max(200) int limit) {
        return service.history(productId, offset, limit);
    }
}
