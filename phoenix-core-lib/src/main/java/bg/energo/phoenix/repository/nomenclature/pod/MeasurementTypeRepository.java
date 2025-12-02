package bg.energo.phoenix.repository.nomenclature.pod;

import bg.energo.phoenix.model.entity.nomenclature.pod.MeasurementType;
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
public interface MeasurementTypeRepository extends JpaRepository<MeasurementType, Long> {

    @Query(
            """ 
                    select c from MeasurementType as c
                        where (:prompt is null or lower(c.name) like :prompt)
                        and (c.status in (:statuses))
                        and (:excludedItemId is null or c.id <> :excludedItemId)
                        order by c.isDefault desc, c.orderingId asc
                     """
    )
    Page<MeasurementType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(p.id, p.name, p.orderingId, p.isDefault, p.status)
                        from MeasurementType as p
                        where (:prompt is null or lower(p.name) like :prompt)
                        and (p.status in (:statuses))
                        order by p.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query(
            """
                    select count(c.id) from MeasurementType c
                        where lower(c.name) = lower(:name)
                        and c.status in (:statuses)
                    """
    )
    Long countMeasurementTypeByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<MeasurementType> findByIsDefaultTrue();

    @Query("select max(c.orderingId) from MeasurementType c")
    Long findLastOrderingId();

    @Query(
            """
                    select c from MeasurementType as c
                        where c.id <> :currentId
                        and (c.orderingId >= :start and c.orderingId <= :end)
                    """

    )
    List<MeasurementType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select c from MeasurementType as c
                        where c.orderingId is not null
                        order by c.name
                    """
    )
    List<MeasurementType> orderByName();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from MeasurementType c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    Optional<MeasurementType> findByIdAndStatus(Long id, NomenclatureItemStatus status);

    Optional<MeasurementType> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> status);

    Optional<MeasurementType> findByNameAndStatus (String name, NomenclatureItemStatus status);
}
