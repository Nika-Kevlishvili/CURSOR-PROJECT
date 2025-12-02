package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.product.service.ServiceSegment;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceSegmentsRepository extends JpaRepository<ServiceSegment, Long> {

    List<ServiceSegment> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);

    List<ServiceSegment> findAllByServiceDetailsIdAndStatus(Long id, ServiceSubobjectStatus productSubObjectStatus);
}
