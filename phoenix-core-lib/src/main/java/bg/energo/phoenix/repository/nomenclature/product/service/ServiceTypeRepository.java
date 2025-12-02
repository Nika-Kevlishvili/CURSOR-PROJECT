package bg.energo.phoenix.repository.nomenclature.product.service;

import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
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
public interface ServiceTypeRepository extends JpaRepository<ServiceType,Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                        s.id,
                        s.name,
                        s.orderingId,
                        s.defaultSelection,
                        s.status
                    )
                    from ServiceType s
                        where (:prompt is null or (lower(s.name) like :prompt))
                        and (s.status in (:statuses))
                    order by s.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query(value = """
                    select s from ServiceType s
                    where s.id=:id
                    and s.status in :statuses
            """)
    Optional<ServiceType> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                    select s from ServiceType s
                     where s.id <> :currentId
                     and (s.orderingId >= :start and s.orderingId <= :end)
                    """
    )
    List<ServiceType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            value = """
                                       select s from ServiceType as s
                                        where s.orderingId is not null
                                        order by s.name
                    """
    )
    List<ServiceType> orderByName();

    @Query(value = """
                    select s from ServiceType s
                    where s.name=:name
                    and s.status in :statuses
            """)
    Optional<ServiceType> findByNameAndStatuses(@Param("name") String name, List<NomenclatureItemStatus> statuses);

    @Query("select max(s.orderingId) from ServiceType s")
    Long findLastOrderingId();

    @Query(
            value = """
                    select s from ServiceType s
                        where (:prompt is null or (lower(s.name) like :prompt))
                        and (s.status in (:statuses))
                        and (:excludedItemId is null or s.id <> :excludedItemId)
                    order by s.defaultSelection desc, s.orderingId asc
                    """
    )
    Page<ServiceType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<ServiceType> findByDefaultSelectionTrue();


    @Query("""
             select count(1) from  ServiceType sa
             where sa.id = :id
             and
             ( exists
             (select 1 from EPService s
               join ServiceDetails sd on sd.service.id = s.id
                and sd.serviceType.id = sa.id
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
                        from ServiceType s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
