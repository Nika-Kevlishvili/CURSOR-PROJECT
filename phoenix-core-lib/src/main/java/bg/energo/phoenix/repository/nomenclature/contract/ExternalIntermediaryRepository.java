package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.contract.ExternalIntermediary;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalIntermediaryRepository extends JpaRepository<ExternalIntermediary, Long> {
    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "c.id, " +
//                    "c.name, " +
                    "CONCAT(c.name, ' (', c.identifier, ')'), " +
                    "c.orderingId, " +
                    "c.defaultSelection, " +
                    "c.status" +
                    ") " +
                    "from ExternalIntermediary as c" +
                    " where ((:prompt is null or lower(c.name) like :prompt)" +
                    " or (:prompt is null or lower(c.identifier) like :prompt))" +
                    " and (c.status in (:statuses))" +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query(
            "select c from ExternalIntermediary as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<ExternalIntermediary> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from ExternalIntermediary as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<ExternalIntermediary> orderByName();

    @Query(
            """
            select count(c.id) from ExternalIntermediary c
                where lower(c.name) = lower(:name)
                and c.status in (:statuses)
            """
    )
    Long countExternalIntermediaryByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
            select count(c.id) from ExternalIntermediary c
                where lower(c.identifier) = lower(:identifier)
                and c.status in (:statuses)
            """
    )
    Long countExternalIntermediaryByStatusAndIdentifier(
            @Param("identifier") String identifier,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query("select max(c.orderingId) from ExternalIntermediary c")
    Long findLastOrderingId();

    Optional<ExternalIntermediary> findByDefaultSelectionTrue();

    @Query(
            "select c from ExternalIntermediary as c" +
                    " where ((:prompt is null or lower(c.name) like :prompt) " +
                    " or (:prompt is null or lower(c.identifier) like :prompt))" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<ExternalIntermediary> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            e.id,
                            e.name
                        )
                        from ExternalIntermediary e
                        where e.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
            select ei.id from ExternalIntermediary ei
            where ei.id in(:externalIntermediaryIds)
            and ei.status in(:statuses)
            """)
    List<Long> findExistingByIdInAndStatusIn(List<Long> externalIntermediaryIds, List<NomenclatureItemStatus> statuses);


    @Query(
            value = """
                    select count(ei.id) > 0 from ExternalIntermediary ei
                        where ei.id = :id
                        and exists (
                            select 1 from ContractExternalIntermediary cei
                            join ProductContractDetails pcd on cei.contractDetailId = pcd.id
                            join ProductContract pc on pcd.contractId = pc.id
                                where cei.externalIntermediaryId = ei.id
                                and pc.status = 'ACTIVE'
                                and cei.status = 'ACTIVE'
                        )
                    """
    )
    boolean hasActiveConnectionToProductContract(Long id);


    @Query(
            value = """
                    select count(ei.id) > 0 from ExternalIntermediary ei
                        where ei.id = :id
                        and exists (
                            select 1 from ServiceContractExternalIntermediary cei
                            join ServiceContractDetails scd on cei.contractDetailId = scd.id
                            join ServiceContracts sc on scd.contractId = sc.id
                                where cei.externalIntermediaryId = ei.id
                                and sc.status = 'ACTIVE'
                                and cei.status = 'ACTIVE'
                        )
                    """
    )
    boolean hasActiveConnectionToServiceContract(Long id);


    @Query(
            value = """
                    select count(ei.id) > 0 from ExternalIntermediary ei
                        where ei.id = :id
                        and exists (
                            select 1 from ServiceOrderExternalIntermediary cei
                            join ServiceOrder so on so.id = cei.orderId
                                where cei.externalIntermediaryId = ei.id
                                and so.status = 'ACTIVE'
                                and cei.status = 'ACTIVE'
                        )
                    """
    )
    boolean hasActiveConnectionToServiceOrder(Long id);


    @Query(
            value = """
                    select count(ei.id) > 0 from ExternalIntermediary ei
                        where ei.id = :id
                        and exists (
                            select 1 from GoodsOrderExternalIntermediary cei
                            join GoodsOrder go on go.id = cei.orderId
                                where cei.externalIntermediaryId = ei.id
                                and go.status = 'ACTIVE'
                                and cei.status = 'ACTIVE'
                        )
                    """
    )
    boolean hasActiveConnectionToGoodsOrder(Long id);

    Optional<ExternalIntermediary> findByNameAndStatus(String name, NomenclatureItemStatus active);


    @Query("""
            select new bg.energo.phoenix.model.CacheObject(ei.id,ei.name) 
            from ExternalIntermediary ei 
            where ei.name in (:intermediaries)
            and ei.status = 'ACTIVE'
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObject> findCacheObjectByNames(Collection<String> intermediaries);
}
