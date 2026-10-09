package bg.energo.phoenix.virtualpos.repository;

import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VirtualPosPaymentDetailsRepository extends JpaRepository<VirtualPosPaymentDetailsEntity, Long> {
    List<VirtualPosPaymentDetailsEntity> findByVirtualPosPaymentId(Long virtualPosPaymentId);

    List<VirtualPosPaymentDetailsEntity> findByLiabilityIdInAndVirtualPosPaymentId(
            @Param("liabilityId") List<Long> liabilityIds,
            @Param("virtualPosPaymentId") Long virtualPosPaymentId
    );
}


