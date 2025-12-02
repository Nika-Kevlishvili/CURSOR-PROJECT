package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "c.id, " +
                    "c.name, " +
                    "c.orderingId, " +
                    "c.defaultSelection, " +
                    "c.status" +
                    ") " +
                    "from Campaign as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Campaign> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            "select c from Campaign as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<Campaign> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select c from Campaign as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<Campaign> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from Campaign as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<Campaign> orderByName();

    @Query(
            """
                    select count(c.id) from Campaign c
                        where lower(c.name) = lower(:name)
                        and c.status in (:statuses)
                    """
    )
    Long countCampaignByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query("select max(c.orderingId) from Campaign c")
    Long findLastOrderingId();

    Optional<Campaign> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from Campaign c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
            select count(pcd.id) > 0
            from ProductContractDetails pcd
            join ProductContract pc on pcd.contractId = pc.id
            join Campaign c on pcd.campaignId = c.id
            where c.id = :id
            and pc.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionsWithProductContract(Long id);

    @Query("""
            select count(scd.id) > 0
            from ServiceContractDetails scd
            join ServiceContracts sc on scd.contractId = sc.id
            join Campaign c on scd.campaignId = c.id
            where c.id = :id
            and sc.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionsWithServiceContracts(Long id);

    @Query("""
            select count(so.id) > 0
            from ServiceOrder so
            join Campaign c on so.campaignId = c.id
            where c.id = :id
            and so.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionsWithServiceOrder(Long id);

    @Query("""
            select count(go.id) > 0
            from GoodsOrder go
            join Campaign c on go.campaignId = c.id
            where c.id = :id
            and go.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionsWithGoodsOrder(Long id);
}
