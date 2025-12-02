package bg.energo.phoenix.repository.nomenclature.contract;


import bg.energo.phoenix.model.entity.nomenclature.contract.DeactivationPurpose;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeactivationPurposeRepository extends JpaRepository<DeactivationPurpose, Long> {
    @Query(
            "select dp from DeactivationPurpose as dp" +
                    " where (:prompt is null or lower(dp.name) like :prompt)" +
                    " and (dp.status in (:statuses))" +
                    " and (:excludedItemId is null or dp.id <> :excludedItemId) " +
                    " ORDER BY CASE WHEN dp.isHardCoded = true THEN 0 WHEN dp.isDefault = true THEN 1 ELSE 2 END, dp.orderingId ASC"
    )
    Page<DeactivationPurpose> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(dp.id, dp.name, dp.orderingId, dp.isDefault, dp.status) " +
                    "from DeactivationPurpose as dp" +
                    " where (:prompt is null or lower(dp.name) like :prompt)" +
                    " and (dp.status in (:statuses))" +
                    " order by dp.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<DeactivationPurpose> findByIsDefaultTrue();

    @Query("select max(p.orderingId) from DeactivationPurpose p")
    Long findLastOrderingId();

    @Query(
            "select dp from DeactivationPurpose as dp" +
                    " where dp.id <> :currentId " +
                    " and (dp.orderingId >= :start and dp.orderingId <= :end) "
    )
    List<DeactivationPurpose> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select dp from DeactivationPurpose as dp" +
                    " where dp.orderingId is not null" +
                    " order by dp.name"
    )
    List<DeactivationPurpose> orderByName();

    @Query(
            """
                                select dp from DeactivationPurpose dp
                                where dp.id = :id
                                and dp.status in :statuses
                    """
    )
    Optional<DeactivationPurpose> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            """
                    select count(1) from DeactivationPurpose as dp
                    where lower(dp.name) = :name and dp.status in :statuses
                    """
    )
    Integer getExistingRecordsCountByName(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            d.id,
                            d.name
                        )
                        from DeactivationPurpose d
                        where d.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
            select count(cp.id) > 0
            from ContractPods cp
            join DeactivationPurpose dp on cp.deactivationPurposeId = dp.id
            join ProductContractDetails pcd on cp.contractDetailId = pcd.id
            join ProductContract pc on pc.id = pcd.contractId
            where dp.id = :id
            and pc.status = 'ACTIVE'
            and cp.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionsToPointOfDeliveries(Long id);
}
