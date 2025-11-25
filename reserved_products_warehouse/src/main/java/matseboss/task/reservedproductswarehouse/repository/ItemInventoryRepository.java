package matseboss.task.reservedproductswarehouse.repository;

import matseboss.task.reservedproductswarehouse.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ItemInventoryRepository extends JpaRepository<InventoryItem, String> {

    @Modifying
    @Transactional
    @Query("""
        UPDATE InventoryItem i
        SET i.available = :available,
            i.reserved = :reserved,
            i.version = i.version + 1
        WHERE i.sku = :sku AND i.version = :version
    """)
    int tryUpdate(
            @Param("sku") String sku,
            @Param("version") long version,
            @Param("available") int available,
            @Param("reserved") int reserved
    );
}
