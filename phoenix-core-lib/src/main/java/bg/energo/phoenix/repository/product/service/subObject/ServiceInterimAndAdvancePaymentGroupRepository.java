package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.entity.product.service.ServiceInterimAndAdvancePaymentGroup;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceInterimAndAdvancePaymentGroupRepository extends JpaRepository<ServiceInterimAndAdvancePaymentGroup, Long> {

    List<ServiceInterimAndAdvancePaymentGroup> findAllByServiceDetailsIdAndStatusIn(Long serviceDetailId, List<ServiceSubobjectStatus> statuses);

}
