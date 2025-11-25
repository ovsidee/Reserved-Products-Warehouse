package matseboss.task.reservedproductswarehouse.repository;

import matseboss.task.reservedproductswarehouse.entity.ItemReservedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemReservedEventRepository extends JpaRepository<ItemReservedEvent, Long> {

}