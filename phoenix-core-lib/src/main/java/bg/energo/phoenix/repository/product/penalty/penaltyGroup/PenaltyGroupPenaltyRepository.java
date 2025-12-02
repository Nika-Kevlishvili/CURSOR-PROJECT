package bg.energo.phoenix.repository.product.penalty.penaltyGroup;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupPenalty;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupPenaltyQueryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PenaltyGroupPenaltyRepository extends JpaRepository<PenaltyGroupPenalty, Long> {

    @Query("""
            select pgp.id as id,
             pgp.penaltyId as penaltyId,
             p.name as penaltyName
             from PenaltyGroupPenalty pgp
             join Penalty p on p.id = pgp.penaltyId
             where pgp.penaltyGroupDetailId = :penaltyGroupDetailId and pgp.status in :statuses
            """)
    List<PenaltyGroupPenaltyQueryResponse> findAllGroupPenalties(@Param("penaltyGroupDetailId") Long penaltyGroupDetailId,@Param("statuses") List<EntityStatus> statuses);

    List<PenaltyGroupPenalty> findAllByPenaltyGroupDetailIdAndStatus(Long penaltyGroupDetailId, EntityStatus status);
}
