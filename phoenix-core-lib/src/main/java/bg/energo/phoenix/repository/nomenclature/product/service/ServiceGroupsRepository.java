package bg.energo.phoenix.repository.nomenclature.product.service;

import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceGroups;
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
public interface ServiceGroupsRepository extends JpaRepository<ServiceGroups, Long> {
    @Query(
            "select sg from ServiceGroups as sg" +
                    " where (:prompt is null or lower(sg.name) like :prompt " +
                    " or (:prompt is null or lower(sg.nameTransliterated) like :prompt))" +
                    " and (sg.status in (:statuses))" +
                    " and (:excludedItemId is null or sg.id <> :excludedItemId) " +
                    " order by sg.defaultSelection desc, sg.orderingId asc"
    )
    Page<ServiceGroups> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(sg.id, sg.name, sg.orderingId, sg.defaultSelection, sg.status) " +
                    "from ServiceGroups as sg" +
                    " where (:prompt is null or lower(sg.name) like :prompt" +
                    " or (:prompt is null or lower(sg.nameTransliterated) like :prompt))" +
                    " and (:excludedItemId is null or sg.id <> :excludedItemId) " +
                    " and (sg.status in (:statuses))" +
                    " order by sg.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<ServiceGroups> findByDefaultSelectionTrue();

    @Query("select max(sg.orderingId) from ServiceGroups sg")
    Long findLastOrderingId();

    @Query(
            "select sg from ServiceGroups as sg" +
                    " where sg.id <> :currentId " +
                    " and (sg.orderingId >= :start and sg.orderingId <= :end) "
    )
    List<ServiceGroups> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select sg from ServiceGroups as sg
                where sg.orderingId is not null
                order by sg.name
            """
    )
    List<ServiceGroups> orderByName();

    @Query("""
             select sg from ServiceGroups sg
             where sg.id = :id and sg.status in(:statuses)
            """
    )
    Optional<ServiceGroups> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select sg from ServiceGroups sg
            where lower(sg.name) like lower(trim(trim(:name))) and sg.status in (:statuses)
            """)
    List<ServiceGroups> findByNameAndStatus(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select count(1) from  ServiceGroups sa
             where sa.id = :id
             and
             ( exists
             (select 1 from EPService s
               join ServiceDetails sd on sd.service.id = s.id
                and sd.serviceGroup.id = sa.id
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
                        from ServiceGroups s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
