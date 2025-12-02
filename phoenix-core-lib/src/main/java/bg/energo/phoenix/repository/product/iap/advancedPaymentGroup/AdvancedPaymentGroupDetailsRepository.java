package bg.energo.phoenix.repository.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdvancedPaymentGroupDetailsRepository extends JpaRepository<AdvancedPaymentGroupDetails, Long> {
    List<AdvancedPaymentGroupDetails> findAllByAdvancedPaymentGroupId(Long id);

    Optional<AdvancedPaymentGroupDetails> findByAdvancedPaymentGroupIdAndVersionId(Long id, Long version);

    Optional<AdvancedPaymentGroupDetails> findFirstByAdvancedPaymentGroupIdOrderByVersionIdDesc(Long id);

    Optional<List<AdvancedPaymentGroupDetails>> findByAdvancedPaymentGroupIdOrderByStartDateAsc(Long id);

    @Query("select max (apgd.versionId) from AdvancedPaymentGroupDetails apgd where apgd.advancedPaymentGroupId = :advancedPaymentGroupId")
    Long findLastVersionByAdvancedPaymentGroupId(Long advancedPaymentGroupId);

    boolean existsByAdvancedPaymentGroupIdAndStartDate(Long id, LocalDate startDate);

    @Query("""
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse(apgd.id,apgd.versionId,apgd.startDate)
            from AdvancedPaymentGroup apg
            join AdvancedPaymentGroupDetails apgd on apg.id = apgd.advancedPaymentGroupId
            where apg.id=:id
            order by apgd.startDate ASC
            """)
    List<CopyDomainWithVersionMiddleResponse> findByCopyDomainWithVersionBaseRequest(@Param("id")Long id);

    Optional<AdvancedPaymentGroupDetails> findFirstByAdvancedPaymentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(Long advancedPaymentGroupId, LocalDate startDate);
}
