package matseboss.task.reservedproductswarehouse.web.controller;

import matseboss.task.reservedproductswarehouse.dto.ReserveRequestDTO;
import matseboss.task.reservedproductswarehouse.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService service) {
        this.inventoryService = service;
    }

    @PostMapping("/{sku}/reserve")
    public ResponseEntity<?> reserve(@PathVariable String sku,
                                     @RequestBody ReserveRequestDTO request) {
        inventoryService.reserve(sku, request.getQty());
        return ResponseEntity.ok("Item reserved successfully");
    }
}
