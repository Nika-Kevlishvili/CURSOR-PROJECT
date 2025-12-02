package bg.energo.phoenix.repository.nomenclature.customer.legalForm;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.LegalFormTranResponse;
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
    @Query(value = """
            select new bg.energo.phoenix.model.response.nomenclature.customer.LegalFormTranResponse(lt.id,lt.name,lt.description) from LegalFormTransliterated lt
            where lt.id=:Id
            """)
    LegalFormTranResponse getLegalFormByLegalFormTransliteratedId(@Param("Id")Long Id);
    @Query(
            """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                        p.id,
                        CONCAT(p.description, ' ', p.name),
                        p.orderingId,
                        p.defaultSelection,
                        p.status
                    )
                    from LegalForm as p
                    where (:prompt is null or (
                        lower(p.name) like :prompt or
                        lower(p.description) like :prompt
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
                lower(p.name) like :prompt or
                lower(p.description) like :prompt
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
            select new bg.energo.phoenix.model.CacheObject(p.id, p.name)
            from LegalForm p
            where p.name =:name
            and p.status =:status
        """
    )
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findByNameAndStatus(@Param("name")String name, @Param("status") NomenclatureItemStatus active);

    @Query("select lf from LegalForm lf " +
            " where " +
            " lower(lf.description) in (:searchStrings) and lf.status in (:statuses) " +
            " order by lf.id DESC ")
    Optional<List<LegalForm>> searchInDescriptions(List<String> searchStrings,List<NomenclatureItemStatus> statuses);

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

    @Query(
            """
            select count(l.id) from LegalForm l
                where lower(l.name) = lower(:name)
                and lower(l.description)  = lower(:description) 
                and l.status in (:statuses)
                and (:id is null or l.id <> :id)
            """
    )
    Long countLegalFormByStatusDescriptionAndName(
            @Param("name") String name,
            @Param("description") String description,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("id") Long id
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            l.id,
                            l.name
                        )
                        from LegalForm l
                        where l.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
