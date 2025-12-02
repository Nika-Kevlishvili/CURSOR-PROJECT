package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.ContractVersionTypesResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractVersionTypesRepository extends JpaRepository<ContractVersionType, Long> {
    Optional<ContractVersionType> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statusList);

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.contract.ContractVersionTypesResponse(cvt) from ContractVersionType cvt " +
                    " where (:prompt is null or lower(cvt.name) like :prompt)" +
                    " and (((cvt.status in (:statuses))" +
                    " and (:excludedItemId is null or cvt.id <> :excludedItemId)) " +
                    " or (cvt.id in (:includedItemIds)))" +
                    " order by case when cvt.id in (:includedItemIds) then 1 else 2 end," +
                    " case when cvt.isHardCoded = true then 0 when cvt.isDefault = true then 1 else 2 END , cvt.orderingId asc"
    )
    Page<ContractVersionTypesResponse> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query("""
            select count(c) from ContractVersionType c
            where c.name like (:name)
            and c.status in(:statuses)
            """)
    long countContractVersionTypesByNameAndStatus(String name, List<NomenclatureItemStatus> statuses);

    @Query("""
            select max(cvt.orderingId) from ContractVersionType cvt
            """)
    Long findLastOrderingId();

    Optional<ContractVersionType> findByIsDefaultTrue();


    @Query("""
            select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                cvt.id,
                cvt.name,
                cvt.orderingId,
                cvt.isDefault,
                cvt.status
            ) from ContractVersionType cvt
            where (:prompt is null or lower(cvt.name) like :prompt)
            and cvt.status in(:statuses)
            order by cvt.isDefault desc, cvt.orderingId asc
            """)
    Page<NomenclatureResponse> filterNomenclature(String prompt, List<NomenclatureItemStatus> statuses, Pageable pageRequest);

    @Query("""
            select cvt from ContractVersionType cvt
            where cvt.id <> :currentId
            and (cvt.orderingId >= :start and cvt.orderingId <= :end)
            """)
    List<ContractVersionType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select cvt from ContractVersionType cvt
            where cvt.orderingId is not null
            order by cvt.name
            """)
    List<ContractVersionType> orderByName();

    @Query("""
            select (count(c) > 0) from ContractVersionType c
            where c.id = :id
            and c.status in(:statuses)
            """)
    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        ) from ContractVersionType c
            where c.id in(:ids)
            """)
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
            select (count(cvt.id) > 0) from ContractVersionType cvt
            where cvt.id = :id
            and
            (
                exists (
                    select 1 from ProductContractVersionTypes pcvt
                    join ProductContractDetails pcd on pcd.id = pcvt.contractDetailId
                    join ProductContract pc on pc.id = pcd.contractId
                    where pcvt.contractVersionTypeId = cvt.id
                    and pcvt.status = 'ACTIVE'
                    and pc.status = 'ACTIVE'
                )
                or exists (
                    select 1 from ServiceContractContractVersionTypes scvt
                    join ServiceContractDetails scd on scd.id = scvt.contractDetailId
                    join ServiceContracts sc on sc.id = scd.contractId
                    where scvt.contractVersionTypeId = cvt.id
                    and scvt.status = 'ACTIVE'
                    and sc.status = 'ACTIVE'
                )
            )
            """)
    boolean hasActiveConnections(Long id);

    Optional<ContractVersionType> findByNameAndStatusIn(String name, List<NomenclatureItemStatus> active);

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(cvt.id, cvt.name)
                from ContractVersionType cvt
                where cvt.status in (:statuses)
                and cvt.name=:interestRateName
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(@Param("interestRateName") String interestRateName,
                                                @Param("statuses") List<NomenclatureItemStatus> statuses);
}
