package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxyFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderProxyFileRepository extends JpaRepository<ServiceOrderProxyFile, Long> {

    List<ServiceOrderProxyFile> findByOrderProxyIdNullAndStatusIn(List<EntityStatus> statuses);

    List<ServiceOrderProxyFile> findAllByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    List<ServiceOrderProxyFile> findByOrderProxyIdAndStatusIn(Long orderProxyId, List<EntityStatus> statuses);

}
