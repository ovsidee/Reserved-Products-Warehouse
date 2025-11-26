package matseboss.task.reservedproductswarehouse.service;

import matseboss.task.reservedproductswarehouse.entity.InventoryItem;
import matseboss.task.reservedproductswarehouse.entity.ItemReservedEvent;
import matseboss.task.reservedproductswarehouse.exception.ConflictException;
import matseboss.task.reservedproductswarehouse.exception.InsufficientStockException;
import matseboss.task.reservedproductswarehouse.exception.InvalidQuantityException;
import matseboss.task.reservedproductswarehouse.exception.NotFoundException;
import matseboss.task.reservedproductswarehouse.repository.ItemInventoryRepository;
import matseboss.task.reservedproductswarehouse.repository.ItemReservedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ItemInventoryRepository itemRepository;

    @Mock
    private ItemReservedEventRepository eventRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(itemRepository, eventRepository, transactionTemplate);

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
    }

    @Test
    void reserve_ShouldSucceed_WhenStockIsAvailableAndNoConcurrentModifications() {
        String sku = "someSku";
        InventoryItem item = new InventoryItem(sku, 10, 0, 1L);

        when(itemRepository.findById(sku)).thenReturn(Optional.of(item));
        when(itemRepository.tryUpdate(sku, 1L, 5, 5)).thenReturn(1);

        service.reserve(sku, 5);

        verify(itemRepository, times(1)).tryUpdate(sku, 1L, 5, 5);

        verify(eventRepository, times(1)).save(any(ItemReservedEvent.class));
    }

    @Test
    void reserve_ShouldRetryAndSucceed_WhenConcurrentModificationOccursOnce() {
        String sku = "someSku";

        InventoryItem itemV1 = new InventoryItem(sku, 10, 0, 1L);
        InventoryItem itemV2 = new InventoryItem(sku, 8, 2, 2L);

        when(itemRepository.findById(sku))
                .thenReturn(Optional.of(itemV1))
                .thenReturn(Optional.of(itemV2));

        when(itemRepository.tryUpdate(sku, 1L, 5, 5)).thenReturn(0);
        when(itemRepository.tryUpdate(sku, 2L, 3, 7)).thenReturn(1);

        service.reserve(sku, 5);

        verify(itemRepository, times(2)).findById(sku);
        verify(itemRepository, times(2)).tryUpdate(anyString(), anyLong(), anyInt(), anyInt());

        verify(eventRepository, times(1)).save(any(ItemReservedEvent.class));
    }

    @Test
    void reserve_ShouldThrowConflictException_AfterThreeFailedAttempts() {
        String sku = "someSku";
        InventoryItem item = new InventoryItem(sku, 10, 0, 1L);

        when(itemRepository.findById(sku)).thenReturn(Optional.of(item));
        when(itemRepository.tryUpdate(anyString(), anyLong(), anyInt(), anyInt())).thenReturn(0);

        assertThrows(ConflictException.class, () -> service.reserve(sku, 5));

        verify(itemRepository, times(3)).findById(sku);
        verify(itemRepository, times(3)).tryUpdate(anyString(), anyLong(), anyInt(), anyInt());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void reserve_ShouldThrowInsufficientStockException_WhenAvailableIsLow() {
        String sku = "someSku";
        InventoryItem item = new InventoryItem(sku, 2, 0, 1L);

        when(itemRepository.findById(sku)).thenReturn(Optional.of(item));

        assertThrows(InsufficientStockException.class, () -> service.reserve(sku, 5));

        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void reserve_ShouldThrowNotFoundException_WhenItemDoesNotExist() {
        String sku = "skuThatDoesNotExist";

        when(itemRepository.findById(sku)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.reserve(sku, 5));
    }

    @Test
    void reserve_ShouldThrowInvalidQuantityException_WhenQtyIsZeroOrNegative() {
        assertThrows(InvalidQuantityException.class, () -> service.reserve("sku", 0));
        assertThrows(InvalidQuantityException.class, () -> service.reserve("sku", -1));
    }

    @Test
    void reserve_ShouldPersistCorrectEventPayload() {
        String sku = "someSku";
        int qty = 5;
        InventoryItem item = new InventoryItem(sku, 10, 0, 100L);

        when(itemRepository.findById(sku)).thenReturn(Optional.of(item));
        when(itemRepository.tryUpdate(anyString(), anyLong(), anyInt(), anyInt())).thenReturn(1);

        service.reserve(sku, qty);

        ArgumentCaptor<ItemReservedEvent> eventCaptor = ArgumentCaptor.forClass(ItemReservedEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        ItemReservedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(sku, capturedEvent.getSku());
        assertEquals("reservedItem", capturedEvent.getType());
        assertTrue(capturedEvent.getPayload().contains("\"qty\":5"));
        assertTrue(capturedEvent.getPayload().contains("\"previousVersion\":100"));
    }

}