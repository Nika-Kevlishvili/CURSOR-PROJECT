package phoenix.core.customer.repository.nomenclature.customer.legalForm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.customer.legalForm.LegalForm;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalFormRepository extends JpaRepository<LegalForm, Long> {

    @Query(value = "select max(p.orderingId) from LegalForm p")
    Long findTopId();


    Optional<LegalForm> findByDefaultSelectionTrue();

    @Query("select p from LegalForm p where p.id=:id and p.status in :statuses")
    Optional<LegalForm> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(value = """
            select p from LegalForm p
            where p.orderingId is not null
            order by p.name
            """)
    List<LegalForm> orderByName();

    @Query(
            """
                    select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(
                        p.id,
                        CONCAT(p.description, ' ', p.name),
                        p.orderingId,
                        p.defaultSelection,
                        p.status
                    )
                    from LegalForm as p
                    where (:prompt is null or (
                        lower(p.name) like lower(concat('%',:prompt,'%')) or
                        lower(p.description) like lower(concat('%',:prompt,'%'))
                    ))
                    and (p.status in (:statuses))
                    order by p.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query("""
            select p from LegalForm p
            where p.id <> :currentId
            and (p.orderingId >= :start and p.orderingId <= :end)
            """)
    List<LegalForm> findInOrderingIdRange(@Param("start") Long start, @Param("end") Long end, @Param("currentId") Long currentId, Sort sort);

    @Query("""
            select p from LegalForm p
            where (:prompt is null or (
                lower(p.name) like lower(concat('%',:prompt,'%')) or
                lower(p.description) like lower(concat('%',:prompt,'%'))
            ))
            and (p.status in (:statuses))
            and (:excludedItemId is null or p.id <> :excludedItemId)
            order by p.defaultSelection desc, p.orderingId asc
            """)
    Page<LegalForm> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
            select count(1) from LegalForm lf
                where lf.id = :id
                and exists (select 1 from CustomerDetails cd, Customer c
                    where cd.legalFormId = :id
                    and cd.customerId = c.id
                    and c.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
