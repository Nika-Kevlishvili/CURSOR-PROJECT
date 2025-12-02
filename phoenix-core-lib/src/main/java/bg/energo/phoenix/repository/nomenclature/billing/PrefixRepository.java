package bg.energo.phoenix.repository.nomenclature.billing;

import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
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
public interface PrefixRepository extends JpaRepository<Prefix, Long> {
    @Query("""
            select p
            from Prefix p
            where p.status = 'ACTIVE'
            and p.prefixType = :invoicePrefixType
            """)
    Optional<Prefix> findByInvoicePrefixType(String invoicePrefixType);

    @Query(
            """ 
                    select c from Prefix as c
                        where (:prompt is null or (lower(c.name) like :prompt or lower(c.prefixType) like :prompt))
                        and (c.status in (:statuses))
                        and (:excludedItemId is null or c.id <> :excludedItemId)
                        order by c.isDefault desc, c.orderingId asc 
                     """
    )
    Page<Prefix> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(p.id, p.name, p.orderingId, p.isDefault, p.status)
                        from Prefix as p
                        where (:prompt is null or lower(p.name) like :prompt)
                        and (p.status in (:statuses))
                        order by p.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query(
            """
                    select count(c.id) from Prefix c
                        where lower(c.name) = lower(:name)
                        and c.status in (:statuses)
                    """
    )
    Long countPrefixByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select count(c.id) from Prefix c
                        where lower(c.prefixType) = lower(:prefixType)
                        and c.status in (:statuses)
                    """
    )
    Long countPrefixByStatusAndPrefixType(
            @Param("prefixType") String prefixType,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<Prefix> findByIsDefaultTrue();

    @Query("select max(c.orderingId) from Prefix c")
    Long findLastOrderingId();

    @Query(
            """
                    select c from Prefix as c
                        where c.id <> :currentId
                        and (c.orderingId >= :start and c.orderingId <= :end)
                    """

    )
    List<Prefix> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select c from Prefix as c
                        where c.orderingId is not null
                        order by c.name
                    """
    )
    List<Prefix> orderByName();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from Prefix c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    Optional<Prefix> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> status);

}
