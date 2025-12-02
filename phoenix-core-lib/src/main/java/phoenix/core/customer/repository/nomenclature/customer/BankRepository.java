package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.customer.Bank;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {

    @Query("""
        select b
        from Bank b
        where b.id = :id
        and b.status in :statuses
    """)
    Optional<Bank> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query(
            "select b from Bank as b" +
                    " where (:prompt is null or (" +
                        " lower(b.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(b.bic) like lower(concat('%',:prompt,'%'))" +
                    "))" +
                    " and (b.status in (:statuses))" +
                    " and (:excludedItemId is null or b.id <> :excludedItemId) " +
                    " order by b.defaultSelection desc, b.orderingId asc"
    )
    Page<Bank> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                    "b.id," +
                    " CONCAT(b.name, ' - ', b.bic), " +
                    " b.orderingId," +
                    " b.defaultSelection," +
                    " b.status" +
                    ") " +
                    "from Bank as b" +
                    " where (:prompt is null or (" +
                        " lower(b.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(b.bic) like lower(concat('%',:prompt,'%'))" +
                    "))" +
                    " and (b.status in (:statuses))" +
                    " order by b.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Bank> findByDefaultSelectionTrue();

    @Query("select max(b.orderingId) from Bank b")
    Long findLastOrderingId();

    @Query(
            "select b from Bank as b" +
                    " where b.id <> :currentId " +
                    " and (b.orderingId >= :start and b.orderingId <= :end) "
    )
    List<Bank> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select b from Bank as b" +
                    " where b.orderingId is not null" +
                    " order by b.name"
    )
    List<Bank> orderByName();

    @Query(
            "select count (1) from Bank b" +
                    " where b.id = :id " +
                    " and exists (select 1 from CustomerDetails cd, Customer c " +
                    " where cd.bank.id = :id " +
                    " and cd.customerId = c.id " +
                    " and c.status = 'ACTIVE')"
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
