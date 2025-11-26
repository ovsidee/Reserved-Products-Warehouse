package matseboss.task.reservedproductswarehouse.controller;

import matseboss.task.reservedproductswarehouse.exception.ConflictException;
import matseboss.task.reservedproductswarehouse.exception.InsufficientStockException;
import matseboss.task.reservedproductswarehouse.exception.InvalidQuantityException;
import matseboss.task.reservedproductswarehouse.exception.NotFoundException;
import matseboss.task.reservedproductswarehouse.service.InventoryService;
import matseboss.task.reservedproductswarehouse.web.GlobalExceptionHandler;
import matseboss.task.reservedproductswarehouse.web.controller.InventoryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import(GlobalExceptionHandler.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void shouldReturn404_WhenItemNotFound() throws Exception {
        doThrow(new NotFoundException("Item not found"))
                .when(inventoryService).reserve(anyString(), anyInt());

        mockMvc.perform(post("/inventory/unknown-sku/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\": 5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Item not found"));
    }

    @Test
    void shouldReturn400_WhenQuantityIsInvalid() throws Exception {
        doThrow(new InvalidQuantityException("qty must be > 0"))
                .when(inventoryService).reserve(anyString(), anyInt());

        mockMvc.perform(post("/inventory/sku-1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("qty must be > 0"));
    }

    @Test
    void shouldReturn400_WhenStockIsInsufficient() throws Exception {
        doThrow(new InsufficientStockException("Not enough stock"))
                .when(inventoryService).reserve(anyString(), anyInt());

        mockMvc.perform(post("/inventory/sku-1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\": 100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not enough stock"));
    }

    @Test
    void shouldReturn409_WhenConflictOccurs() throws Exception {
        doThrow(new ConflictException("Concurrent modification detected"))
                .when(inventoryService).reserve(anyString(), anyInt());

        mockMvc.perform(post("/inventory/sku-1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qty\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Concurrent modification detected"));
    }

    @Test
    void shouldReturn200_WhenReservationIsSuccessful() throws Exception {
        doNothing().when(inventoryService).reserve(anyString(), anyInt());

        mockMvc.perform(post("/inventory/sku-1/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qty\": 5}"))
           .andExpect(status().isOk())
           .andExpect(content().string("Item reserved successfully"));
    }
}
