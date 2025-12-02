package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForBank;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
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
import org.springframework.stereotype.Repository;

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
                        " lower(b.name) like :prompt or " +
                        " lower(b.bic) like :prompt" +
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
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "b.id," +
                    " CONCAT(b.name, ' - ', b.bic), " +
                    " b.orderingId," +
                    " b.defaultSelection," +
                    " b.status" +
                    ") " +
                    "from Bank as b" +
                    " where (:prompt is null or (" +
                        " lower(b.name) like :prompt or " +
                        " lower(b.bic) like :prompt" +
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
            """
                    select count (1) from Bank b
                        where b.id = :id 
                        and (exists (select 1 from CustomerDetails cd, Customer c
                           where cd.bank.id = :id 
                           and cd.customerId = c.id 
                           and c.status = 'ACTIVE')
                        or exists (
                            select 1 from CompanyBank cb
                            where cb.bankId = :id
                            and cb.status = 'ACTIVE')
                        or exists (
                            select 1 from BillingRun br
                            where br.bankId = :id
                            and cast(br.status as string)  not in ('DELETED','CANCELLED') 
                        )
                        or exists (
                            select 1 from CollectionChannel cc 
                            join CollectionChannelBanks ccb on ccb.collectionChannelId=cc.id
                            where ccb.bankId=:id
                            and cast(ccb.status as string ) ='ACTIVE'
                            and cast(cc.status as string ) ='ACTIVE'
                        )
                        or exists (
                            select 1 from CustomerLiability cl 
                            where cl.bankId=:id
                            and cast(cl.status as string ) ='ACTIVE'
                        )
                        or exists (
                            select 1 from CustomerReceivable cr 
                            where cr.bankId=:id
                            and cast(cr.status as string ) ='ACTIVE'
                        )
                        or exists (
                            select 1 from CompanyBank cb 
                            where cb.bankId=:id
                            and cast(cb.status as string ) ='ACTIVE'
                        )
                        )"""
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query("""
        select new bg.energo.phoenix.model.CacheObjectForBank(b.id, b.name, b.bic)
        from Bank b
        where b.name = :name
        and b.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObjectForBank> getByNameAndStatus(@Param("name") String name,
                                                    @Param("status") NomenclatureItemStatus active);

    @Query(
            """
            select count(b.id) from Bank b
                where lower(b.name) = lower(:name)
                and b.status in (:statuses)
            """
    )
    Long countBankByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );


    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            b.id,
                            b.name
                        )
                        from Bank b
                        where b.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    Optional<Bank> findByNameAndStatusIn(String name,List<NomenclatureItemStatus> statuses);

    @Query("""
           select new bg.energo.phoenix.model.CacheObject(b.id,b.name)
           from Bank b
           where b.name=:bankName
           and b.status in :status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(String bankName, List<NomenclatureItemStatus> status);

    boolean existsByBicAndStatusIn(String bic, List<NomenclatureItemStatus> statuses);
}
