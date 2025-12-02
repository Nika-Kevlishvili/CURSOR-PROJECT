package bg.energo.phoenix.repository.nomenclature.address;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.address.Municipality;
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

import java.util.List;
import java.util.Optional;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {
    @Query("select m from Municipality as m" +
            " left join m.region as mr " +
            " left join mr.country as mrc " +
            " where (m.status in (:statuses))" +
            " and (:prompt is null or (" +
                " lower(m.name) like :prompt)) " +
//                " lower(m.name) like :prompt or " +
//                " lower(mr.name) like :prompt or " +
//                " lower(mrc.name) like :prompt))" +
            " and (:regionId is null or mr.id = :regionId)" +
            " and (:excludedItemId is null or m.id <> :excludedItemId) " +
            " order by m.defaultSelection desc, m.orderingId asc "
    )
    Page<Municipality> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("regionId") Long regionId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                        "m.id, " +
                        "CONCAT(m.name, ' - ', mr.name, ' - ', mrc.name), " +
                        "m.orderingId, " +
                        "m.defaultSelection, " +
                        "m.status" +
                    ") " +
                    "from Municipality as m" +
                    " left join m.region as mr " +
                    " left join mr.country as mrc " +
                    " where (m.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        " lower(m.name) like :prompt)) " +
//                        " lower(m.name) like :prompt or " +
//                        " lower(mr.name) like :prompt or " +
//                        " lower(mrc.name) like :prompt)) " +
                    " and (:excludedItemId is null or m.id <> :excludedItemId) " +
                    " order by m.defaultSelection desc, m.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<Municipality> findByDefaultSelectionTrue();

    @Query(value = "select max(m.orderingId) from Municipality m")
    Long findLastOrderingId();

    @Query(
            "select m from Municipality as m" +
                    " where m.id <> :currentId " +
                    " and (m.orderingId >= :start and m.orderingId <= :end) "
    )
    List<Municipality> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select m from Municipality as m" +
                    " where m.orderingId is not null" +
                    " order by m.name"
    )
    List<Municipality> orderByName();

    @Query(
            "select count(p.id) from PopulatedPlace as p" +
                    " where p.municipality.id = :municipalityId " +
                    " and (p.status in (:statuses))"
    )
    Long getPopulatedPlacesCountByStatusAndMunicipalityId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("municipalityId") Long municipalityId
    );

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(m.id, m.name)
        from Municipality m
        where m.name = :name
        and m.region.id =:regionId
        and m.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getByNameAndRegionId(@Param("name") String name,
                                               @Param("regionId") Long regionId,
                                               @Param("status") NomenclatureItemStatus status);

    @Query(
            """
            select count(1) from Municipality m
                where m.id = :id
                and (exists (select 1 from
                    PopulatedPlace pp,
                    CustomerCommunications cc,
                    CustomerDetails cd,
                    Customer c
                        where pp.municipality.id = m.id
                        and cc.populatedPlaceId = pp.id
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE'
                )
                or exists( select 1 from
                    PopulatedPlace pp,
                    CustomerDetails cd,
                    Customer c
                        where pp.municipality.id = m.id
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
            select count(m.id) from Municipality m
                left join m.region as rc
                where lower(m.name) = lower(:name)
                and rc.id = :parentId
                and m.status in (:statuses)
                and (:childId is null or m.id <> :childId)
            """
    )
    Long countMunicipalityByStatusRegionAndName(
            @Param("name") String name,
            @Param("parentId") Long parentId,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("childId") Long childId
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            m.id,
                            m.name
                        )
                        from Municipality m
                        where m.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
