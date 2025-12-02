package bg.energo.phoenix.repository.nomenclature.pod;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.pod.UserType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
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
public interface UserTypeRepository extends JpaRepository<UserType, Long> {

    Optional<UserType> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);


    @Query(
            value = """
                    select ut from UserType as ut
                        where (:prompt is null or lower(ut.name) like :prompt)
                        and ut.status in (:statuses)
                        and (:excludedItemId is null or ut.id <> :excludedItemId)
                        order by ut.defaultSelection desc, ut.orderingId asc
                    """
    )
    Page<UserType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    @Query(
            value = """
                    select ut from UserType as ut
                        where (:prompt is null or lower(ut.name) like :prompt)
                        and ut.status in (:statuses)
                        and (:excludedItemId is null or ut.id <> :excludedItemId)
                        order by ut.defaultSelection desc, ut.orderingId asc
                    """
    )
    Page<UserType> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    Optional<UserType> findByDefaultSelectionTrue();


    @Query("select max(ut.orderingId) from UserType ut")
    Long findLastOrderingId();


    @Query(
            value = """
                    select ut from UserType ut
                        where ut.id <> :currentId
                        and ut.orderingId between :start and :end
                    """
    )
    List<UserType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );


    @Query(
            value = """
                    select ut from UserType ut
                        where ut.orderingId is not null
                        order by ut.name
                    """
    )
    List<UserType> orderByName();


    @Query(
            value = """
                    select count(ut.id) > 0 from UserType ut
                        where lower(ut.name) = lower(:name)
                        and ut.status in (:statuses)
                    """
    )
    boolean existsByName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );


    @Query(
            value = """
                    select count(ut.id) > 0 from UserType ut
                        where ut.id = :userTypeId
                        and exists (select 1 from PointOfDeliveryDetails pd
                            join PointOfDelivery p on pd.podId = p.id
                                where pd.userTypeId = ut.id
                                and p.status = 'ACTIVE')
                    """
    )
    boolean hasActiveConnections(@Param("userTypeId") Long userTypeId);


    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(g.id, g.name)
        from UserType g
        where g.name = :name
        and g.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus(@Param("name")String name, @Param("status") NomenclatureItemStatus status);


    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            u.id,
                            u.name
                        )
                        from UserType u
                        where u.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}