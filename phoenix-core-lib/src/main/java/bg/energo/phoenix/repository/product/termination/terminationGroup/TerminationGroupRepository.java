package bg.energo.phoenix.repository.product.termination.terminationGroup;

import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroup;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.termination.terminationGroup.TerminationGroupStatus;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedGroupEntityResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedGroupEntityResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TerminationGroupRepository extends JpaRepository<TerminationGroup, Long> {

    /**
     * Executes a native SQL query to retrieve a list of termination group details with the given parameters.
     *
     * @param terminationGroupStatuses a list of status values for the termination groups to search for depending on user permissions
     * @param searchBy                 the type of search to perform, either "ALL", "TERMINATION_GROUP_NAME", or "TERMINATION_NAME"
     * @param prompt                   the search string to use for the search
     * @return a list of termination group details that match the search criteria
     */
    @Query(
            nativeQuery = true,
            value = """
                        select
                            tbl.groupId as groupId,
                            tbl.name as name,
                            tbl.numberOfTerminations as numberOfTerminations,
                            tbl.dateOfCreation as dateOfCreation,
                            tbl.status as status
                        from (
                        select
                            tg.id as groupId,
                            tgd.name as name,
                            tgd.start_date as startDate,
                            (select count(1) from product.termination_group_terminations tgt where tgt.termination_group_detail_id = tgd.id and tgt.status = 'ACTIVE') as numberOfTerminations,
                            (select max(start_date) from product.termination_group_details tt where tt.termination_group_id = tg.id and start_date <= now()) as currentstartdate,
                            tg.create_date as dateOfCreation,
                            tg.status as status
                        from
                          product.termination_group_details tgd
                          join product.termination_groups tg on tg.id = tgd.termination_group_id
                            where tg.id in(
                                select tg.id from product.termination_group_details tgd
                                left join product.termination_group_terminations tgt on tgt.termination_group_detail_id = tgd.id and tgt.status = 'ACTIVE'
                                left join product.terminations t on tgt.termination_id = t.id
                                join product.termination_groups tg on tg.id = tgd.termination_group_id
                                where text(tg.status) in (:terminationGroupStatuses)
                                           and (coalesce(:excludeVersion,'0') = '0' or
                                                 (:excludeVersion = 'PASTVERSION' and
                                                  tgd.start_date >=
                                                  (select max(start_date) from product.termination_group_details tt
                                                    where tt.termination_group_id = tg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'RESPECTVERSION' and
                                                  tgd.start_date <=
                                                  (select max(start_date) from product.termination_group_details tt
                                                    where tt.termination_group_id = tg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'PASTANDRESPECTVERSION' and
                                                  tgd.start_date =
                                                  (select max(start_date) from product.termination_group_details tt
                                                    where tt.termination_group_id = tg.id and start_date <= current_date)
                                                 )                                                
                                                )     
                                and (
                                    :searchBy is null
                                    or (:searchBy = 'ALL' and (lower(tgd.name) like :prompt or lower(t.name) like :prompt))
                                    or (
                                        (:searchBy = 'TERMINATION_GROUP_NAME' and lower(tgd.name) like :prompt)
                                        or (:searchBy = 'TERMINATION_NAME' and lower(t.name) like :prompt)
                                    )
                                )
                            )
                        ) as tbl
                        where startDate = currentstartdate
                    """,
            countQuery = """
                        select
                            count(1)
                        from (
                        select
                            tg.id as groupId,
                            tgd.name as name,
                            tgd.start_date as startDate,
                            (select count(1) from product.termination_group_terminations tgt where tgt.termination_group_detail_id = tgd.id and tgt.status = 'ACTIVE') as numberOfTerminations,
                            (select max(start_date) from product.termination_group_details tt where tt.termination_group_id = tg.id and start_date <= now()) as currentstartdate,
                            tg.create_date as dateOfCreation,
                            tg.status as status
                        from
                          product.termination_group_details tgd
                          join product.termination_groups tg on tg.id = tgd.termination_group_id
                            where tg.id in(
                                select tg.id from product.termination_group_details tgd
                                left join product.termination_group_terminations tgt on tgt.termination_group_detail_id = tgd.id and tgt.status = 'ACTIVE'
                                left join product.terminations t on tgt.termination_id = t.id
                                join product.termination_groups tg on tg.id = tgd.termination_group_id
                                where text(tg.status) in (:terminationGroupStatuses)
                                           and (coalesce(:excludeVersion,'0') = '0' or
                                                 (:excludeVersion = 'PASTVERSION' and
                                                  tgd.start_date >=
                                                  (select max(start_date) from product.termination_group_details tt
                                                    where tt.termination_group_id = tg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'RESPECTVERSION' and
                                                  tgd.start_date <=
                                                  (select max(start_date) from product.termination_group_details tt
                                                    where tt.termination_group_id = tg.id and start_date <= current_date)
                                                 )
                                                or
                                                 (:excludeVersion = 'PASTANDRESPECTVERSION' and
                                                  tgd.start_date =
                                                  (select max(start_date) from product.termination_group_details tt
                                                    where tt.termination_group_id = tg.id and start_date <= current_date)
                                                 )                                                
                                                )     
                                and (
                                    :searchBy is null
                                    or (:searchBy = 'ALL' and (lower(tgd.name) like :prompt or lower(t.name) like :prompt))
                                    or (
                                        (:searchBy = 'TERMINATION_GROUP_NAME' and lower(tgd.name) like :prompt)
                                        or (:searchBy = 'TERMINATION_NAME' and lower(t.name) like :prompt)
                                    )
                                )
                            )
                        ) as tbl
                        where startDate = currentstartdate
                    """
    )
    Page<TerminationGroupListResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("terminationGroupStatuses") List<String> terminationGroupStatuses,
            @Param("excludeVersion") String excludeVersion,
            Pageable pageable
    );

    Optional<TerminationGroup> findByIdAndStatusIn(Long id, List<TerminationGroupStatus> statuses);

    @Query(
            """
                select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse(
                    tg.id,
                    concat(tgd.name, ' (', tg.id, ')')
                )
                from TerminationGroup tg
                join TerminationGroupDetails tgd on tgd.terminationGroupId = tg.id
                and tgd.startDate = (
                    select max (tgd.startDate) from TerminationGroupDetails tgd
                    where tgd.terminationGroupId=tg.id
                    and tgd.startDate <= :currentDate
                )
                where tg.id in (
                    select distinct ntg.id from TerminationGroup ntg
                    join TerminationGroupDetails ntgd on ntgd.terminationGroupId=ntg.id
                        where (:prompt is null or (lower(ntgd.name) like :prompt or cast(ntg.id as string) like :prompt))
                        and ntg.status ='ACTIVE'
                )
            """
    )
    Page<CopyDomainWithVersionBaseResponse> findByCopyGroupBaseRequest(String prompt, LocalDate currentDate, Pageable pageable);

    boolean existsByIdAndStatusIn(Long groupId, List<TerminationGroupStatus> statuses);

    List<TerminationGroup> findByIdInAndStatusIn(List<Long> ids, List<TerminationGroupStatus> statuses);


    @Query(
            value = """
            select count (pd.id) > 0 from ProductDetails pd
            join ProductTerminationGroups ptg on pd.id = ptg.productDetails.id
                where pd.product.productStatus in (:productStatuses)
                and ptg.productSubObjectStatus in (:ptgStatuses)
                and ptg.terminationGroup.id = :tgId
        """
    )
    boolean hasConnectionToProduct(
            @Param("tgId") Long tgId,
            @Param("productStatuses") List<ProductStatus> productStatuses,
            @Param("ptgStatuses") List<ProductSubObjectStatus> ptgStatuses
    );


    @Query(
            value = """
            select count (sd.id) > 0 from ServiceDetails sd
            join ServiceTerminationGroup stg on sd.id = stg.serviceDetails.id
                where sd.service.status in (:serviceStatuses)
                and stg.status in (:stgStatuses)
                and stg.terminationGroup.id = :tgId
        """
    )
    boolean hasConnectionToService(
            @Param("tgId") Long tgId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses,
            @Param("stgStatuses") List<ServiceSubobjectStatus> stgStatuses
    );

    @Query(nativeQuery = true,
            value = """            
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from product.termination_groups tg
                    join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                        where tg.id in (
                            select tg1.id from product.termination_groups tg1
                            join product.termination_group_details tgd1 on tgd1.termination_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from product.termination_group_details tgd3
                                where tgd3.termination_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
            """,
            countQuery = """
                    select count(1) from product.termination_groups tg
                    join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                        where tg.id in (
                            select tg1.id from product.termination_groups tg1
                            join product.termination_group_details tgd1 on tgd1.termination_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from product.termination_group_details tgd3
                                where tgd3.termination_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableProductRelatedGroupEntityResponse> findAvailableTerminationGroupsForProduct(@Param("prompt") String prompt, PageRequest pageRequest);


    @Query(nativeQuery = true,
            value = """
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from product.termination_groups tg
                    join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                        where tg.id in (
                            select tg1.id from product.termination_groups tg1
                            join product.termination_group_details tgd1 on tgd1.termination_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from product.termination_group_details tgd3
                                where tgd3.termination_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
                    """,
            countQuery = """
                    select count(1) from product.termination_groups tg
                    join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                        where tg.id in (
                            select tg1.id from product.termination_groups tg1
                            join product.termination_group_details tgd1 on tgd1.termination_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from product.termination_group_details tgd3
                                where tgd3.termination_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableServiceRelatedGroupEntityResponse> findAvailableTerminationGroupsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    select coalesce(max('true'),'false') as is_connected from product.termination_groups tg
                      where tg.id = :terminationgroupid and
                       (exists(select 1 
                                from product.products p
                                join product.product_details pd
                                  on pd.product_id = p.id
                                 and p.status = 'ACTIVE'
                                join product.product_termination_groups ptg
                                  on ptg.product_detail_id = pd.id
                                 and ptg.termination_group_id = tg.id
                                 and ptg.status = 'ACTIVE'
                                join product.termination_group_details tgd
                                  on tgd.termination_group_id = tg.id
                                join product.termination_group_terminations tgt 
                                  on tgt.termination_group_detail_id = tgd.id
                                 and tgt.status = 'ACTIVE'
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
                                join service.service_termination_groups ptg
                                  on ptg.service_detail_id = sd.id
                                 and ptg.termination_group_id = tg.id
                                 and ptg.status = 'ACTIVE'
                                join product.termination_group_details tgd
                                  on tgd.termination_group_id = tg.id
                                join product.termination_group_terminations tgt 
                                  on tgt.termination_group_detail_id = tgd.id
                                 and tgt.status = 'ACTIVE'
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
                                join service.service_termination_groups ptg
                                  on ptg.service_detail_id = sd.id
                                 and ptg.termination_group_id = tg.id
                                 and ptg.status = 'ACTIVE'
                                join product.termination_group_details tgd
                                  on tgd.termination_group_id = tg.id
                                join product.termination_group_terminations tgt 
                                  on tgt.termination_group_detail_id = tgd.id
                                 and tgt.status = 'ACTIVE'
                                join service_order.orders o 
                                  on o.service_detail_id =  sd.id
                                 and o.status = 'ACTIVE')
                       )
                    """
    )
    boolean hasLockedConnection(
            @Param("terminationgroupid") Long id
    );

}
