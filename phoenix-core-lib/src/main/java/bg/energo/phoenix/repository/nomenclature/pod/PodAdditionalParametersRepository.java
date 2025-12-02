package bg.energo.phoenix.repository.nomenclature.pod;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.pod.PodAdditionalParameters;
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

import java.util.List;
import java.util.Optional;

@Repository
public interface PodAdditionalParametersRepository extends JpaRepository<PodAdditionalParameters, Long> {

    @Query(
            "select p from PodAdditionalParameters as p" +
                    " where (:prompt is null or lower(p.name) like :prompt)" +
                    " and (p.status in (:statuses))" +
                    " and (:excludedItemId is null or p.id <> :excludedItemId) " +
                    " order by p.isDefault desc, p.orderingId asc"
    )
    Page<PodAdditionalParameters> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(p.id, p.name, p.orderingId, p.isDefault, p.status) " +
                    "from PodAdditionalParameters as p" +
                    " where (:prompt is null or lower(p.name) like :prompt)" +
                    " and (p.status in (:statuses))" +
                    " order by p.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<PodAdditionalParameters> findByIsDefaultTrue();

    @Query("select param from PodAdditionalParameters param where param.status = 'ACTIVE'and param.isDefault = true")
    Optional<PodAdditionalParameters> findDefaultSelection();

    @Query("select max(p.orderingId) from PodAdditionalParameters p")
    Long findLastOrderingId();

    @Query(
            "select p from PodAdditionalParameters as p" +
                    " where p.id <> :currentId " +
                    " and (p.orderingId >= :start and p.orderingId <= :end) "
    )
    List<PodAdditionalParameters> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select p from PodAdditionalParameters as p" +
                    " where p.orderingId is not null" +
                    " order by p.name"
    )
    List<PodAdditionalParameters> orderByName();

    @Query(
            """
                    select p from PodAdditionalParameters p
                    where p.id = :id
                    and p.status in :statuses
                    """
    )
    Optional<PodAdditionalParameters> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select count(1) from PodAdditionalParameters as p
                    where lower(p.name) = :name and p.status in :statuses
                    """
    )
    Integer getExistingRecordsCountByName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                        p.id,
                        p.name
                    )
                    from PodAdditionalParameters p
                    where p.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(pap.id, pap.name)
                from PodAdditionalParameters  pap
                where pap.name = :name
                and pap.status =:status
            """
    )
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findByNameAndStatus(@Param("name") String name,
                                              @Param("status") NomenclatureItemStatus status);
}
