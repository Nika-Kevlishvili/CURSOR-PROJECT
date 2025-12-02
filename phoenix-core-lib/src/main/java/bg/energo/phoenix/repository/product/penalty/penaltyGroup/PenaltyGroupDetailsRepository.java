package bg.energo.phoenix.repository.product.penalty.penaltyGroup;

import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PenaltyGroupDetailsRepository extends JpaRepository<PenaltyGroupDetails, Long> {
    Optional<PenaltyGroupDetails> findByPenaltyGroupIdAndVersionId(Long penaltyGroupId, Integer versionId);
    Optional<PenaltyGroupDetails> findFirstByPenaltyGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(Long penaltyGroupId, LocalDate startDate);

    List<PenaltyGroupDetails> findAllByPenaltyGroupIdOrderByStartDateDesc(Long penaltyGroupId);

    boolean existsByPenaltyGroupIdAndStartDate(Long penaltyGroupId, LocalDate startDate);

    @Query("select max(pgd.versionId) from PenaltyGroupDetails pgd where pgd.penaltyGroupId = :penaltyGroupId")
    Long findLastVersionByPenaltyGroupId(Long penaltyGroupId);

    @Query("""
            select pgd
            from PenaltyGroupDetails pgd
            where pgd.penaltyGroupId =:id
            order by pgd.startDate ASC
            """)
    List<PenaltyGroupDetails> findByCopyGroupBaseRequest(@Param("id") Long id);


}
