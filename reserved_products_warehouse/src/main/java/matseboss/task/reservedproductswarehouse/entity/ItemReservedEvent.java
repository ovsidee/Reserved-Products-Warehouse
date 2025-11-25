package matseboss.task.reservedproductswarehouse.entity;

import jakarta.persistence.*;
import matseboss.task.reservedproductswarehouse.converter.JsonMapConverter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "item_reserved_event")
public class ItemReservedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @Convert(converter = JsonMapConverter.class)
    private Map<String,Object> payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ItemReservedEvent() {}

    public ItemReservedEvent(String sku, String type, Map<String, Object> payload) {
        this.sku = sku;
        this.type = type;
        this.payload = payload;
    }


    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}