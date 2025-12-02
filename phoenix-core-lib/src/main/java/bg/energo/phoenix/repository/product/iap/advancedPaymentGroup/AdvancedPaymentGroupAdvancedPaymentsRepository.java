package bg.energo.phoenix.repository.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupAdvancedPayments;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdvancedPaymentGroupAdvancedPaymentsRepository extends JpaRepository<AdvancedPaymentGroupAdvancedPayments, Long> {

    List<AdvancedPaymentGroupAdvancedPayments> findAdvancedPaymentGroupAdvancedPaymentsByAdvancePaymentGroupDetailIdInAndStatusIn(List<Long> groupDetailIds, List<AdvancedPaymentGroupStatus> status);
    List<AdvancedPaymentGroupAdvancedPayments> findByAdvancePaymentIdAndStatus(Long id,AdvancedPaymentGroupStatus status);
    Optional<List<AdvancedPaymentGroupAdvancedPayments>> findByAdvancePaymentGroupDetailIdAndStatusIn(Long id, List<AdvancedPaymentGroupStatus> statuses);
    Optional<List<AdvancedPaymentGroupAdvancedPayments>> findByAdvancePaymentIdInAndStatusIn(List<Long> id, List<AdvancedPaymentGroupStatus> statuses);

    List<AdvancedPaymentGroupAdvancedPayments> findAllByAdvancePaymentGroupDetailIdAndStatus(Long id, AdvancedPaymentGroupStatus statuses);

    boolean existsByAdvancePaymentIdAndStatus(Long advancedPaymentId, AdvancedPaymentGroupStatus status);

}
