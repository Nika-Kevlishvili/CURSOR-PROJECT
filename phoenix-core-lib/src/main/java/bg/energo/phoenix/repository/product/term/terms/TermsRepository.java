package bg.energo.phoenix.repository.product.term.terms;

import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponseProjection;
import bg.energo.phoenix.model.response.terms.AvailableTermsResponse;
import bg.energo.phoenix.model.response.terms.TermsListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TermsRepository extends JpaRepository<Terms, Long> {
    @Query("""
            select t
            from TermsGroups tg
            join TermGroupDetails tgd on tgd.groupId = tg.id
            join TermsGroupTerms tgt on tgt.termGroupDetailId = tgd.id
            join Terms t on t.id = tgt.termId
            where tg.id = :termsGroupId
            and tgt.termGroupStatus = 'ACTIVE'
            and tgd.startDate <= :currentDate
            order by tgd.startDate desc
            """)
    Optional<Terms> findRespectiveTermsByTermsGroupId(Long termsGroupId,
                                                      LocalDateTime currentDate,
                                                      PageRequest pageRequest);

    Optional<Terms> findByIdAndStatusIn(Long id, List<TermStatus> statuses);

    /**
     * Filters terms according to the search criteria and returns a page of terms.
     * Availability condition for the term is calculated as follows:
     * <ul>
     *     <li>Term should not be attached to a group</li>
     *     <li>Term should not be attached to an active service</li>
     *     <li>Term should not be attached to an active product</li>
     * </ul>
     *
     * @param prompt      search criteria for term name or payment term name
     * @param searchField field where the search criteria should be applied
     * @param statuses    list of statuses to filter terms by
     * @param pageable    page request
     * @return page of terms
     */
    @Query(
            nativeQuery = true,
            value = """
                    select
                            tgo.id as id,
                            tgo.name as name,
                            tgo.noInterestOnOverdueDebts as noInterestOnOverdueDebts,
                            coalesce(tgo.group_product_service_name, 'Available') as available,
                            tgo.status as status,
                            tgo.dateOfCreation as dateOfCreation from (
                                select
                                    t.id as id,
                                    t.name as name,
                                    t.no_interest_on_overdue_debts as noInterestOnOverdueDebts,
                                   (select tgd.name
                                      from terms.term_group_details tgd
                                      join terms.term_groups tg on tgd.group_id = tg.id
                                      join terms.term_group_terms tgt on tgt.term_group_detail_id = tgd.id
                                       and tgt.term_id = t.id
                                       and tgt.status = 'ACTIVE'
                                       and tg.status = 'ACTIVE'
                                    union
                                     select pd.name
                                      from
                                       product.product_details pd
                                       join product.products p on pd.product_id = p.id
                                           where pd.term_id = t.id
                                             and pd.status = 'ACTIVE'    
                                             and p.status = 'ACTIVE'      
                                     union
                                       select sd.name
                                     from
                                      service.service_details sd
                                      join service.services s on sd.service_id = s.id
                                          where sd.term_id = t.id
                                            and sd.status = 'ACTIVE'
                                            and s.status = 'ACTIVE'
                                        limit 1) as group_product_service_name,       
                                    t.status as status,
                                    t.create_date as dateOfCreation
                                from terms.terms t
                                    where text(t.status) in (:statuses)
                                    and  (:columnName is null
                                        or (:columnName = 'ALL'
                                            and (lower(t.name) like :columnValue
                                            or exists(select 1 from terms.invoice_payment_terms ipt
                                                where lower(ipt.name) like :columnValue and ipt.term_id = t.id)))
                                        or (:columnName = 'NAME' and lower(t.name) like :columnValue)
                                        or (:columnName = 'PAYMENT_TERM_NAME' and exists(select 1 from terms.invoice_payment_terms ipt
                                            where lower(ipt.name) like :columnValue and ipt.term_id = t.id))
                                    )
                            ) as tgo
                            where
                              (coalesce(:available, '0') = '0' or :available = 'YES' and tgo.group_product_service_name is null or
                                   :available = 'NO' and tgo.group_product_service_name is not null) 
                    """,
            countQuery = """
                    select count(tgo.id) from (
                            select
                                    t.id as id,
                                    t.name as name,
                                    t.no_interest_on_overdue_debts as noInterestOnOverdueDebts,
                                   (select tgd.name
                                      from terms.term_group_details tgd
                                      join terms.term_groups tg on tgd.group_id = tg.id
                                      join terms.term_group_terms tgt on tgt.term_group_detail_id = tgd.id
                                       and tgt.term_id = t.id
                                       and tgt.status = 'ACTIVE'
                                       and tg.status = 'ACTIVE'
                                    union
                                     select pd.name
                                      from
                                       product.product_details pd
                                       join product.products p on pd.product_id = p.id
                                           where pd.term_id = t.id
                                             and pd.status = 'ACTIVE'
                                             and p.status = 'ACTIVE'          
                                     union
                                       select sd.name
                                     from
                                      service.service_details sd
                                      join service.services s on sd.service_id = s.id
                                          where sd.term_id = t.id
                                            and sd.status = 'ACTIVE'
                                            and s.status = 'ACTIVE'
                                        limit 1) as group_product_service_name,       
                                    t.status as status,
                                    t.create_date as dateOfCreation
                                from terms.terms t
                                    where text(t.status) in (:statuses)
                                    and  (:columnName is null
                                        or (:columnName = 'ALL'
                                            and (lower(t.name) like :columnValue
                                            or exists(select 1 from terms.invoice_payment_terms ipt
                                                where lower(ipt.name) like :columnValue and ipt.term_id = t.id)))
                                        or (:columnName = 'NAME' and lower(t.name) like :columnValue)
                                        or (:columnName = 'PAYMENT_TERM_NAME' and exists(select 1 from terms.invoice_payment_terms ipt
                                            where lower(ipt.name) like :columnValue and ipt.term_id = t.id))
                                    )
                            ) as tgo
                            where
                              (coalesce(:available, '0') = '0' or :available = 'YES' and tgo.group_product_service_name is null or
                                   :available = 'NO' and tgo.group_product_service_name is not null) 
                    """
    )
    Page<TermsListResponse> filter(
            @Param("columnValue") String prompt,
            @Param("columnName") String searchField,
            @Param("statuses") List<String> statuses,
            @Param("available") String available,
            Pageable pageable
    );


    @Query(value =
            """
                    select new bg.energo.phoenix.model.response.terms.AvailableTermsResponse(
                        t.id,
                        t.name,
                        t.createDate
                    )
                    from Terms t
                        where t.status = 'ACTIVE'
                        and (:prompt is null or (lower(t.name) like :prompt or cast (t.id as string) like :prompt))
                        and t.groupDetailId is null
                        and not exists (select 1 from ServiceDetails sd
                            join EPService s on sd.service.id = s.id
                            and sd.terms.id = t.id and s.status = 'ACTIVE')
                        and not exists (select 1 from ProductDetails pd
                            join Product p on pd.product.id = p.id
                            and pd.terms.id = t.id and p.productStatus = 'ACTIVE')
                        and (select count(ipt) from InvoicePaymentTerms ipt
                            where ipt.termId = t.id
                            and ipt.status = 'ACTIVE') = 1
                        and (select count(ipt) from InvoicePaymentTerms ipt
                            where ipt.termId = t.id
                                and ipt.status = 'ACTIVE'
                                and ipt.value is not null
                                and ipt.valueFrom is null
                                and ipt.valueTo is null) = 1
                    """
    )
    Page<AvailableTermsResponse> findAvailableTerms(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query("""
                select new bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse(t.id, concat(t.name, ' (', t.id, ')'))
                from Terms t
                where (:prompt is null
                or lower(t.name) like :prompt
                or cast(t.id as string)  = :prompt)
                and t.status in (:statuses)
                order by t.id DESC
            """)
    Page<CopyDomainListResponse> filterForCopy(
            @Param("prompt") String prompt,
            @Param("statuses") List<TermStatus> statuses,
            Pageable pageable
    );


    @Query(value = """
            select t from Terms as t
            join TermGroupDetails tgd on tgd.groupId = :termGroupId
            join TermsGroupTerms tgt on tgt.termGroupDetailId = tgd.id
                where tgt.termGroupStatus = 'ACTIVE'
                and t.id = tgt.termId
            """)
    List<Terms> getConnectedActiveTermsByTermsGroupId(@Param("termGroupId") Long termGroupId);


    @Query(value = """
            select count (pd.id) > 0 from ProductDetails pd
                where pd.terms.id = :termId
                and pd.product.productStatus in (:productStatuses)
            """)
    boolean hasConnectionToProduct(
            @Param("termId") Long termId,
            @Param("productStatuses") List<ProductStatus> productStatuses
    );


    @Query(value = """
            select count (sd.id) > 0 from ServiceDetails sd
                where sd.terms.id = :termId
                and sd.service.status in (:serviceStatuses)
            """)
    boolean hasConnectionToService(
            @Param("termId") Long termId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses
    );


    @Query("""
            select new bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse(t.id, t.name) from Terms t
            where t.groupDetailId is null
            and t.status = 'ACTIVE'
            and not exists
            (
              select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              where pd.terms.id = t.id
              and cast(pd.productDetailStatus as string) in('ACTIVE', 'INACTIVE')
              and p.productStatus = 'ACTIVE'
            )
            and not exists
            (
              select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              where sd.terms.id = t.id
              and cast(sd.status as string) in ('ACTIVE','INACTIVE')
              and s.status = 'ACTIVE'
            )
            and (:prompt is null or concat(lower(t.name), t.id) like :prompt)
            order by t.createDate desc
            """)
    Page<AvailableProductRelatedEntitiesResponse> findAvailableTermsForProduct(@Param("prompt") String prompt, PageRequest pageRequest);

    @Query("""
            select t.id from Terms t
            where t.groupDetailId is null
            and t.status = 'ACTIVE'
            and not exists
            (
              select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              where pd.terms.id = t.id
              and cast(pd.productDetailStatus as string) in('ACTIVE', 'INACTIVE')
              and p.productStatus = 'ACTIVE'
            )
            and not exists
            (
              select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              where sd.terms.id = t.id
              and cast(sd.status as string) in ('ACTIVE','INACTIVE')
              and s.status = 'ACTIVE'
            )
            """)
    List<Long> findAllAvailableTermIdsForProduct();

    @Query(
            value = """
                    select t.id from terms.terms t
                        where t.group_detail_id is null
                        and t.status = 'ACTIVE'
                        and not exists (
                            select 1 from product.products p
                            join product.product_details pd on pd.product_id = p.id
                                where pd.term_id = t.id
                                and p.status = 'ACTIVE'
                        )
                        and not exists (
                            select 1 from service.services s
                            join service.service_details sd on sd.service_id = s.id
                                where sd.term_id = t.id
                                and s.status = 'ACTIVE'
                        )
                        and
                        (t.contract_entry_into_force is null or (
                        ((cast(t.contract_entry_into_force as text[]) && '{DATE_CHANGE_OF_CBG}') = false)
                            and 
                        (cast(t.contract_entry_into_force as text[]) && '{FIRST_DELIVERY}') = false))
                      and
                      (t.start_initial_term_of_contract is null or (
                        ((cast(t.start_initial_term_of_contract as text[]) && '{DATE_OF_CHANGE_OF_CBG}') = false)
                            and 
                        (cast(t.start_initial_term_of_contract as text[]) && '{FIRST_DELIVERY}') = false))
                    """, nativeQuery = true
    )
    List<Long> findAllAvailableTermIdsForService(List<Long> ids);


    @Query(
            value = """
                    select t.id as id,
                           concat(t.name, ' (', t.id, ')') as name
                                              from terms.terms t
                                                  where t.group_detail_id is null
                                                  and t.status = 'ACTIVE'
                                                  and not exists (
                                                      select 1 from product.products p
                                                      join product.product_details pd on pd.product_id = p.id
                                                          where pd.term_id = t.id
                                                          and p.status = 'ACTIVE'
                                                  )
                                                  and not exists (
                                                      select 1 from service.services s
                                                      join service.service_details sd on sd.service_id = s.id
                                                          where sd.term_id = t.id
                                                          and s.status = 'ACTIVE'
                                                  )
                                                  and
                                              (t.contract_entry_into_force is null or (
                                                ((cast(t.contract_entry_into_force as text[]) && '{DATE_CHANGE_OF_CBG}') = false)
                                                    and 
                                                (cast(t.contract_entry_into_force as text[]) && '{FIRST_DELIVERY}') = false))
                                              and
                                              (t.start_initial_term_of_contract is null or (
                                                ((cast(t.start_initial_term_of_contract as text[]) && '{DATE_OF_CHANGE_OF_CBG}') = false)
                                                    and 
                                                (cast(t.start_initial_term_of_contract as text[]) && '{FIRST_DELIVERY}') = false))
                                                   and (:prompt is null or concat(lower(t.name), t.id) like :prompt)
                                              order by t.create_date desc
                    """, nativeQuery = true
    )
    Page<ShortResponseProjection> findAvailableTermsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );


    @Query(
            nativeQuery = true,
            value = """
                    select t.id
                    from terms.term_groups tg
                    join terms.term_group_details tgd on tgd.group_id = tg.id
                    join terms.term_group_terms tgt on tgt.term_group_detail_id = tgd.id
                    join terms.terms t on t.id = tgt.term_id and tgt.term_id = t.id
                        where tg.id = :termGroupId
                        and tg.status = 'ACTIVE'
                        and t.status = 'ACTIVE'
                        and tgt.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(start_date) from terms.term_group_details dt
                            where dt.group_id = tg.id
                            and dt.start_date <= now()
                        )
                    """
    )
    Long getTermIdFromCurrentTermGroup(
            @Param("termGroupId") Long termGroupId
    );

    @Query(
            value = """
                      select t.*
                      from terms.terms t
                      where t.id =  :id
                      and t.status = 'ACTIVE'
                       and
                      (t.contract_entry_into_force is null or (
                        ((cast(t.contract_entry_into_force as text[]) && '{DATE_CHANGE_OF_CBG}') = false)
                            and 
                        (cast(t.contract_entry_into_force as text[]) && '{FIRST_DELIVERY}') = false))
                      and
                      (t.start_initial_term_of_contract is null or (
                        ((cast(t.start_initial_term_of_contract as text[]) && '{DATE_OF_CHANGE_OF_CBG}') = false)
                            and 
                        (cast(t.start_initial_term_of_contract as text[]) && '{FIRST_DELIVERY}') = false))
                    """, nativeQuery = true
    )
    Optional<Terms> findByIdForService(
            @Param("id") Long id
    );

    @Query(nativeQuery = true,
            value = """
                    select
                     t.id
                    from
                     terms.terms t
                     where
                      t.id =  :id
                      and
                       t.status = 'ACTIVE'
                      and
                      (exists(select 1
                               from service.service_details sd
                                join service.services s on sd.service_id = s.id
                                 and sd.term_id = t.id
                                 and s.status =  'ACTIVE')
                       or
                       exists(select 1
                               from terms.term_group_terms tgt
                               join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
                               join terms.term_groups tg on tgd.group_id = tg.id
                                and tg.status = 'ACTIVE'
                               join service.service_details sd on sd.term_group_id = tg.id
                               join service.services s on sd.service_id = s.id
                                and s.status = 'ACTIVE'
                               where tgt.term_id  = t.id
                                and tgt.status = 'ACTIVE')
                       )
                    """
    )
    Optional<Long> findTermsByIdWhichIsPartOfTheService(
            @Param("id") Long id
    );

    @Query(
            nativeQuery = true,
            value =
                    """
                            select coalesce(max('true'),'false') as is_connected from terms.terms t
                                  where t.id = :termid and
                                   (exists (select 1
                                            from product.products p
                                                 join product.product_details pd
                                                   on pd.product_id = p.id
                                                  and p.status = 'ACTIVE'
                                                 join product_contract.contract_details cd
                                                   on cd.product_detail_id =  pd.id
                                                 join product_contract.contracts c
                                                   on cd.contract_id =  c.id
                                                  and c.status = 'ACTIVE'
                                             where pd.term_id = t.id)
                                    or
                                    exists
                                            (select 1
                                                        from product.products p
                                                             join product.product_details pd
                                                               on pd.product_id = p.id
                                                              and p.status = 'ACTIVE'                                 
                                                             join product_contract.contract_details cd
                                                               on cd.product_detail_id =  pd.id
                                                             join product_contract.contracts c
                                                               on cd.contract_id =  c.id
                                                              and c.status = 'ACTIVE'                             
                                                             join terms.term_groups tg
                                                               on pd.term_group_id = tg.id
                                                              and tg.status = 'ACTIVE'
                                                             join terms.term_group_details tgd
                                                               on tgd.group_id = tg.id
                                                             join terms.term_group_terms tgt
                                                               on tgt.term_group_detail_id = tgd.id
                                                              and tgt.term_id =  t.id
                                                              and tgt.status = 'ACTIVE'
                                                              )
                                    or
                                    exists (select 1
                                                from service.services s
                                                     join service.service_details sd
                                                       on sd.service_id = s.id
                                                      and s.status = 'ACTIVE'
                                                     join service_contract.contract_details cd
                                                       on cd.service_detail_id =  sd.id
                                                     join service_contract.contracts c
                                                       on cd.contract_id =  c.id
                                                      and c.status = 'ACTIVE'
                                                 where sd.term_id = t.id)
                                    or
                                    exists (select 1
                                                from service.services s
                                                     join service.service_details sd
                                                       on sd.service_id = s.id
                                                      and s.status = 'ACTIVE'
                                                     join service_order.orders o
                                                       on o.service_detail_id =  sd.id
                                                 where sd.term_id = t.id
                                                   and o.status = 'ACTIVE'
                                                   )                
                                    or
                                    exists
                                            (select 1
                                                        from service.services s
                                                             join service.service_details sd
                                                               on sd.service_id = s.id
                                                              and s.status = 'ACTIVE'
                                                             join service_contract.contract_details cd
                                                               on cd.service_detail_id =  sd.id
                                                             join service_contract.contracts c
                                                               on cd.contract_id = c.id
                                                              and c.status = 'ACTIVE'
                                                             join terms.term_groups tg
                                                               on sd.term_group_id = tg.id
                                                              and tg.status = 'ACTIVE'
                                                             join terms.term_group_details tgd
                                                               on tgd.group_id = tg.id
                                                             join terms.term_group_terms tgt
                                                               on tgt.term_group_detail_id = tgd.id
                                                              and tgt.term_id =  t.id
                                                              and tgt.status = 'ACTIVE'
                                                              )
                                   or
                                    exists
                                            (select 1
                                                        from service.services s
                                                             join service.service_details sd
                                                               on sd.service_id = s.id
                                                              and s.status = 'ACTIVE'
                                                             join service_order.orders o
                                                               on o.service_detail_id =  sd.id
                                                              and o.status = 'ACTIVE'
                                                             join terms.term_groups tg
                                                               on sd.term_group_id = tg.id
                                                              and tg.status = 'ACTIVE'
                                                             join terms.term_group_details tgd
                                                               on tgd.group_id = tg.id
                                                             join terms.term_group_terms tgt
                                                               on tgt.term_group_detail_id = tgd.id
                                                              and tgt.term_id =  t.id
                                                              and tgt.status = 'ACTIVE'
                                                              )   
                            
                                   )
                            """
    )
    boolean hasLockedConnection(
            @Param("termid") Long id
    );
}
