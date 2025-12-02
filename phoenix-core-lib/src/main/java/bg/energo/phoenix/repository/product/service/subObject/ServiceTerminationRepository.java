package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.entity.product.service.ServiceTermination;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTerminationRepository extends JpaRepository<ServiceTermination, Long> {

    List<ServiceTermination> findByServiceDetailsIdAndStatusIn(Long serviceDetailId, List<ServiceSubobjectStatus> statuses);

}
