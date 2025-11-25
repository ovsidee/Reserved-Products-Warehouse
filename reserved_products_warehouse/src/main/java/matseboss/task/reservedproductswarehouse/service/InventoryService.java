package matseboss.task.reservedproductswarehouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import matseboss.task.reservedproductswarehouse.entity.InventoryItem;
import matseboss.task.reservedproductswarehouse.entity.ItemReservedEvent;
import matseboss.task.reservedproductswarehouse.exception.ConflictException;
import matseboss.task.reservedproductswarehouse.exception.InsufficientStockException;
import matseboss.task.reservedproductswarehouse.exception.InvalidQuantityException;
import matseboss.task.reservedproductswarehouse.exception.NotFoundException;
import matseboss.task.reservedproductswarehouse.repository.ItemInventoryRepository;
import matseboss.task.reservedproductswarehouse.repository.ItemReservedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Service
public class InventoryService {

    private final TransactionTemplate transactionTemplate;
    private final ItemInventoryRepository itemRepository;
    private final ItemReservedEventRepository eventRepository;
    private final ObjectMapper mapper;

    public InventoryService(ItemInventoryRepository itemInventoryRepository,
                            ItemReservedEventRepository itemReservedEventRepository,
                            TransactionTemplate transactionTemplate) {
        this.itemRepository = itemInventoryRepository;
        this.eventRepository = itemReservedEventRepository;
        this.transactionTemplate = transactionTemplate;
        this.mapper = new ObjectMapper();
    }

    public void reserve(String sku, int qty) {
        if (qty <= 0) throw new InvalidQuantityException("qty must be > 0");

        for (int attempt = 1; attempt <= 3; attempt++) {
            InventoryItem current = itemRepository
                    .findById(sku)
                    .orElseThrow(() -> new NotFoundException("Provided item: " + sku + " not found."));

            if (current.getAvailable() < qty) {
                throw new InsufficientStockException("Not enough stock for this item: " + sku);
            }

            long expectedVersion = current.getVersion();
            int newAvailable = current.getAvailable() - qty;
            int newReserved = current.getReserved() + qty;

            boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                int updated = itemRepository.tryUpdate(sku, expectedVersion, newAvailable, newReserved);

                if (updated == 1) {
                    try {
                        String payload = mapper.writeValueAsString(Map.of(
                                "sku", sku,
                                "qty", qty,
                                "previousVersion", expectedVersion
                        ));
                        ItemReservedEvent newItemReservedEvent = new ItemReservedEvent(sku, "reservedItem", payload);
                        eventRepository.save(newItemReservedEvent);
                        return true;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw new RuntimeException("Failed to persist event", e);
                    }
                }
                return false;
            }));

            if (isSuccess) return;
        }

        throw new ConflictException("Could not reserve item due to concurrent updates (sku: " + sku + ")");
    }
}