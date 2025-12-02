package bg.energo.phoenix.repository.contract.activity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.activity.ProductContractActivity;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractActivityRepository extends JpaRepository<ProductContractActivity, Long> {

    Optional<ProductContractActivity> findBySystemActivityIdAndStatusIn(Long systemActivityId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        pca.createDate
                    )
                    from ProductContractActivity pca
                    join SystemActivity sa on sa.id = pca.systemActivityId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where pca.contractId = :productContractId
                        and sa.status in (:statuses)
                        order by pca.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findByContractIdAndStatusIn(
            @Param("productContractId") Long productContractId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
