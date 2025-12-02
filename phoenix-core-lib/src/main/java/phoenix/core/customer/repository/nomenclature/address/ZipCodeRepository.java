package phoenix.core.customer.repository.nomenclature.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.address.ZipCode;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;
import phoenix.core.customer.model.response.nomenclature.ZipCodeTreeResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZipCodeRepository extends JpaRepository<ZipCode, Long>, QueryByExampleExecutor<ZipCode> {

    @Query("""
        select z
        from ZipCode z
        where z.id = :id
        and z.populatedPlace.id = :populatedPlaceId
        and z.status in :statuses
    """)
    Optional<ZipCode> findByIdAndPopulatedPlaceIdAndStatus(
            @Param("id") Long id,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );


    @Query("select z from ZipCode as z" +
            " left join z.populatedPlace as zp " +
            " left join zp.municipality as zpm " +
            " left join zpm.region as zpmr " +
            " left join zpmr.country as zpmrc " +
            " where (z.status in (:statuses))" +
            " and (:prompt is null or (" +
                "lower(z.name) like lower(concat('%',:prompt,'%')) or " +
                "lower(zp.name) like lower(concat('%',:prompt,'%')) or " +
                "lower(zpm.name) like lower(concat('%',:prompt,'%')) or " +
                "lower(zpmr.name) like lower(concat('%',:prompt,'%')) or " +
                "lower(zpmrc.name) like lower(concat('%',:prompt,'%'))))" +
            " and (:populatedPlaceId is null or zp.id = :populatedPlaceId)" +
            " and (:excludedItemId is null or z.id <> :excludedItemId) " +
            " order by z.orderingId asc "
    )
    Page<ZipCode> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                        "z.id," +
                        " CONCAT(z.name, ' - ', zp.name, ' - ', zpm.name, ' - ', zpmr.name, ' - ', zpmrc.name), " +
                        "z.orderingId, " +
                        "z.defaultSelection, " +
                        "z.status" +
                    ") " +
                    "from ZipCode as z" +
                    " left join z.populatedPlace as zp " +
                    " left join zp.municipality as zpm " +
                    " left join zpm.region as zpmr " +
                    " left join zpmr.country as zpmrc " +
                    " where (z.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        "lower(z.name) like lower(concat('%',:prompt,'%')) or " +
                        "lower(zp.name) like lower(concat('%',:prompt,'%')) or " +
                        "lower(zpm.name) like lower(concat('%',:prompt,'%')) or " +
                        "lower(zpmr.name) like lower(concat('%',:prompt,'%')) or " +
                        "lower(zpmrc.name) like lower(concat('%',:prompt,'%'))))" +
                    " and (:excludedItemId is null or z.id <> :excludedItemId) " +
                    " order by z.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<ZipCode> findByDefaultSelectionTrue();

    @Query(value = "select max(z.orderingId) from ZipCode z")
    Long findLastOrderingId();

    @Query(
            "select z from ZipCode as z" +
                    " where z.id <> :currentId " +
                    " and (z.orderingId >= :start and z.orderingId <= :end) "
    )
    List<ZipCode> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select z from ZipCode as z" +
                    " where z.orderingId is not null" +
                    " order by z.name"
    )
    List<ZipCode> orderByName();

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.ZipCodeTreeResponse(" +
                        "zp.id, " +
                        "zp.name, " +
                        "zpm.id, " +
                        "zpm.name, " +
                        "zpmr.id, " +
                        "zpmr.name, " +
                        "zpmrc.id, " +
                        "zpmrc.name" +
                    ") " +
                    "from ZipCode as z" +
                        " left join z.populatedPlace as zp " +
                        " left join zp.municipality as zpm " +
                        " left join zpm.region as zpmr " +
                        " left join zpmr.country as zpmrc " +
                    " where z.id = :zipCodeId"
    )
    ZipCodeTreeResponse getZipCodeTreeView(@Param("zipCodeId") Long zipCodeId);

    @Query(
            """
            select count(1) from ZipCode z
                where z.id = :id
                and (exists (select 1 from CustomerDetails cd, Customer c
                        where cd.zipCode.id = :id
                        and c.id = cd.customerId
                        and c.status = 'ACTIVE')
                or exists (select 1 from CustomerCommunications cc, CustomerDetails cd, Customer c
                        where cc.zipCodeId = :id
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE')
                )
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
