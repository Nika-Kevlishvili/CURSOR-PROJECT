package bg.energo.phoenix.repository.product.termination.terminations;

import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationStatus;
import bg.energo.phoenix.model.response.contract.action.ActionTerminationResponse;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse;
import bg.energo.phoenix.model.response.terminations.TerminationForNotificationResponse;
import bg.energo.phoenix.model.response.terminations.TerminationsListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TerminationRepository extends JpaRepository<Termination, Long> {

    @Query("""
                select t
                from Termination t
                where t.status in :statuses
                and t.id = :terminationId
            """)
    Optional<Termination> findByIdAndStatusIn(
            @Param("terminationId") Long terminationId,
            @Param("statuses") List<TerminationStatus> statuses
    );

    @Query(nativeQuery = true,
            value = """
                    select
                          tbl.id,
                          tbl.name,
                          tbl.auto_termination as autoTermination,
                          tbl.notice_due as noticeDue,
                          date(tbl.create_date) as createDate,
                          tbl.status as status,
                          coalesce(tbl.group_product_service_name, 'Available') as available
                      from
                          (select
                          t.id,
                          t.name,
                          t.auto_termination,
                          t.notice_due,
                           (select tgd.name
                             from product.termination_group_details tgd
                             join product.termination_groups tg
                               on tgd.termination_group_id = tg.id
                              and tg.status = 'ACTIVE'
                             join product.termination_group_terminations tgt
                               on tgt.termination_group_detail_id = tgd.id
                              and tgt.termination_id = t.id
                              and tgt.status = 'ACTIVE'
                             union
                           select pd.name
                              from product.product_terminations pt
                                join product.product_details pd on pt.product_detail_id = pd.id
                                 and pt.status = 'ACTIVE'
                                join product.products p on pd.product_id = p.id
                                 and p.status = 'ACTIVE'
                                 and pt.termination_id = t.id
                             union
                              select sd.name
                              from service.service_terminations st
                                join service.service_details sd on st.service_detail_id = sd.id
                                 and st.termination_id = t.id
                                 and st.status = 'ACTIVE'
                                join service.services s on sd.service_id = s.id
                                 and s.status = 'ACTIVE' limit 1
                             ) group_product_service_name,
                          t.create_date,
                          t.status
                        from
                          product.terminations t
                        where
                          (
                            :columnName is null
                            or (
                              :columnName = 'NAME'
                              and lower(t.name) like :columnValue
                            )
                            or (
                              :columnName = 'ALL'
                              and lower(t.name) like :columnValue
                            )
                          )
                          and text(t.status) in(:statuses)
                          and (
                            :autoTermination is null
                            or :autoTermination = t.auto_termination
                          )
                          and (
                            :noticeDue is null
                            or :noticeDue = t.notice_due
                          )
                      ) as tbl
                     where (coalesce(:available, '0') = '0' or :available = 'YES' and tbl.group_product_service_name is null or
                           :available = 'NO' and tbl.group_product_service_name is not null)
                    """,
            countQuery = """
                    select
                    count(tbl.id)
                    from
                      (select
                          t.id,
                          t.name,
                          t.auto_termination,
                          t.notice_due,
                           (select tgd.name
                             from product.termination_group_details tgd
                             join product.termination_groups tg
                               on tgd.termination_group_id = tg.id
                              and tg.status = 'ACTIVE'
                             join product.termination_group_terminations tgt
                               on tgt.termination_group_detail_id = tgd.id
                              and tgt.termination_id = t.id
                              and tgt.status = 'ACTIVE'
                             union
                           select pd.name
                              from product.product_terminations pt
                                join product.product_details pd on pt.product_detail_id = pd.id
                                 and pt.status = 'ACTIVE'
                                join product.products p on pd.product_id = p.id
                                 and p.status = 'ACTIVE'
                                 and pt.termination_id = t.id
                             union
                              select sd.name
                              from service.service_terminations st
                                join service.service_details sd on st.service_detail_id = sd.id
                                 and st.termination_id = t.id
                                 and st.status = 'ACTIVE'
                                join service.services s on sd.service_id = s.id
                                 and s.status = 'ACTIVE' limit 1
                             ) group_product_service_name,
                          t.create_date,
                          t.status
                        from
                          product.terminations t
                        where
                          (
                            :columnName is null
                            or (
                              :columnName = 'NAME'
                              and lower(t.name) like :columnValue
                            )
                            or (
                              :columnName = 'ALL'
                              and lower(t.name) like :columnValue
                            )
                          )
                          and text(t.status) in(:statuses)
                          and (
                            :autoTermination is null
                            or :autoTermination = t.auto_termination
                          )
                          and (
                            :noticeDue is null
                            or :noticeDue = t.notice_due
                          )
                      ) as tbl
                     where (coalesce(:available, '0') = '0' or :available = 'YES' and tbl.group_product_service_name is null or
                           :available = 'NO' and tbl.group_product_service_name is not null)
                    """)
    Page<TerminationsListResponse> filter(
            @Param("columnValue") String prompt,
            @Param("columnName") String searchField,
            @Param("statuses") List<String> statuses,
            @Param("autoTermination") Boolean autoTermination,
            @Param("noticeDue") Boolean noticeDue,
            @Param("available") String available,
            Pageable pageable
    );

    List<Termination> findByStatus(TerminationStatus status);

    /**
     * Retrieves a list of all available {@link Termination} entities and, optionally, filters by a name or id.
     *
     * @param prompt prompt to filter by terminations
     * @return a list of available {@link Termination} objects
     */
    @Query(
            value = """
                        select t from Termination t
                            where t.terminationGroupDetailId is null
                            and t.status = 'ACTIVE'
                            and not exists (select 1 from ServiceTermination st
                                where st.termination.id = t.id
                                and st.serviceDetails.service.status = 'ACTIVE'
                                and st.status = 'ACTIVE'
                            )
                            and not exists (select 1 from ProductTerminations pt
                                where pt.termination.id = t.id
                                and pt.productDetails.product.productStatus = 'ACTIVE'
                                and pt.productSubObjectStatus = 'ACTIVE'
                            )
                            and (:prompt is null or concat(lower(t.name), t.id) like :prompt)
                            order by t.createDate desc
                    """
    )
    Page<Termination> getAvailableTerminations(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    /**
     * Retrieves a list of available {@link Termination} entities filtered by a list of {@link Termination} IDs.
     *
     * @param terminationIds a List of IDs of terminations to filter by
     * @return a list of available {@link Termination} objects
     */
    @Query(
            value = """
                        select t from Termination t
                            where t.terminationGroupDetailId is null
                            and t.status = 'ACTIVE'
                            and not exists (select 1 from ServiceTermination st
                                where st.termination.id = t.id
                                and st.serviceDetails.service.status = 'ACTIVE'
                                and st.status = 'ACTIVE'
                            )
                            and not exists (select 1 from ProductTerminations pt
                                where pt.termination.id = t.id
                                and pt.productDetails.product.productStatus = 'ACTIVE'
                                and pt.productSubObjectStatus = 'ACTIVE'
                            )
                            and ((:terminationIds) is null or t.id in (:terminationIds))
                    """
    )
    List<Termination> getAvailableTerminationsIn(
            @Param("terminationIds") List<Long> terminationIds
    );

    @Query(
            value = """
                        select t from Termination t
                        join TerminationGroupDetails tgd on tgd.terminationGroupId = :terminationGroupId
                        join TerminationGroupTermination tgt on tgt.terminationGroupDetailId = tgd.id
                            where tgt.status = 'ACTIVE'
                            and t.id = tgt.terminationId
                    """
    )
    List<Termination> getConnectedActiveTerminationsByTerminationGroupId(
            @Param("terminationGroupId") Long terminationGroupId
    );

    @Query("""
                select new bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse(
                    t.id, 
                    concat(t.name, ' (', t.id, ')')
                )
                from Termination t 
                    where (:prompt is null or lower(t.name) like :prompt or cast(t.id as string) like :prompt)
                    and t.status in (:statuses)
                order by t.id DESC
            """)
    Page<CopyDomainListResponse> filterForCopy(
            @Param("prompt") String prompt,
            @Param("statuses") List<TerminationStatus> statuses,
            Pageable pageable
    );

    List<Termination> findByIdInAndStatusIn(List<Long> ids, List<TerminationStatus> statuses);

    List<Termination> findByTerminationGroupDetailIdAndStatusIn(Long terminationGroupDetailId, List<TerminationStatus> statuses);


    @Query(
            value = """
                        select count (pd.id) > 0 from ProductDetails pd
                        join ProductTerminations pt on pd.id = pt.productDetails.id
                            where pd.product.productStatus in (:productStatuses)
                            and pt.productSubObjectStatus in (:ptStatuses)
                            and pt.termination.id = :tId
                    """
    )
    boolean hasConnectionToProduct(
            @Param("tId") Long tId,
            @Param("productStatuses") List<ProductStatus> productStatuses,
            @Param("ptStatuses") List<ProductSubObjectStatus> ptStatuses
    );


    @Query(
            value = """
                        select count (sd.id) > 0 from ServiceDetails sd
                        join ServiceTermination st on sd.id = st.serviceDetails.id
                            where sd.service.status in (:serviceStatuses)
                            and st.status in (:stStatuses)
                            and st.termination.id = :tId
                    """
    )
    boolean hasConnectionToService(
            @Param("tId") Long tId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses,
            @Param("stStatuses") List<ServiceSubobjectStatus> stStatuses
    );

    @Query("""
            select new bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse(t.id,t.name) from Termination t
            where t.status = 'ACTIVE'
             and t.terminationGroupDetailId is null
             and not exists
             (
               select 1 from Product p
               join ProductDetails pd on pd.product.id = p.id
               join ProductTerminations pt on 
               pt.productDetails.id = pd.id
               and
               pt.termination.id = t.id
               where p.productStatus = 'ACTIVE'
               and pt.productSubObjectStatus = 'ACTIVE'
             )
             and not exists
             (
               select 1 from EPService s
               join ServiceDetails sd on sd.service.id = s.id
               join ServiceTermination st on 
               st.serviceDetails.id = sd.id
               and
               st.termination.id = t.id
               where s.status = 'ACTIVE'
               and st.status = 'ACTIVE'
             )
             and (:prompt is null or concat(lower(t.name), t.id) like :prompt)
             order by t.createDate desc
             """)
    Page<AvailableProductRelatedEntitiesResponse> findAvailableTerminationsForProduct(@Param("prompt") String prompt, PageRequest pageRequest);

    @Query("""
            select t.id from Termination t
            where t.status = 'ACTIVE'
             and t.terminationGroupDetailId is null
             and not exists
             (
               select 1 from Product p
               join ProductDetails pd on pd.product.id = p.id
               join ProductTerminations pt on 
               pt.productDetails.id = pd.id
               and
               pt.termination.id = t.id
               where p.productStatus = 'ACTIVE'
               and pt.productSubObjectStatus = 'ACTIVE'
             )
             and not exists
             (
               select 1 from EPService s
               join ServiceDetails sd on sd.service.id = s.id
               join ServiceTermination st on 
               st.serviceDetails.id = sd.id
               and
               st.termination.id = t.id
               where s.status = 'ACTIVE'
               and st.status = 'ACTIVE'
             )
             """)
    List<Long> findAvailableTerminationIdsForProduct(Collection<Long> ids);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse(
                        t.id,
                        t.name
                    )
                    from Termination t
                        where t.status = 'ACTIVE'
                        and t.terminationGroupDetailId is null
                        and not exists
                         (
                           select 1 from Product p
                           join ProductDetails pd on pd.product.id = p.id
                           join ProductTerminations pt on 
                           pt.productDetails.id = pd.id
                           and
                           pt.termination.id = t.id
                           where p.productStatus = 'ACTIVE'
                           and pt.productSubObjectStatus = 'ACTIVE'
                         )
                         and not exists
                         (
                           select 1 from EPService s
                           join ServiceDetails sd on sd.service.id = s.id
                           join ServiceTermination st on 
                           st.serviceDetails.id = sd.id
                           and
                           st.termination.id = t.id
                           where s.status = 'ACTIVE'
                           and st.status = 'ACTIVE'
                         )
                        and (:prompt is null or concat(lower(t.name), t.id) like :prompt)
                    order by t.createDate desc
                    """
    )
    Page<AvailableServiceRelatedEntitiyResponse> findAvailableTerminationsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );


    @Query(
            value = """
                            select t.id from Termination t
                                where t.status = 'ACTIVE'
                                and t.terminationGroupDetailId is null
                                and not exists
                                 (
                                   select 1 from Product p
                                   join ProductDetails pd on pd.product.id = p.id
                                   join ProductTerminations pt on 
                                   pt.productDetails.id = pd.id
                                   and
                                   pt.termination.id = t.id
                                   where p.productStatus = 'ACTIVE'
                                   and pt.productSubObjectStatus = 'ACTIVE'
                                 )
                                 and not exists
                                 (
                                   select 1 from EPService s
                                   join ServiceDetails sd on sd.service.id = s.id
                                   join ServiceTermination st on 
                                   st.serviceDetails.id = sd.id
                                   and
                                   st.termination.id = t.id
                                   where s.status = 'ACTIVE'
                                   and st.status = 'ACTIVE'
                                 )
                                and t.id in (:ids)
                    """
    )
    List<Long> findAvailableTerminationIdsForService(Collection<Long> ids);


    @Query(
            nativeQuery = true,
            value = """
                    select
                        id,
                        name
                    from product.terminations t
                        where (exists (
                            select 1
                            from product_contract.contract_details cd
                            join product.product_terminations pt2 on pt2.product_detail_id = cd.product_detail_id
                                where pt2.termination_id = t.id
                                and pt2.status = 'ACTIVE'
                                and cd.contract_id = :contractId
                                and (
                                    :executionDate >= cd.start_date and :executionDate < coalesce((
                                        select min(start_date)
                                        from product_contract.contract_details cd1
                                            where cd1.contract_id = cd.contract_id
                                            and cd1.start_date > cd.start_date), date(:executionDate) + 1)
                                )
                    )
                    or exists(
                        select 1 from product_contract.contract_details cd
                        join product.product_termination_groups ptg
                            on ptg.product_detail_id = cd.product_detail_id
                            and ptg.status = 'ACTIVE'
                        join product.termination_group_details tgd on tgd.termination_group_id = ptg.termination_group_id
                        join product.termination_group_terminations tgt
                            on tgt.termination_group_detail_id  = tgd.id
                            and tgt.termination_id  = t.id
                            and tgt.status = 'ACTIVE'
                                where cd.contract_id = :contractId
                                and (
                                    :executionDate >= cd.start_date and :executionDate < coalesce((
                                        select min(start_date)
                                        from product_contract.contract_details cd1
                                            where cd1.contract_id = cd.contract_id
                                            and cd1.start_date > cd.start_date), date(:executionDate) + 1)
                                )
                                and (
                                    :executionDate >= tgd.start_date and :executionDate < coalesce((
                                    select min(start_date)
                                    from product.termination_group_details tgd2
                                        where tgd2.termination_group_id = tgd.termination_group_id
                                        and tgd2.start_date > tgd.start_date), date(:executionDate) + 1)
                                )
                    ))
                    and (:prompt is null or lower(t.name) like :prompt)
                    and t.status = 'ACTIVE'
                    and t.auto_termination = 'true'
                    and t.event = 'EXPIRATION_OF_THE_NOTICE'
                    and (:terminationId is null or t.id = :terminationId)
                    """
    )
    Page<ActionTerminationResponse> getAvailableProductTerminationsForAction(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate,
            @Param("prompt") String prompt,
            @Param("terminationId") Long terminationId,
            Pageable pageable
    );


    @Query(
            nativeQuery = true,
            value = """
                    select
                        id,
                        name
                    from product.terminations t
                        where (exists (
                            select 1
                            from service_contract.contract_details cd
                            join service.service_terminations pt2 on pt2.service_detail_id = cd.service_detail_id
                                where pt2.termination_id = t.id
                                and pt2.status = 'ACTIVE'
                                and cd.contract_id = :contractId
                                and (
                                    :executionDate >= cd.start_date and :executionDate < coalesce((
                                        select min(start_date)
                                        from service_contract.contract_details cd1
                                            where cd1.contract_id = cd.contract_id
                                            and cd1.start_date > cd.start_date), date(:executionDate) + 1)
                                )
                    )
                    or exists(
                        select 1 from service_contract.contract_details cd
                        join service.service_termination_groups ptg
                            on ptg.service_detail_id = cd.service_detail_id
                            and ptg.status = 'ACTIVE'
                        join product.termination_group_details tgd on tgd.termination_group_id = ptg.termination_group_id
                        join product.termination_group_terminations tgt
                            on tgt.termination_group_detail_id  = tgd.id
                            and tgt.termination_id  = t.id
                            and tgt.status = 'ACTIVE'
                                where cd.contract_id = :contractId
                                and (
                                    :executionDate >= cd.start_date and :executionDate < coalesce((
                                        select min(start_date)
                                        from service_contract.contract_details cd1
                                            where cd1.contract_id = cd.contract_id
                                            and cd1.start_date > cd.start_date), date(:executionDate) + 1)
                                )
                                and (
                                    :executionDate >= tgd.start_date and :executionDate < coalesce((
                                    select min(start_date)
                                    from product.termination_group_details tgd2
                                        where tgd2.termination_group_id = tgd.termination_group_id
                                        and tgd2.start_date > tgd.start_date), date(:executionDate) + 1)
                                )
                    ))
                    and (:prompt is null or lower(t.name) like :prompt)
                    and t.status = 'ACTIVE'
                    and t.auto_termination = 'true'
                    and t.event = 'EXPIRATION_OF_THE_NOTICE'
                    and (:terminationId is null or t.id = :terminationId)
                    """
    )
    Page<ActionTerminationResponse> getAvailableServiceTerminationsForAction(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate,
            @Param("prompt") String prompt,
            @Param("terminationId") Long terminationId,
            Pageable pageable
    );

    @Query(value = """
            select t from product.terminations t
                        where t.id in (
                        select pt.termination_id from product.product_terminations pt where pt.product_detail_id = :productDetailsId and pt.status = 'ACTIVE')
                        or t.id in (
                        select tgt.termination_id  from product.product_termination_groups ptg
                        join product.termination_group_details tgd on tgd.termination_group_id =ptg.termination_group_id
                        join product.termination_group_terminations tgt on tgt.termination_group_detail_id = tgd.id
                        where tgd.start_Date =  (select max(tgd2.start_Date) from product.termination_group_details tgd2
                                                    where tgd2.termination_group_id = ptg.termination_group_id
                                                     and tgd2.start_date < :startDate)
                        and ptg.product_detail_id = :productDetailsId
                        and tgt.status='ACTIVE'
                        and ptg.status  = 'ACTIVE')
             and t.event = 'EXPIRATION_OF_THE_CONTRACT_TERM'
             order by t.create_date limit 1
            """, nativeQuery = true)
    Optional<Termination> findByProductDetailId(Long productDetailsId, LocalDate startDate);

    @Query(
            nativeQuery = true,
            value =
                    """
                            select coalesce(max('true'),'false') as is_connected from product.terminations t
                                                                         where t.id = :terminationid and
                                                                          (exists (select 1
                                                                                   from product.products p
                                                                                        join product.product_details pd
                                                                                          on pd.product_id = p.id
                                                                                         and p.status = 'ACTIVE'
                                                                                        join product.product_terminations pt
                                                                                          on pt.product_detail_id = pd.id
                                                                                         and pt.termination_id = t.id
                                                                                         and pt.status = 'ACTIVE'
                                                                                        join product_contract.contract_details cd
                                                                                          on cd.product_detail_id =  pd.id
                                                                                        join product_contract.contracts c
                                                                                          on cd.contract_id =  c.id
                                                                                         and c.status = 'ACTIVE')
                                                                           or
                                                                           exists
                                                                                   (select 1
                                                                                               from product.products p
                                                                                                    join product.product_details pd
                                                                                                      on pd.product_id = p.id
                                                                                                     and p.status = 'ACTIVE'
                                                                                                    join product.product_termination_groups ptg
                                                                                                      on ptg.product_detail_id = pd.id
                                                                                                     and ptg.status = 'ACTIVE'
                                                                                                    join product.termination_groups tg
                                                                                                      on ptg.termination_group_id = tg.id
                                                                                                     and tg.status = 'ACTIVE'
                                                                                                    join product.termination_group_details tgd
                                                                                                      on tgd.termination_group_id = tg.id
                                                                                                    join product.termination_group_terminations tgt
                                                                                                      on tgt.termination_group_detail_id = tgd.id
                                                                                                     and tgt.termination_id =  t.id
                                                                                                     and tgt.status = 'ACTIVE'
                                                                                                    join product_contract.contract_details cd
                                                                                                      on cd.product_detail_id =  pd.id
                                                                                                    join product_contract.contracts c
                                                                                                      on cd.contract_id =  c.id
                                                                                                     and c.status = 'ACTIVE')
                                                                           or
                                                                           exists (select 1
                                                                                       from service.services s
                                                                                            join service.service_details sd
                                                                                              on sd.service_id = s.id
                                                                                             and s.status = 'ACTIVE'
                                                                                            join service.service_terminations st
                                                                                              on st.service_detail_id = sd.id
                                                                                             and st.termination_id = t.id
                                                                                             and st.status = 'ACTIVE'                   
                                                                                            join service_contract.contract_details cd
                                                                                              on cd.service_detail_id =  sd.id
                                                                                            join service_contract.contracts c
                                                                                              on cd.contract_id =  c.id
                                                                                             and c.status = 'ACTIVE')
                                                                           or
                                                                           exists (select 1
                                                                                       from service.services s
                                                                                            join service.service_details sd
                                                                                              on sd.service_id = s.id
                                                                                             and s.status = 'ACTIVE'
                                                                                            join service.service_terminations st
                                                                                              on st.service_detail_id = sd.id
                                                                                             and st.termination_id = t.id
                                                                                             and st.status = 'ACTIVE'
                                                                                            join service_order.orders o
                                                                                              on o.service_detail_id =  sd.id
                                                                                             and o.status = 'ACTIVE'
                                                                                          )
                                                                           or
                                                                           exists
                                                                                   (select 1
                                                                                               from service.services s
                                                                                                    join service.service_details sd
                                                                                                      on sd.service_id = s.id
                                                                                                     and s.status = 'ACTIVE'
                                                                                                    join service.service_termination_groups stg
                                                                                                      on stg.service_detail_id = sd.id
                                                                                                     and stg.status = 'ACTIVE'
                                                                                                    join product.termination_groups tg
                                                                                                      on stg.termination_group_id = tg.id
                                                                                                     and tg.status = 'ACTIVE'
                                                                                                    join product.termination_group_details tgd
                                                                                                      on tgd.termination_group_id = tg.id
                                                                                                    join product.termination_group_terminations tgt
                                                                                                      on tgt.termination_group_detail_id = tgd.id
                                                                                                     and tgt.termination_id =  t.id
                                                                                                     and tgt.status = 'ACTIVE'                      
                                                                                                    join service_contract.contract_details cd
                                                                                                      on cd.service_detail_id =  sd.id
                                                                                                    join service_contract.contracts c
                                                                                                      on cd.contract_id = c.id
                                                                                                     and c.status = 'ACTIVE'
                                                                                                     )
                                                                          or
                                                                           exists
                                                                                   (select 1
                                                                                               from service.services s
                                                                                                    join service.service_details sd
                                                                                                      on sd.service_id = s.id
                                                                                                     and s.status = 'ACTIVE'
                                                                                                    join service.service_termination_groups stg
                                                                                                      on stg.service_detail_id = sd.id
                                                                                                     and stg.status = 'ACTIVE'
                                                                                                    join product.termination_groups tg
                                                                                                      on stg.termination_group_id = tg.id
                                                                                                     and tg.status = 'ACTIVE'
                                                                                                    join product.termination_group_details tgd
                                                                                                      on tgd.termination_group_id = tg.id
                                                                                                    join product.termination_group_terminations tgt
                                                                                                      on tgt.termination_group_detail_id = tgd.id
                                                                                                     and tgt.termination_id =  t.id
                                                                                                     and tgt.status = 'ACTIVE'                  
                                                                                                    join service_order.orders o
                                                                                                      on o.service_detail_id =  sd.id
                                                                                                     and o.status = 'ACTIVE'
                                                                                                     )   
                                                                                                    
                                                                          )
                                                                  """
    )
    boolean hasLockedConnection(
            @Param("terminationid") Long id
    );

    @Query(nativeQuery = true, value = """
            select cc.id                                                    as communicationId,
                   coalesce(pcd.customer_detail_id, scd.customer_detail_id) as customerDetailId,
                   trim(string_agg(ccc.contact_value, ';'))                 as emails,
                   (trim(string_agg(text(ccc.id), ' ')))                    as commContactIds,
                   t.contract_template_id                                   as templateId,
                   a.id                                                     as actionId,
                   t.id                                                     as terminationId,
                   td.subject                                               as emailSubject
            from action.actions a
                     join product.terminations t
                          on (a.termination_id = t.id
                              and t.auto_termination = true and t.contract_template_id is not null and
                              t.event = 'EXPIRATION_OF_THE_NOTICE' and t.notice_due = true and
                              t.notice_due_value_min is not null and
                              t.auto_email_notification = true
                              )
                     left join product_contract.contracts pc on (a.product_contract_id = pc.id and pc.status = 'ACTIVE' and
                                                                 pc.contract_status in
                                                                 ('ACTIVE_IN_TERM',
                                                                  'ACTIVE_IN_PERPETUITY',
                                                                  'ENTERED_INTO_FORCE')
                and case
                        when t.calculate_from = 'CONTRACT_END_DATE' then pc.contract_term_end_date is not null
                            and pc.contract_term_end_date - ((case
                                                                  when t.notice_due_type = 'DAY'
                                                                      then interval '1 days'
                                                                  when t.notice_due_type = 'MONTH'
                                                                      then interval '1 month'
                                                                  when t.notice_due_type = 'WEEK'
                                                                      then interval '1 week' end) *
                                                             t.notice_due_value_min) = current_date
                        else true end)
                     left join product_contract.contract_details pcd
                               on (pc.id = pcd.contract_id and pcd.start_date = (select max(inpcd.start_date)
                                                                                 from product_contract.contract_details inpcd
                                                                                 where inpcd.contract_id = pc.id
                                                                                   and inpcd.start_date <= current_date))
                     left join service_contract.contracts sc
                               on (a.service_contract_id = sc.id and sc.status = 'ACTIVE' and sc.contract_status in
                                                                                              ('ACTIVE_IN_TERM',
                                                                                               'ACTIVE_IN_PERPETUITY',
                                                                                               'ENTERED_INTO_FORCE')
                                   and
                                   case
                                       when t.calculate_from = 'CONTRACT_END_DATE' then sc.contract_term_end_date is not null
                                           and sc.contract_term_end_date - ((case
                                                                                 when t.notice_due_type = 'DAY'
                                                                                     then interval '1 days'
                                                                                 when t.notice_due_type = 'MONTH'
                                                                                     then interval '1 month'
                                                                                 when t.notice_due_type = 'WEEK'
                                                                                     then interval '1 week' end) *
                                                                            t.notice_due_value_min) = current_date
                                       else true end)
                     left join service_contract.contract_details scd
                               on (sc.id = scd.contract_id and scd.start_date = (select max(inscd.start_date)
                                                                                 from service_contract.contract_details inscd
                                                                                 where inscd.contract_id = sc.id
                                                                                   and inscd.start_date <= current_date))
                     left join customer.customer_communications cc on (coalesce(pcd.customer_communication_id_for_contract,
                                                                                scd.customer_communication_id_for_contract) = cc.id
                and cc.status = 'ACTIVE')
                     left join customer.customer_communication_contacts ccc on (ccc.customer_communication_id = cc.id
                and ccc.status = 'ACTIVE'
                and ccc.contact_type = 'EMAIL')
                     left join template.templates temp on t.contract_template_id = temp.id
                     left join template.template_details td
                               on (temp.id = td.template_id and td.start_date = (select max(itd.start_date)
                                                                                 from template.template_details itd
                                                                                 where itd.template_id = temp.id
                                                                                   and itd.start_date <= current_date))
            where a.status = 'ACTIVE'
              and a.execution_date > current_date
              and a.penalty_payer = 'CUSTOMER'
              and (t.calculate_from = 'CONTRACT_END_DATE'
                or (a.execution_date - ((case
                                             when t.notice_due_type = 'DAY'
                                                 then interval '1 days'
                                             when t.notice_due_type = 'MONTH'
                                                 then interval '1 month'
                                             when t.notice_due_type = 'WEEK'
                                                 then interval '1 week' end) *
                                        t.notice_due_value_min) = current_date))
              and coalesce(sc.id, pc.id, 0) <> 0
            group by t.contract_template_id, coalesce(pcd.customer_detail_id, scd.customer_detail_id), cc.id, a.id, t.id, td.subject
            """)
    List<TerminationForNotificationResponse> retrieveForTerminationNotification();

}
