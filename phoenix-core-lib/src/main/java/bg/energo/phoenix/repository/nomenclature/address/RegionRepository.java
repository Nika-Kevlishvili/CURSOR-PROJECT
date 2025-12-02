package bg.energo.phoenix.repository.nomenclature.address;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.address.Region;
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
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long>, QueryByExampleExecutor<Region> {

    @Query("select r from Region as r" +
                    " left join r.country as rc " +
                    " where (r.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        "lower(r.name) like :prompt)) " +
//                        "lower(r.name) like :prompt or " +
//                        "lower(rc.name) like :prompt))" +
                    " and (:countryId is null or rc.id = :countryId)" +
                    " and (:excludedItemId is null or r.id <> :excludedItemId) " +
                    " order by r.defaultSelection desc, r.orderingId asc "
    )
    Page<Region> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("countryId") Long countryId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                        "r.id, " +
                        "CONCAT(r.name, ' - ', rc.name), " +
                        "r.orderingId, " +
                        "r.defaultSelection, " +
                        "r.status" +
                    ") " +
                    "from Region as r" +
                    " left join r.country as rc " +
                    " where (r.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        "lower(r.name) like :prompt)) " +
//                        "lower(r.name) like :prompt or " +
//                        "lower(rc.name) like :prompt)) " +
                    " and (:excludedItemId is null or r.id <> :excludedItemId) " +
                    " order by r.defaultSelection desc,r.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<Region> findByDefaultSelectionTrue();

    @Query(value = "select max(r.orderingId) from Region r")
    Long findLastOrderingId();

    @Query(
            "select r from Region as r" +
                    " where r.id <> :currentId " +
                    " and (r.orderingId >= :start and r.orderingId <= :end) "
    )
    List<Region> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select r from Region as r" +
                    " where r.orderingId is not null" +
                    " order by r.name"
    )
    List<Region> orderByName();

    @Query(
            "select count(m.id) from Municipality as m" +
                    " where m.region.id = :regionId " +
                    " and (m.status in (:statuses))"
    )
    Long getMunicipalitiesCountByStatusAndRegionId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("regionId") Long regionId
    );

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(r.id, r.name)
        from Region r
        where r.name = :name
        and r.country.id =:countryId
        and r.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findByNameAndCountryId(@Param("name") String name,
                                                 @Param("countryId") Long countryId,
                                                 @Param("status") NomenclatureItemStatus status);

    @Query(
        """
        select count(1) from Region r
            where r.id = :id
            and (exists (select 1 from
                Municipality m,
                PopulatedPlace pp,
                CustomerCommunications cc,
                CustomerDetails cd,
                Customer c
                    where m.region.id = r.id
                    and pp.municipality.id = m.id
                    and cc.populatedPlaceId = pp.id
                    and cd.id = cc.customerDetailsId
                    and c.id = cd.customerId
                    and cc.status = 'ACTIVE' and c.status = 'ACTIVE'
            )
            or exists( select 1 from
                Municipality m,
                PopulatedPlace pp,
                CustomerDetails cd,
                Customer c
                    where m.region.id = r.id
                    and pp.municipality.id = m.id
                    and pp.id = cd.populatedPlaceId
                    and c.id = cd.customerId
                    and c.status = 'ACTIVE'
            ))
        """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query(
            """
            select count(r.id) from Region r
                left join r.country as rc
                where lower(r.name) = lower(:name)
                and rc.id = :parentId
                and r.status in (:statuses)
                and (:childId is null or r.id <> :childId)
            """
    )
    Long countRegionsByStatusCountryAndName(
            @Param("name") String name,
            @Param("parentId") Long parentId,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("childId") Long childId
    );

    Optional<Region> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> status);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> status);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            r.id,
                            r.name
                        )
                        from Region r
                        where r.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
