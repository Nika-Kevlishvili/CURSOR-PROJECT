package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderPriceComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderPriceComponentRepository extends JpaRepository<ServiceOrderPriceComponent, Long> {

    List<ServiceOrderPriceComponent> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);

}
