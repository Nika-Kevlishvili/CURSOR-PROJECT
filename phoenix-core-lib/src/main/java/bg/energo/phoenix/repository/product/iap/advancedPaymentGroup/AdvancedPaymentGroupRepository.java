package bg.energo.phoenix.repository.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroup;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentGroupListResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponseInterface;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedGroupEntityResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedGroupEntityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdvancedPaymentGroupRepository extends JpaRepository<AdvancedPaymentGroup, Long> {
    Optional<AdvancedPaymentGroup> findByIdAndStatusIn(Long id, List<AdvancedPaymentGroupStatus> statuses);

    List<AdvancedPaymentGroup> findByIdInAndStatusIn(List<Long> ids, List<AdvancedPaymentGroupStatus> status);

    @Query(
            nativeQuery = true,
            value = """
                    select tbl.id                       as id,
                           tbl.name                     as name,
                           tbl.numberOfAdvancedPayments as numberOfAdvancedPayments,
                           tbl.dateOfCreation           as dateOfCreation,
                           tbl.status                   as status
                    from (select pg.id                              as id,
                                 pgd.name                           as name,
                                 pgd.start_date                     as startDate,
                                 (select count(1)
                                  from interim_advance_payment.iap_group_iaps pgp
                                  where pgp.interim_advance_payment_group_detail_id = pgd.id
                                    and pgp.status = 'ACTIVE')      as numberOfAdvancedPayments,
                                 (select max(start_date)
                                  from interim_advance_payment.interim_advance_payment_group_details tt
                                  where tt.interim_advance_payment_group_id = pg.id
                                    and start_date <= current_date) as currentstartdate,
                                 pg.create_date                     as dateOfCreation,
                                 pg.status                          as status
                          from interim_advance_payment.interim_advance_payment_group_details pgd
                                   join interim_advance_payment.interim_advance_payment_groups pg
                                        on pg.id = pgd.interim_advance_payment_group_id
                          where pg.id in (select pg.id
                                          from interim_advance_payment.interim_advance_payment_group_details pgd
                                                   left join interim_advance_payment.iap_group_iaps pgp
                                                             on pgp.interim_advance_payment_group_detail_id = pgd.id and
                                                                pgp.status = 'ACTIVE'
                                                   left join interim_advance_payment.interim_advance_payments p
                                                             on pgp.interim_advance_payment_id = p.id
                                                   join interim_advance_payment.interim_advance_payment_groups pg
                                                        on pg.id = pgd.interim_advance_payment_group_id
                                          where text(pg.status) in (:statuses)
                                            and (coalesce(:excludeVersion, '0') = '0' or
                                                 (:excludeVersion = 'OLDVERSION' and
                                                  pgd.start_date >=
                                                  (select max(start_date)
                                                   from interim_advance_payment.interim_advance_payment_group_details tt
                                                   where tt.interim_advance_payment_group_id = pg.id
                                                     and start_date <= current_date)
                                                     )
                                              or
                                                 (:excludeVersion = 'FUTUREVERSION' and
                                                  pgd.start_date <=
                                                  (select max(start_date)
                                                   from interim_advance_payment.interim_advance_payment_group_details tt
                                                   where tt.interim_advance_payment_group_id = pg.id
                                                     and start_date <= current_date)
                                                     )
                                              or
                                                 (:excludeVersion = 'OLDANDFUTUREVERSION' and
                                                  pgd.start_date =
                                                  (select max(start_date)
                                                   from interim_advance_payment.interim_advance_payment_group_details tt
                                                   where tt.interim_advance_payment_group_id = pg.id
                                                     and start_date <= current_date)
                                                     )
                                              )
                                            and (
                                                  :searchBy is null
                                                  or (:searchBy = 'ALL' and (lower(pgd.name) like :prompt or lower(p.name) like :prompt or
                                                                             (cast(pg.id as text) like :prompt)))
                                                  or (
                                                          (:searchBy = 'GROUP_OF_ADVANCED_PAYMENT_NAME' and lower(pgd.name) like :prompt)
                                                          or (:searchBy = 'ADVANCED_PAYMENT_NAME' and lower(p.name) like :prompt)
                                                      )
                                              ))) as tbl
                    where startDate = currentstartdate
                                        """,
            countQuery = """
                    select count(1)
                    from (select pg.id                              as id,
                                 pgd.name                           as name,
                                 pgd.start_date                     as startDate,
                                 (select max(start_date)
                                  from interim_advance_payment.interim_advance_payment_group_details tt
                                  where tt.interim_advance_payment_group_id = pg.id
                                    and start_date <= current_date) as currentstartdate
                          from interim_advance_payment.interim_advance_payment_group_details pgd
                                   join interim_advance_payment.interim_advance_payment_groups pg
                                        on pg.id = pgd.interim_advance_payment_group_id
                          where pg.id in (select pg.id
                                          from interim_advance_payment.interim_advance_payment_group_details pgd
                                                   left join interim_advance_payment.iap_group_iaps pgp
                                                             on pgp.interim_advance_payment_group_detail_id = pgd.id and
                                                                pgp.status = 'ACTIVE'
                                                   left join interim_advance_payment.interim_advance_payments p
                                                             on pgp.interim_advance_payment_id = p.id
                                                   join interim_advance_payment.interim_advance_payment_groups pg
                                                        on pg.id = pgd.interim_advance_payment_group_id
                                          where text(pg.status) in (:statuses)
                                            and (coalesce(:excludeVersion, '0') = '0' or
                                                 (:excludeVersion = 'OLDVERSION' and
                                                  pgd.start_date >=
                                                  (select max(start_date)
                                                   from interim_advance_payment.interim_advance_payment_group_details tt
                                                   where tt.interim_advance_payment_group_id = pg.id
                                                     and start_date <= current_date)
                                                     )
                                              or
                                                 (:excludeVersion = 'FUTUREVERSION' and
                                                  pgd.start_date <=
                                                  (select max(start_date)
                                                   from interim_advance_payment.interim_advance_payment_group_details tt
                                                   where tt.interim_advance_payment_group_id = pg.id
                                                     and start_date <= current_date)
                                                     )
                                              or
                                                 (:excludeVersion = 'OLDANDFUTUREVERSION' and
                                                  pgd.start_date =
                                                  (select max(start_date)
                                                   from interim_advance_payment.interim_advance_payment_group_details tt
                                                   where tt.interim_advance_payment_group_id = pg.id
                                                     and start_date <= current_date)
                                                     )
                                              )
                                            and (
                                                  :searchBy is null
                                                  or (:searchBy = 'ALL' and (lower(pgd.name) like :prompt or lower(p.name) like :prompt or
                                                                             (cast(pg.id as text) like :prompt)))
                                                  or (
                                                          (:searchBy = 'GROUP_OF_ADVANCED_PAYMENT_NAME' and lower(pgd.name) like :prompt)
                                                          or (:searchBy = 'ADVANCED_PAYMENT_NAME' and lower(p.name) like :prompt)
                                                      )
                                              ))) as tbl
                    where startDate = currentstartdate
                                        """
    )
    Page<AdvancedPaymentGroupListResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("statuses") List<String> statuses,
            @Param("excludeVersion") String excludeVersion,
            Pageable pageable
    );


    @Query(
            value = """
                    select count (pd.id) > 0 from ProductDetails pd
                    join ProductGroupOfInterimAndAdvancePayments pg on pg.productDetails.id = pd.id
                        where pg.interimAdvancePaymentGroup.id = :id
                        and pg.productSubObjectStatus = 'ACTIVE'
                        and pd.product.productStatus = 'ACTIVE'
                    """
    )
    boolean hasConnectionToProduct(@Param("id") Long id);


    @Query(
            value = """
                    select count (sd.id) > 0 from ServiceDetails sd
                    join ServiceInterimAndAdvancePaymentGroup sg on sg.serviceDetails.id = sd.id
                        where sg.advancedPaymentGroup.id = :id
                        and sg.status = 'ACTIVE'
                        and sd.service.status = 'ACTIVE'
                    """
    )
    boolean hasConnectionToService(@Param("id") Long id);


    @Query(
            value = """
                    select iap from InterimAdvancePayment iap
                        join AdvancedPaymentGroupDetails apgd on apgd.advancedPaymentGroupId = :groupId
                        join AdvancedPaymentGroupAdvancedPayments apgap on apgap.advancePaymentGroupDetailId = apgd.id
                            where apgap.status = 'ACTIVE'
                            and iap.id = apgap.advancePaymentId
                    """
    )
    List<InterimAdvancePayment> getConnectedActiveIAPsByGroupId(@Param("groupId") Long groupId);


    @Query(
            value = """
                    select
                        apgd1.interim_advance_payment_group_id as id,
                        apgd1.name || ' (' || cast(apgd1.interim_advance_payment_group_id as text) || ')' AS displayName
                    from interim_advance_payment.interim_advance_payment_group_details apgd1
                    where apgd1.interim_advance_payment_group_id in (
                        select apg.id
                        from interim_advance_payment.interim_advance_payment_groups apg
                        join interim_advance_payment.interim_advance_payment_group_details apgd on apgd.interim_advance_payment_group_id = apg.id
                            where (:prompt is null or (lower(apgd.name) like :prompt or cast(apg.id as text) like :prompt))
                            and apg.status ='ACTIVE'
                    )
                    and apgd1.start_date = (
                        select max(start_Date) from interim_advance_payment.interim_advance_payment_group_details iapgd
                        where iapgd.interim_advance_payment_group_id = apgd1.interim_advance_payment_group_id
                        and iapgd.start_date <= now()
                    )
                    order by apgd1.id DESC
                    """,
            nativeQuery = true
    )
    Page<CopyDomainWithVersionBaseResponseInterface> findByCopyDomainWithVersionBaseRequest(
            String prompt,
            PageRequest pageRequest
    );


    @Query(nativeQuery = true,
            value = """
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from interim_advance_payment.interim_advance_payment_groups tg
                    join interim_advance_payment.interim_advance_payment_group_details tgd on tgd.interim_advance_payment_group_id = tg.id
                        where tg.id in (
                            select tg1.id from interim_advance_payment.interim_advance_payment_groups tg1
                            join interim_advance_payment.interim_advance_payment_group_details tgd1 on tgd1.interim_advance_payment_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from interim_advance_payment.interim_advance_payment_group_details tgd3
                                where tgd3.interim_advance_payment_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
                    """,
            countQuery = """
                    select count(1) from interim_advance_payment.interim_advance_payment_groups tg
                    join interim_advance_payment.interim_advance_payment_group_details tgd on tgd.interim_advance_payment_group_id = tg.id
                        where tg.id in (
                            select tg1.id from interim_advance_payment.interim_advance_payment_groups tg1
                            join interim_advance_payment.interim_advance_payment_group_details tgd1 on tgd1.interim_advance_payment_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from interim_advance_payment.interim_advance_payment_group_details tgd3
                                where tgd3.interim_advance_payment_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableProductRelatedGroupEntityResponse> findAvailableAdvancePaymentGroupsForProduct(
            @Param("prompt") String prompt,
            PageRequest pageRequest
    );


    @Query(nativeQuery = true,
            value = """
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from interim_advance_payment.interim_advance_payment_groups tg
                    join interim_advance_payment.interim_advance_payment_group_details tgd on tgd.interim_advance_payment_group_id = tg.id
                        where tg.id in (
                            select tg1.id from interim_advance_payment.interim_advance_payment_groups tg1
                            join interim_advance_payment.interim_advance_payment_group_details tgd1 on tgd1.interim_advance_payment_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from interim_advance_payment.interim_advance_payment_group_details tgd3
                                where tgd3.interim_advance_payment_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
                    """,
            countQuery = """
                    select count(1) from interim_advance_payment.interim_advance_payment_groups tg
                    join interim_advance_payment.interim_advance_payment_group_details tgd on tgd.interim_advance_payment_group_id = tg.id
                        where tg.id in (
                            select tg1.id from interim_advance_payment.interim_advance_payment_groups tg1
                            join interim_advance_payment.interim_advance_payment_group_details tgd1 on tgd1.interim_advance_payment_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from interim_advance_payment.interim_advance_payment_group_details tgd3
                                where tgd3.interim_advance_payment_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableServiceRelatedGroupEntityResponse> findAvailableInterimAdvancePaymentsGroupsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    select coalesce(max('true'),'false') as is_connected from interim_advance_payment.interim_advance_payment_groups iapg 
                    where iapg.id = :iapgroupid and
                     (exists(select 1 
                    	from product.products p
                    	join product.product_details pd
                     			  on pd.product_id = p.id
                    	 and p.status = 'ACTIVE'
                    	join product.product_interim_advance_payment_groups piapg
                    	  on piapg.product_detail_id = pd.id
                    	 and piapg.interim_advance_payment_group_id = iapg.id
                    	 and piapg.status = 'ACTIVE'
                    	join interim_advance_payment.interim_advance_payment_group_details iapgd
                    	  on iapgd.interim_advance_payment_group_id = iapg.id
                    	join interim_advance_payment.iap_group_iaps igi 
                    	  on igi.interim_advance_payment_group_detail_id = iapgd.id
                    	 and igi.status = 'ACTIVE'
                    	join product_contract.contract_details cd 
                    	  on cd.product_detail_id =  pd.id
                    	join product_contract.contracts c
                    	  on cd.contract_id =  c.id
                    	 and c.status = 'ACTIVE')
                    or
                     exists(select 1 
                    	from service.services s
                    	join service.service_details sd
                     			  on sd.service_id = s.id
                    	 and s.status = 'ACTIVE'
                    	join service.service_interim_advance_payment_groups siapg
                    	  on siapg.service_detail_id = sd.id
                    	 and siapg.interim_advance_payment_group_id = iapg.id
                    	 and siapg.status = 'ACTIVE'
                    	join interim_advance_payment.interim_advance_payment_group_details iapgd
                    	  on iapgd.interim_advance_payment_group_id = iapg.id
                    	join interim_advance_payment.iap_group_iaps igi 
                    	  on igi.interim_advance_payment_group_detail_id = iapgd.id
                    	 and igi.status = 'ACTIVE'
                    	join service_contract.contract_details cd 
                    	  on cd.service_detail_id =  sd.id
                    	join service_contract.contracts c
                    	  on cd.contract_id =  c.id
                    	 and c.status = 'ACTIVE')
                    or
                     exists(select 1 
                    	from service.services s
                    	join service.service_details sd
                     			  on sd.service_id = s.id
                    	 and s.status = 'ACTIVE'
                    	join service.service_interim_advance_payment_groups siapg
                    	  on siapg.service_detail_id = sd.id
                    	 and siapg.interim_advance_payment_group_id = iapg.id
                    	 and siapg.status = 'ACTIVE'
                    	join interim_advance_payment.interim_advance_payment_group_details iapgd
                    	  on iapgd.interim_advance_payment_group_id = iapg.id
                    	join interim_advance_payment.iap_group_iaps igi 
                    	  on igi.interim_advance_payment_group_detail_id = iapgd.id
                    	 and igi.status = 'ACTIVE'
                              join service_order.orders o 
                                on o.service_detail_id =  sd.id
                               and o.status = 'ACTIVE')
                     )
                    """
    )
    boolean hasLockedConnection(
            @Param("iapgroupid") Long id
    );

    @Query("""
            select adgd
            from AdvancedPaymentGroupDetails adgd
            where adgd.advancedPaymentGroupId = :advancePaymentGroupId
            """)
    List<AdvancedPaymentGroupDetails> findAdvancedPaymentGroupDetailsByAdvancePaymentGroup(Long advancePaymentGroupId);
}
