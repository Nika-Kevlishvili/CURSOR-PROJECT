package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.TaxesForTheGridOperator;
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
public interface TaxForTheGriOperatorRepository extends JpaRepository<TaxesForTheGridOperator, Long> {

    @Query("""
           select tax from TaxesForTheGridOperator tax
           where lower( tax.disconnectionType) like lower(:name)
           and tax.status in(:statuses)
           """)
    List<TaxesForTheGridOperator> findByNameAndStatuses(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("select max(tax.orderingId) from TaxesForTheGridOperator tax")
    Long findLastOrderingId();

    @Query("""
            select tax from TaxesForTheGridOperator as tax
                where tax.orderingId is not null
                order by tax.disconnectionType
            """
    )
    List<TaxesForTheGridOperator> orderByName();

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                           tax.id,
                            tax.disconnectionType
                        )
                        from TaxesForTheGridOperator tax
                        where tax.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            "select tax from TaxesForTheGridOperator as tax" +
                    " where tax.id <> :currentId " +
                    " and (tax.orderingId >= :start and tax.orderingId <= :end) "
    )
    List<TaxesForTheGridOperator> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(tax.id, tax.disconnectionType, tax.orderingId, tax.defaultSelection, tax.status) " +
                    "from TaxesForTheGridOperator as tax" +
                    " where (:prompt is null or lower(tax.disconnectionType) like :prompt)" +
                    " and (:excludedItemId is null or tax.id <> :excludedItemId) " +
                    " and (tax.status in (:statuses))" +
                    " order by tax.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query("""
            select got
            from TaxesForTheGridOperator got
            join GridOperator go2 on got.gridOperator = go2.id
            where ( (:prompt is null or lower(got.disconnectionType) like :prompt)
            or (lower(go2.name) like :prompt) or (lower(text(got.supplierType)) like :prompt) )
            and ((got.status in :statuses)
            and (:excludedItemId is null or got.id <> :excludedItemId)
            or (coalesce(:includedItemId, null) is null or got.id in (:includedItemId)) )
            order by got.defaultSelection desc, got.orderingId asc
            """)
    Page<TaxesForTheGridOperator> filter(String prompt, List<NomenclatureItemStatus> statuses, Long excludedItemId, List<Long> includedItemId, Pageable pageable);

    Optional<TaxesForTheGridOperator> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

}
