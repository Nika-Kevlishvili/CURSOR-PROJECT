package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.customer.AccountManagerType;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountManagerTypeRepository extends JpaRepository<AccountManagerType, Long> {
    @Query(
            "select a from AccountManagerType as a" +
                    " where (:prompt is null or lower(a.name) like lower(concat('%',:prompt,'%')))" +
                    " and (a.status in (:statuses))" +
                    " and (:excludedItemId is null or a.id <> :excludedItemId) " +
                    " order by a.defaultSelection desc, a.orderingId asc"
    )
    Page<AccountManagerType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                    "a.id, " +
                    "a.name, " +
                    "a.orderingId, " +
                    "a.defaultSelection, " +
                    "a.status" +
                    ") " +
                    "from AccountManagerType as a" +
                    " where (:prompt is null or lower(a.name) like lower(concat('%',:prompt,'%')))" +
                    " and (a.status in (:statuses))" +
                    " order by a.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<AccountManagerType> findByDefaultSelectionTrue();

    @Query("select max(a.orderingId) from AccountManagerType a")
    Long findLastOrderingId();

    @Query(
            "select a from AccountManagerType as a" +
                    " where a.id <> :currentId " +
                    " and (a.orderingId >= :start and a.orderingId <= :end) "
    )
    List<AccountManagerType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select a from AccountManagerType as a" +
                    " where a.orderingId is not null" +
                    " order by a.name"
    )
    List<AccountManagerType> orderByName();

    @Query(
            "select count(1) from AccountManagerType amt " +
                    " where amt.id = :id " +
                    " and exists (select 1 from " +
                    "CustomerAccountManager cam, " +
                    "CustomerDetails cd, " +
                    "Customer c" +
                    " where cam.accountManagerType.id = :id " +
                    " and cam.customerDetail.id = cd.id " +
                    " and cd.customerId = c.id" +
                    " and cam.status = 'ACTIVE' and c.status = 'ACTIVE')"
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
