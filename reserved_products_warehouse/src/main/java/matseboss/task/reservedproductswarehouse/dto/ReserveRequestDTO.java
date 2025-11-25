package matseboss.task.reservedproductswarehouse.dto;

public class ReserveRequestDTO {
    private int qty;

    public ReserveRequestDTO() { }

    public ReserveRequestDTO(int qty) {
        this.qty = qty;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }
}

