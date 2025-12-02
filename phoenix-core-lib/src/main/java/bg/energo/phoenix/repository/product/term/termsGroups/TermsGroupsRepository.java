package bg.energo.phoenix.repository.product.term.termsGroups;

import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedGroupEntityResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedGroupEntityResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupListingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TermsGroupsRepository extends JpaRepository<TermsGroups, Long> {
    Optional<TermsGroups> findByIdAndStatusIn(Long id, List<TermGroupStatus> statuses);

    @Query(
            """ 
                select new bg.energo.phoenix.model.response.termsGroup.TermsGroupListingResponse(
                        tg.id,
                        tgd.name,
                        t.noInterestOnOverdueDebts,
                        tg.status,
                        tg.createDate
                    )
                    from TermsGroups tg
                    join TermGroupDetails tgd on tgd.groupId = tg.id
                    join TermsGroupTerms tgt2 on tgt2.termGroupDetailId = tgd.id
                    join Terms t on t.id = tgt2.termId
                    where tg.id in (
                        select tg1.id from TermsGroups tg1
                        join TermGroupDetails tgd1 on tgd1.groupId = tg1.id
                        where (:columnName is null
                            or (:columnName = 'ALL'
                                and (lower(tgd1.name) like :columnValue
                                or exists(select 1 from TermsGroupTerms tgt
                                    join Terms t2 on tgt.termId = t2.id
                                    where tgt.termGroupDetailId = tgd1.id and tgt.termGroupStatus = 'ACTIVE'
                                    and t2.status = 'ACTIVE' and lower(t2.name) like :columnValue)
                                or exists(select 1 from TermsGroupTerms tgt
                                    join InvoicePaymentTerms ipt2 on ipt2.termId = tgt.termId
                                    where tgt.termGroupDetailId = tgd1.id and tgt.termGroupStatus = 'ACTIVE'
                                    and ipt2.status = 'ACTIVE' and lower(ipt2.name) like :columnValue))
                            or (:columnName = 'TERMS_GROUP_NAME' and lower(tgd1.name) like :columnValue)
                            or (:columnName = 'TERMS_NAME' and exists(select 1 from TermsGroupTerms tgt join Terms t2 on tgt.termId = t2.id
                                    where tgt.termGroupDetailId = tgd1.id and tgt.termGroupStatus = 'ACTIVE'
                                    and t2.status = 'ACTIVE' and lower(t2.name) like :columnValue))
                            or (:columnName = 'PAYMENT_TERM_NAME'
                                and exists(select 1 from TermsGroupTerms tgt
                                    join InvoicePaymentTerms ipt2 on ipt2.termId = tgt.termId
                                    where tgt.termGroupDetailId = tgd1.id and tgt.termGroupStatus = 'ACTIVE'
                                    and ipt2.status = 'ACTIVE' and lower(ipt2.name) like :columnValue)))
                        )
                        and (coalesce(:excludeVersion,'0') = '0'
                            or (:excludeVersion = 'excludeOld'
                                    and tgd1.startDate >= (select max(tt.startDate) from TermGroupDetails tt
                                                where tt.groupId = tg1.id and tt.startDate <= current_date))
                            or (:excludeVersion = 'excludeFuture'
                                    and tgd1.startDate <= current_date
                            )
                            or (:excludeVersion = 'excludeOldAndFuture'
                                    and tgd1.startDate =
                                              (select max(tt.startDate) from TermGroupDetails tt
                                                where tt.groupId = tg1.id and tt.startDate <= current_date)
                            )
                        )
                    )
                    and tgt2.termGroupStatus = 'ACTIVE'
                    and tg.status in (:statuses)
                    and tgd.startDate = (
                        select max(tgd3.startDate) from TermGroupDetails tgd3
                        where tgd3.groupId = tg.id
                        and tgd3.startDate <= current_date
                    )
            """
    )
    Page<TermsGroupListingResponse> filter(
            @Param("columnValue") String prompt,
            @Param("columnName") String searchField,
            @Param("statuses") List<TermGroupStatus> showDeletedTerms,
            @Param("excludeVersion") String excludeVersion,
            Pageable pageable
    );


    @Query(
              value = """
                    select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse(
                        t.id,
                        concat(td.name, ' (', t.id, ')')
                    )
                    from TermsGroups t
                    join TermGroupDetails td on t.id = td.groupId
                    and t.id in(
                        select t1.id from TermsGroups t1
                        join TermGroupDetails td1 on t1.id = td1.groupId
                            where (:prompt is null or (lower(td1.name) like lower(:prompt) or cast(t1.id as string) like :prompt))
                    )
                    and t.status = 'ACTIVE'
                    and td.startDate = (
                        select max(tgd3.startDate) from TermGroupDetails tgd3
                        where tgd3.groupId = t.id
                        and tgd3.startDate <= current_date
                    )
                    order by t.createDate desc
              """
    )
    Page<CopyDomainWithVersionBaseResponse> findByCopyDomainWithVersionBaseRequest(
            String prompt,
            Pageable pageable
    );


    @Query(value = """
            select count (pd.id) > 0 from ProductDetails pd
                where pd.termsGroups.id = :termGroupId
                and pd.product.productStatus in (:productStatuses)
            """)
    boolean hasConnectionToProduct(
            @Param("termGroupId") Long termGroupId,
            @Param("productStatuses") List<ProductStatus> productStatuses
    );

    @Query(value = """
            select count (sd.id) > 0 from ServiceDetails sd
                where sd.termsGroups.id = :termGroupId
                and sd.service.status in (:serviceStatuses)
            """)
    boolean hasConnectionToService(
            @Param("termGroupId") Long termGroupId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses
    );

    @Query(nativeQuery = true,
            value = """
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from terms.term_groups tg
                    join terms.term_group_details tgd on tgd.group_id = tg.id
                        where tg.id in (
                            select tg1.id from terms.term_groups tg1
                            join terms.term_group_details tgd1 on tgd1.group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from terms.term_group_details tgd3
                                where tgd3.group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
            """,
            countQuery = """
                    select count(1) from terms.term_groups tg
                    join terms.term_group_details tgd on tgd.group_id = tg.id
                        where tg.id in (
                            select tg1.id from terms.term_groups tg1
                            join terms.term_group_details tgd1 on tgd1.group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from terms.term_group_details tgd3
                                where tgd3.group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableProductRelatedGroupEntityResponse> findAvailableTermGroupsForProduct(@Param("prompt") String prompt, PageRequest pageRequest);


    @Query(nativeQuery = true,
            value = """
                    select id,name
                    from
                    (select distinct
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name,
                        tg.create_date,
                        tgd.start_date
                    from terms.term_groups tg
                    join terms.term_group_details tgd on tgd.group_id = tg.id
                    join terms.term_group_terms tgt on tgt.term_group_detail_id = tgd.id
                     and tgt.status = 'ACTIVE'
                    join terms.terms t on tgt.term_id = t.id
                     and t.status = 'ACTIVE'
                     and
                      array_to_string(t.contract_entry_into_force, ',') not like '%DATE_CHANGE_OF_CBG%'
                     and
                      array_to_string(t.contract_entry_into_force, ',') not like '%FIRST_DELIVERY%'
                     and
                      array_to_string(t.start_initial_term_of_contract, ',') not like '%DATE_OF_CHANGE_OF_CBG%'
                     and
                      array_to_string(t.start_initial_term_of_contract, ',') not like '%FIRST_DELIVERY%'
                        where tg.status = 'ACTIVE'
                        and (:prompt is null or lower(tgd.name) like :prompt or cast(tg.id as text) like :prompt)) as tbl
                        where tbl.start_date = (
                            select max(tgd3.start_date) from terms.term_group_details tgd3
                                where tgd3.group_id = tbl.id
                                and tgd3.start_date <= current_date
                        )
                    order by tbl.create_date desc
                    """,
            countQuery = """
                    select count(1)  from
                    (select distinct
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name,
                        tg.create_date,
                        tgd.start_date
                    from terms.term_groups tg
                    join terms.term_group_details tgd on tgd.group_id = tg.id
                    join terms.term_group_terms tgt on tgt.term_group_detail_id = tgd.id
                     and tgt.status = 'ACTIVE'
                    join terms.terms t on tgt.term_id = t.id
                     and t.status = 'ACTIVE'
                     and
                      array_to_string(t.contract_entry_into_force, ',') not like '%DATE_CHANGE_OF_CBG%'
                     and
                      array_to_string(t.contract_entry_into_force, ',') not like '%FIRST_DELIVERY%'
                     and
                      array_to_string(t.start_initial_term_of_contract, ',') not like '%DATE_OF_CHANGE_OF_CBG%'
                     and
                      array_to_string(t.start_initial_term_of_contract, ',') not like '%FIRST_DELIVERY%'
                        where tg.status = 'ACTIVE'
                        and (:prompt is null or lower(tgd.name) like :prompt or cast(tg.id as text) like :prompt)) as tbl
                        where tbl.start_date = (
                            select max(tgd3.start_date) from terms.term_group_details tgd3
                                where tgd3.group_id = tbl.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableServiceRelatedGroupEntityResponse> findAvailableTermsGroupsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                         select
                           tg.*
                       from terms.term_groups tg
                        where tg.id =  :id
                          and tg.status = 'ACTIVE'
                          and not exists
                          ---------------------
                          (select 1 from terms.term_group_details tgd
                             join terms.term_group_terms tgt on tgt.term_group_detail_id = tgd.id
                              and tgt.status = 'ACTIVE'
                             join terms.terms t on tgt.term_id = t.id
                              and t.status = 'ACTIVE'
                             where tgd.group_id = tg.id
                              and
                             (array_to_string(t.contract_entry_into_force, ',') like '%DATE_CHANGE_OF_CBG%'
                              or
                             array_to_string(t.contract_entry_into_force, ',') like '%FIRST_DELIVERY%'
                              or
                             array_to_string(t.start_initial_term_of_contract, ',') like '%DATE_OF_CHANGE_OF_CBG%'
                              or
                             array_to_string(t.start_initial_term_of_contract, ',') like '%FIRST_DELIVERY%')
                                                       )
                    """
    )
    Optional<TermsGroups> findByIdForService(
            @Param("id") Long id
    );

    @Query(nativeQuery = true,
            value = """
                    select
                     *
                    from
                     terms.term_groups tg
                     where
                      tg.id =  :id
                      and
                       tg.status = 'ACTIVE'
                      and
                      exists(select 1
                               from service.service_details sd
                                join service.services s on sd.service_id = s.id
                                 and sd.term_group_id = tg.id
                                 and s.status =  'ACTIVE')
                    """
    )
    Optional<Long> findTermsGroupByIdWhichIsPartOfTheService(
            @Param("id") Long id
    );

    @Query(nativeQuery = true,
            value = """
                    select coalesce(max('true'),'false') as is_connected from terms.term_groups tg
                    where tg.id = :termgroupid and
                     (exists(select 1 
                        from product.products p
                        join product.product_details pd
                     		      on pd.product_id = p.id
                         and p.status = 'ACTIVE'
                         and pd.term_group_id = tg.id
                        join terms.term_group_details tgd 
                          on tgd.group_id = tg.id
                              join terms.term_group_terms tgt
                                on tgt.term_group_detail_id = tgd.id
                               and tgt.status = 'ACTIVE'
                        join product_contract.contract_details cd 
                          on cd.product_detail_id =  pd.id
                        join product_contract.contracts c
                          on cd.contract_id =  c.id
                         and c.status = 'ACTIVE'
                             )
                    or
                     exists(select 1 
                        from service.services s
                        join service.service_details sd
                     		      on sd.service_id = s.id
                         and s.status = 'ACTIVE'
                         and sd.term_group_id = tg.id
                        join terms.term_group_details tgd 
                          on tgd.group_id = tg.id
                              join terms.term_group_terms tgt
                                on tgt.term_group_detail_id = tgd.id
                               and tgt.status = 'ACTIVE'
                        join service_contract.contract_details cd 
                          on cd.service_detail_id =  sd.id
                        join product_contract.contracts c
                          on cd.contract_id =  c.id
                         and c.status = 'ACTIVE'
                             )
                    or
                     exists(select 1 
                        from service.services s
                        join service.service_details sd
                     		      on sd.service_id = s.id
                         and s.status = 'ACTIVE'
                         and sd.term_group_id = tg.id
                        join terms.term_group_details tgd 
                          on tgd.group_id = tg.id
                              join terms.term_group_terms tgt
                                on tgt.term_group_detail_id = tgd.id
                               and tgt.status = 'ACTIVE'
                              join service_order.orders o 
                                on o.service_detail_id =  sd.id
                            and o.status = 'ACTIVE'
                             )
                     )
                    """
    )
    boolean hasLockedConnection(
            @Param("termgroupid") Long id
    );

}
