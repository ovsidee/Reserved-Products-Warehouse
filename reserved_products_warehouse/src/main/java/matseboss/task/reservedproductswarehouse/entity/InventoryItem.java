package matseboss.task.reservedproductswarehouse.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "available", nullable = false)
    private int available;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Column(name = "version", nullable = false)
    private long version;

    protected InventoryItem() {}

    public InventoryItem(String sku, int available, int reserved, Long version) {
        this.sku = sku;
        this.available = available;
        this.reserved = reserved;
        this.version = version;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public int getReserved() {
        return reserved;
    }

    public Long getVersion() {
        return version;
    }
}
