package bg.energo.phoenix.repository.nomenclature.product.service;

import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceUnit;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceUnitRepository extends JpaRepository<ServiceUnit, Long> {

    @Query(
            """
            select su from ServiceUnit as su
                where (:prompt is null or lower(su.name) like :prompt)
                and su.status in (:statuses)
                and :excludedItemId is null or su.id <> :excludedItemId
                order by su.orderingId asc
            """
    )
    Page<ServiceUnit> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    @Query(
            """
            select su from ServiceUnit as su
                where su.id<> :currentId
                and (su.orderingId >= :start and su.orderingId <= :end)
            """
    )
    List<ServiceUnit> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
            select su from ServiceUnit as su
                where su.orderingId is not null
                order by su.name
            """
    )
    List<ServiceUnit> orderByName();


    @Query(
            """
            select su from ServiceUnit as su
                where (:prompt is null or lower(su.name) like :prompt)
                and su.status in (:statuses)
                and :excludedItemId is null or su.id <> :excludedItemId
                order by su.defaultSelection desc, su.orderingId asc
            """
    )
    Page<ServiceUnit> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
            select count(1) from ServiceUnit su
                where lower(su.name) = lower(:name)
                and su.status in :statuses
            """
    )
    Long countServiceUnitsByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
            select max(su.orderingId) from ServiceUnit su
            """
    )
    Long findLastOrderingId();

    Optional<ServiceUnit> findByDefaultSelectionTrue();

    Optional<ServiceUnit> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
            select count(1) from  ServiceUnit sa
             where sa.id = :id
             and
             ( exists
             (select 1 from EPService s
               join ServiceDetails sd on sd.service.id = s.id
                and sd.serviceUnit.id = sa.id
               where
                 sd.status in (:serviceDetailStatuses)
                 and s.status = 'ACTIVE'))
           """)
    Long activeConnectionCount(@Param("id") Long id,
                               @Param("serviceDetailStatuses") List<ServiceDetailStatus> serviceDetailStatuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from ServiceUnit s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
