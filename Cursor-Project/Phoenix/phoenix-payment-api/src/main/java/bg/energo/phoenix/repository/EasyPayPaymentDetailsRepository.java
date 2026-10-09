package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.EasyPayPaymentDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EasyPayPaymentDetailsRepository extends JpaRepository<EasyPayPaymentDetailsEntity, Long> {
    List<EasyPayPaymentDetailsEntity> findByEasyPayPaymentId(Long easyPayPaymentId);

    List<EasyPayPaymentDetailsEntity> findByLiabilityIdInAndEasyPayPaymentId(
            @Param("liabilityId") List<Long> liabilityIds,
            @Param("easyPayPaymentId") Long easyPayPaymentId
    );

}
