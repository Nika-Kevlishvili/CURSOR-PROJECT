package bg.energo.phoenix.repository.nomenclature.address;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.address.Country;
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

public interface CountryRepository extends JpaRepository<Country, Long> {

    @Query("""
        select c
        from Country c
        where c.id = :id
        and c.status in :statuses
    """)
    Optional<Country> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query(
            "select c from Country as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<Country> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                        "c.id, " +
                        "c.name, " +
                        "c.orderingId, " +
                        "c.defaultSelection, " +
                        "c.status" +
                    ") " +
                    "from Country as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Country> findByDefaultSelectionTrue();

    @Query("select max(c.orderingId) from Country c")
    Long findLastOrderingId();

    @Query(
            "select c from Country as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<Country> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from Country as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<Country> orderByName();

    @Query(
            "select count(r.id) from Region as r" +
                    " where r.country.id = :countryId " +
                    " and (r.status in (:statuses))"
    )
    Long getRegionsCountByStatusAndCountryId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("countryId") Long countryId
    );

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(c.id, c.name)
        from Country c
        where c.name = :name
        and c.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus( @Param("name")String name, @Param("status") NomenclatureItemStatus status);



    @Query(
            """
            select count(1) from Country country
                where country.id = :id
                and (exists (select 1 from CustomerDetails cd, Customer c
                        where cd.countryId = :id
                        and c.id = cd.customerId
                        and c.status = 'ACTIVE')
                or exists (select 1 from CustomerCommunications cc, CustomerDetails cd, Customer c
                        where cc.countryId = :id 
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE')
                        
                or exists
                ( select 1 from PointOfDelivery p
                  join PointOfDeliveryDetails pd on pd.podId = p.id
                    and pd.countryId = country.id
                  where
                    p.status = 'ACTIVE')
                )""")
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query(
            """
            select count(c.id) from Country c
                where lower(c.name) = lower(:name)
                and c.status in (:statuses)
            """
    )
    Long countCountryByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id,List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from Country c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
