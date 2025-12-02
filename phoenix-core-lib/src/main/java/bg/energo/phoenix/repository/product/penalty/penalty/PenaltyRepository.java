package bg.energo.phoenix.repository.product.penalty.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.response.contract.action.ActionPenaltyResponse;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyListMiddleResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    @Query(nativeQuery = true,
            value = """
                    select
                      tbl.id as id,
                      tbl.name as name,
                      text(tbl.penalty_party_receiver) as partyReceivingPenalties,
                      tbl.applicability as applicability,
                      coalesce(tbl.group_product_service_name, 'Available') as available,
                      tbl.status as status,
                      tbl.create_date as createDate
                      from(select p.id,
                            p.name,
                            p.penalty_party_receiver,
                            p.applicability,
                            p.status,
                                 (select pgd.name from terms.penalty_group_details pgd
                                   join terms.penalty_groups pg on pgd.penalty_group_id = pg.id
                                    and pg.status ='ACTIVE'
                                   join terms.penalty_group_penalties pgp
                                   on pgp.penalty_group_detail_id = pgd.id
                                    and pgp.penalty_id = p.id             
                                    and pgp.status ='ACTIVE'
                                   union
                                  select pd.name from product.product_penalties pp
                                   join product.product_details pd
                                    on pp.product_detail_id = pd.id
                                   and pp.penalty_id = p.id
                                   and pp.status = 'ACTIVE'
                                   join product.products p
                                    on pd.product_id = p.id
                                    and p.status = 'ACTIVE'
                                  union
                                  select sd.name from service.service_penalties sp
                                   join service.service_details sd
                                    on sp.service_detail_id = sd.id
                                    and sp.penalty_id = p.id              
                                    and sp.status = 'ACTIVE'
                                   join service.services s
                                    on sd.service_id = s.id
                                     and s.status = 'ACTIVE' limit 1
                                 ) as group_product_service_name,
                            p.create_date
                     from terms.penalties p
                      where text(p.status) in(:statuses)
                       and ((:penaltyReceivingParties) is null or cast(p.penalty_party_receiver as text[]) && cast(:penaltyReceivingParties as text[]))
                       and ((:applicability) is null or text(p.applicability) in(:applicability))
                       and (:searchBy is null
                        or (:searchBy = 'ALL' and (lower(p.name) like :prompt
                        or lower(p.contract_clause_number) like :prompt
                        or lower(p.process_start_code) like :prompt
                        or lower(p.additional_info) like :prompt
                        or exists(select 1
                                   From terms.penalty_payment_terms ppt2
                                    where ppt2.penalty_id = p.id
                                     and lower(ppt2.name) like :prompt)
                            ))
                        or ((:searchBy = 'NAME' and lower(p.name) like :prompt)
                               or (:searchBy = 'PAYMENT_TERM_NAME'
                                    and exists(select 1
                                                From terms.penalty_payment_terms ppt2
                                                where ppt2.penalty_id = p.id
                                                 and lower(ppt2.name) like :prompt))))
                        ) as tbl
                      where  (coalesce(:available, '0') = '0' or :available = 'YES' and tbl.group_product_service_name is null or
                            :available = 'NO' and tbl.group_product_service_name is not null)
                    """,
            countQuery = """
                        select
                        count(tbl.id)
                        from(select p.id,
                           p.name,
                           p.penalty_party_receiver,
                           p.applicability,
                           p.status,
                                (select pgd.name from terms.penalty_group_details pgd
                                  join terms.penalty_groups pg on pgd.penalty_group_id = pg.id
                                   and pg.status ='ACTIVE'
                                  join terms.penalty_group_penalties pgp
                                  on pgp.penalty_group_detail_id = pgd.id
                                   and pgp.penalty_id = p.id             
                                   and pgp.status ='ACTIVE'
                                  union
                                 select pd.name from product.product_penalties pp
                                  join product.product_details pd
                                   on pp.product_detail_id = pd.id
                                  and pp.penalty_id = p.id
                                  and pp.status = 'ACTIVE'
                                  join product.products p
                                   on pd.product_id = p.id
                                   and p.status = 'ACTIVE'
                                 union
                                 select sd.name from service.service_penalties sp
                                  join service.service_details sd
                                   on sp.service_detail_id = sd.id
                                   and sp.penalty_id = p.id              
                                   and sp.status = 'ACTIVE'
                                  join service.services s
                                   on sd.service_id = s.id
                                    and s.status = 'ACTIVE' limit 1
                                ) as group_product_service_name,
                           p.create_date
                    from terms.penalties p
                     where text(p.status) in(:statuses)
                      and ((:penaltyReceivingParties) is null or cast(p.penalty_party_receiver as text[]) && cast(:penaltyReceivingParties as text[]))
                      and ((:applicability) is null or text(p.applicability) in(:applicability))
                      and (:searchBy is null
                       or (:searchBy = 'ALL' and (lower(p.name) like :prompt
                       or lower(p.contract_clause_number) like :prompt
                       or lower(p.process_start_code) like :prompt
                       or lower(p.additional_info) like :prompt
                       or exists(select 1
                                  From terms.penalty_payment_terms ppt2
                                   where ppt2.penalty_id = p.id
                                    and lower(ppt2.name) like :prompt)
                           ))
                       or ((:searchBy = 'NAME' and lower(p.name) like :prompt)
                              or (:searchBy = 'PAYMENT_TERM_NAME'
                                   and exists(select 1
                                               From terms.penalty_payment_terms ppt2
                                               where ppt2.penalty_id = p.id
                                                and lower(ppt2.name) like :prompt))))
                       ) as tbl
                     where  (coalesce(:available, '0') = '0' or :available = 'YES' and tbl.group_product_service_name is null or
                           :available = 'NO' and tbl.group_product_service_name is not null)
                        """
    )
    Page<PenaltyListMiddleResponse> findAll(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("penaltyReceivingParties") String penaltyReceivingParties,
            @Param("applicability") List<String> applicability,
            @Param("statuses") List<String> statusList,
            @Param("available") String available,
            Pageable pageable
    );

    @Query(
            """
                    Select p from Penalty p
                    where p.penaltyGroupDetailId is null
                    and p.id in :penaltyIds
                    AND p.status = 'ACTIVE'
                    and not exists (select 1 from ServicePenalty sp
                             where sp.penalty.id = p.id
                             and sp.serviceDetails.service.status = 'ACTIVE'
                             and sp.status = 'ACTIVE'
                                )
                    and not exists (select 1 from ProductPenalty pp
                            where pp.penalty.id = p.id
                            and pp.productDetails.product.productStatus = 'ACTIVE'
                            and pp.productSubObjectStatus = 'ACTIVE'
                            )
                    """
    )
    List<Penalty> getAvailablePenalties(List<Long> penaltyIds);


    @Query(
            """
                    Select p from Penalty p
                    where ((p.penaltyGroupDetailId is null or p.penaltyGroupDetailId = :groupDetailId)
                            and not exists (select 1 from ServicePenalty sp
                                     where sp.penalty.id = p.id
                                     and sp.serviceDetails.service.status = 'ACTIVE'
                                     and sp.status = 'ACTIVE'
                                        )
                            and not exists (select 1 from ProductPenalty pp
                                    where pp.penalty.id = p.id
                                    and pp.productDetails.product.productStatus = 'ACTIVE'
                                    and pp.productSubObjectStatus = 'ACTIVE'
                                    )        
                            ) 
                    and p.id in :penaltyIds
                    """
    )
    List<Penalty> getAvailablePenaltiesForGroupDetail(List<Long> penaltyIds, Long groupDetailId);

    @Query(value = """
            select p from Penalty p
            join PenaltyGroupDetails pgd on pgd.penaltyGroupId = :penaltyGroupId
            join PenaltyGroupPenalty pgp on pgp.penaltyGroupDetailId = pgd.id
            where pgp.status = 'ACTIVE'
            """
    )
    List<Penalty> findAllActiveByPenaltyGroupId(@Param("penaltyGroupId") Long penaltyGroupId);


    @Query("""
             select p
             from PenaltyGroupPenalty pgp
             join Penalty p on p.id = pgp.penaltyId
             where pgp.penaltyGroupDetailId = :penaltyGroupDetailId and pgp.status in :statuses
            """)
    List<Penalty> findAllByPenaltyGroupDetailIdAndStatusIn(Long penaltyGroupDetailId, List<EntityStatus> statuses);


    @Query("""
                select new bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse(p.id, concat(p.name, ' (', p.id, ')'))
                from Penalty p
                where (:prompt is null
                or lower(p.name) like :prompt
                or cast(p.id as string)  = :prompt)
                and p.status in (:statuses)
                order by p.id DESC
            """)
    Page<CopyDomainListResponse> filterForCopy(@Param("prompt") String prompt,
                                               @Param("statuses") List<EntityStatus> statuses,
                                               Pageable pageable);

    Optional<Penalty> findByIdAndStatus(Long id, EntityStatus status);

    Optional<Penalty> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);

    List<Penalty> findByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    @Query(
            value = """
                        select p from Penalty p
                            where p.penaltyGroupDetailId is null
                            and p.status = 'ACTIVE'
                            and not exists (select 1 from ServicePenalty sp
                                where sp.penalty.id = p.id
                                and sp.serviceDetails.service.status = 'ACTIVE'
                                and sp.status = 'ACTIVE'
                            )
                            and not exists (select 1 from ProductPenalty pp
                                where pp.penalty.id = p.id
                                and pp.productDetails.product.productStatus = 'ACTIVE'
                                and pp.productSubObjectStatus = 'ACTIVE'
                            )
                            and (:prompt is null or concat(lower(p.name), p.id) like :prompt)
                            order by p.createDate desc
                    """
    )
    Page<Penalty> getAvailablePenaltiesByPrompt(String prompt, PageRequest of);


    @Query(
            value = """
                        select count (pd.id) > 0 from ProductDetails pd
                        join ProductPenalty pp on pd.id = pp.productDetails.id
                            where pd.product.productStatus in (:productStatuses)
                            and pp.productSubObjectStatus in (:ppStatuses)
                            and pp.penalty.id = :pId
                    """
    )
    boolean hasConnectionToProduct(
            @Param("pId") Long pId,
            @Param("productStatuses") List<ProductStatus> productStatuses,
            @Param("ppStatuses") List<ProductSubObjectStatus> ppStatuses
    );


    @Query(
            value = """
                        select count (sd.id) > 0 from ServiceDetails sd
                        join ServicePenalty sp on sd.id = sp.serviceDetails.id
                            where sd.service.status in (:serviceStatuses)
                            and sp.status in (:spStatuses)
                            and sp.penalty.id = :pId
                    """
    )
    boolean hasConnectionToService(
            @Param("pId") Long pId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses,
            @Param("spStatuses") List<ServiceSubobjectStatus> spStatuses
    );


    @Query("""
            select new bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse(penalty.id, penalty.name) from Penalty penalty
            where penalty.status = 'ACTIVE'
            and penalty.penaltyGroupDetailId is null
            and not exists
            (
              select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductPenalty pp on 
              pp.productDetails.id = pd.id
              and
              pp.penalty.id = penalty.id
              where p.productStatus = 'ACTIVE'
              and pp.productSubObjectStatus = 'ACTIVE'
            )
            and not exists
            (
              select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServicePenalty st on 
              st.serviceDetails.id = st.id
              and
              st.penalty.id = penalty.id
              where s.status = 'ACTIVE'
              and st.status = 'ACTIVE'
            )
            and (:prompt is null or concat(lower(penalty.name), penalty.id) like :prompt)
            order by penalty.createDate desc
            """)
    Page<AvailableProductRelatedEntitiesResponse> findAvailablePenaltiesForProduct(@Param("prompt") String prompt, PageRequest pageRequest);

    @Query("""
            select penalty.id from Penalty penalty
            where penalty.status = 'ACTIVE'
            and penalty.penaltyGroupDetailId is null
            and not exists
            (
              select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductPenalty pp on 
              pp.productDetails.id = pd.id
              and
              pp.penalty.id = penalty.id
              where p.productStatus = 'ACTIVE'
              and pp.productSubObjectStatus = 'ACTIVE'
            )
            and not exists
            (
              select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServicePenalty st on 
              st.serviceDetails.id = st.id
              and
              st.penalty.id = penalty.id
              where s.status = 'ACTIVE'
              and st.status = 'ACTIVE'
            )
            """)
    List<Long> findAvailablePenaltyIdsForProduct(Collection<Long> ids);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse(
                        penalty.id,
                        penalty.name
                    )
                    from Penalty penalty
                        where penalty.status = 'ACTIVE'
                        and penalty.penaltyGroupDetailId is null
                        and not exists
                        (
                          select 1 from Product p
                          join ProductDetails pd on pd.product.id = p.id
                          join ProductPenalty pp on 
                          pp.productDetails.id = pd.id
                          and
                          pp.penalty.id = penalty.id
                          where p.productStatus = 'ACTIVE'
                          and pp.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists
                        (
                          select 1 from EPService s
                          join ServiceDetails sd on sd.service.id = s.id
                          join ServicePenalty st on 
                          st.serviceDetails.id = st.id
                          and
                          st.penalty.id = penalty.id
                          where s.status = 'ACTIVE'
                          and st.status = 'ACTIVE'
                        )
                        and (:prompt is null or concat(lower(penalty.name), penalty.id) like :prompt)
                    order by penalty.createDate desc
                    """
    )
    Page<AvailableServiceRelatedEntitiyResponse> findAvailablePenaltiesForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );


    @Query(
            value = """
                    select penalty.id from Penalty penalty
                        where penalty.status = 'ACTIVE'
                        and penalty.penaltyGroupDetailId is null
                        and not exists
                        (
                          select 1 from Product p
                          join ProductDetails pd on pd.product.id = p.id
                          join ProductPenalty pp on 
                          pp.productDetails.id = pd.id
                          and
                          pp.penalty.id = penalty.id
                          where p.productStatus = 'ACTIVE'
                          and pp.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists
                        (
                          select 1 from EPService s
                          join ServiceDetails sd on sd.service.id = s.id
                          join ServicePenalty st on 
                          st.serviceDetails.id = st.id
                          and
                          st.penalty.id = penalty.id
                          where s.status = 'ACTIVE'
                          and st.status = 'ACTIVE'
                        )
                        and penalty.id in (:ids)
                    """
    )
    List<Long> findAvailablePenaltyIdsForService(Collection<Long> ids);


    @Query(
            nativeQuery = true,
            value = """
                    select
                        id,
                        name
                    from terms.penalties p
                    where (exists(
                        select 1 from product_contract.contract_details cd
                        join product.product_penalties pp2 on pp2.product_detail_id = cd.product_detail_id
                            where pp2.penalty_id = p.id
                            and pp2.status = 'ACTIVE'
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
                        join product.product_penalty_groups ppg
                            on ppg.product_detail_id = cd.product_detail_id
                            and ppg.status = 'ACTIVE'
                        join terms.penalty_group_details pgd on pgd.penalty_group_id = ppg.penalty_group_id
                        join terms.penalty_group_penalties pgp
                            on pgp.penalty_group_detail_id = pgd.id
                            and pgp.penalty_id = p.id
                            and pgp.status = 'ACTIVE'
                                where cd.contract_id = :contractId and (
                                    :executionDate >= cd.start_date and :executionDate < coalesce((
                                        select min(start_date)
                                        from product_contract.contract_details cd1
                                        where cd1.contract_id = cd.contract_id and cd1.start_date > cd.start_date), date(:executionDate) + 1)
                                )
                                and (
                                    :executionDate >= pgd.start_date and :executionDate < coalesce((
                                        select min(start_date)
                                        from terms.penalty_group_details pgd3
                                            where pgd3.penalty_group_id = pgd.penalty_group_id
                                            and pgd3.start_date > pgd.start_date), date(:executionDate) + 1)
                                )
                    ))
                    and p.status = 'ACTIVE'
                    and (:prompt is null or lower(p.name) like :prompt)
                    and
                    (
                      (:penaltyPayer = 'CUSTOMER' and p.penalty_party_receiver && '{CUSTOMER}')
                      or (:penaltyPayer = 'EPRES' and p.penalty_party_receiver && '{ENERGO_PRO}')
                    )
                    and (:penaltyId is null or p.id = :penaltyId)
                    and exists(select 1 from terms.penalty_action_types pat  where pat.penalty_id = p.id and pat.action_type_id = :actionTypeId and pat.status = 'ACTIVE')
                    """
    )
    Page<ActionPenaltyResponse> getAvailableProductPenaltiesForAction(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate,
            @Param("penaltyPayer") String penaltyPayer,
            @Param("prompt") String prompt,
            @Param("penaltyId") Long penaltyId,
            @Param("actionTypeId") Long actionTypeId,
            Pageable pageable
    );


    @Query(
            nativeQuery = true,
            value = """
                    select
                        id,
                        name
                    from terms.penalties p
                    where (exists(
                        select 1 from service_contract.contract_details cd
                        join service.service_penalties pp2 on pp2.service_detail_id = cd.service_detail_id
                            where pp2.penalty_id = p.id
                            and pp2.status = 'ACTIVE'
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
                        join service.service_penalty_groups ppg
                            on ppg.service_detail_id = cd.service_detail_id
                            and ppg.status = 'ACTIVE'
                        join terms.penalty_group_details pgd on pgd.penalty_group_id = ppg.penalty_group_id
                        join terms.penalty_group_penalties pgp
                            on pgp.penalty_group_detail_id = pgd.id
                            and pgp.penalty_id = p.id
                            and pgp.status = 'ACTIVE'
                                where cd.contract_id = :contractId and (
                                    :executionDate >= cd.start_date and :executionDate < coalesce((
                                        select min(start_date)
                                        from service_contract.contract_details cd1
                                        where cd1.contract_id = cd.contract_id and cd1.start_date > cd.start_date), date(:executionDate) + 1) 
                                )
                                and (
                                    :executionDate >= pgd.start_date and :executionDate < coalesce((
                                        select min(start_date) 
                                        from terms.penalty_group_details pgd3 
                                            where pgd3.penalty_group_id = pgd.penalty_group_id 
                                            and pgd3.start_date > pgd.start_date), date(:executionDate) + 1) 
                                )
                    ))
                    and p.status = 'ACTIVE'
                    and (:prompt is null or lower(p.name) like :prompt)
                    and 
                    (
                      (:penaltyPayer = 'CUSTOMER' and p.penalty_party_receiver && '{CUSTOMER}')
                      or (:penaltyPayer = 'EPRES' and p.penalty_party_receiver && '{ENERGO_PRO}')
                    )
                    and (:penaltyId is null or p.id = :penaltyId)
                    and exists(select 1 from terms.penalty_action_types pat  where pat.penalty_id = p.id and pat.action_type_id = :actionTypeId and pat.status = 'ACTIVE')
                    """
    )
    Page<ActionPenaltyResponse> getAvailableServicePenaltiesForAction(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate,
            @Param("penaltyPayer") String penaltyPayer,
            @Param("prompt") String prompt,
            @Param("penaltyId") Long penaltyId,
            @Param("actionTypeId") Long actionTypeId,
            Pageable pageable
    );


    @Query(nativeQuery = true, value = """
            select case
                       when :serviceContractId is not null then :penaltyId in (select p.id
                                                                               from terms.penalties p
                                                                               where (exists(select 1
                                                                                             from service_contract.contract_details cd
                                                                                                      join service.service_penalties pp2
                                                                                                           on pp2.service_detail_id = cd.service_detail_id
                                                                                             where pp2.penalty_id = p.id
                                                                                               and pp2.status = 'ACTIVE'
                                                                                               and cd.contract_id = :serviceContractId
                                                                                               and (
                                                                                                 :executionDate >= cd.start_date and
                                                                                                 :executionDate <
                                                                                                 coalesce((select min(start_date)
                                                                                                           from service_contract.contract_details cd1
                                                                                                           where cd1.contract_id = cd.contract_id
                                                                                                             and cd1.start_date > cd.start_date),
                                                                                                          date(:executionDate) + 1)
                                                                                                 ))
                                                                                   or exists(select 1
                                                                                             from service_contract.contract_details cd
                                                                                                      join service.service_penalty_groups ppg
                                                                                                           on ppg.service_detail_id =
                                                                                                              cd.service_detail_id
                                                                                                               and
                                                                                                              ppg.status = 'ACTIVE'
                                                                                                      join terms.penalty_group_details pgd
                                                                                                           on pgd.penalty_group_id = ppg.penalty_group_id
                                                                                                      join terms.penalty_group_penalties pgp
                                                                                                           on pgp.penalty_group_detail_id =
                                                                                                              pgd.id
                                                                                                               and
                                                                                                              pgp.penalty_id = p.id
                                                                                                               and
                                                                                                              pgp.status = 'ACTIVE'
                                                                                             where cd.contract_id = :serviceContractId
                                                                                               and (
                                                                                                 :executionDate >= cd.start_date and
                                                                                                 :executionDate <
                                                                                                 coalesce((select min(start_date)
                                                                                                           from service_contract.contract_details cd1
                                                                                                           where cd1.contract_id = cd.contract_id
                                                                                                             and cd1.start_date > cd.start_date),
                                                                                                          date(:executionDate) + 1)
                                                                                                 )
                                                                                               and (
                                                                                                 :executionDate >=
                                                                                                 pgd.start_date and :executionDate <
                                                                                                                    coalesce(
                                                                                                                            (select min(start_date)
                                                                                                                             from terms.penalty_group_details pgd3
                                                                                                                             where pgd3.penalty_group_id = pgd.penalty_group_id
                                                                                                                               and pgd3.start_date > pgd.start_date),
                                                                                                                            date(:executionDate) +
                                                                                                                            1)
                                                                                                 )))
                                                                                 and p.status = 'ACTIVE'
                                                                                 and (
                                                                                   (:penaltyPayer = 'CUSTOMER' and p.penalty_party_receiver && '{CUSTOMER}')
                                                                                       or
                                                                                   (:penaltyPayer = 'EPRES' and p.penalty_party_receiver && '{ENERGO_PRO}')
                                                                                   )
                                                                                 and (:penaltyId is null or p.id = :penaltyId)
                                                                                 and exists(select 1
                                                                                            from terms.penalty_action_types pat
                                                                                            where pat.penalty_id = p.id
                                                                                              and pat.action_type_id = :actionTypeId
                                                                                              and pat.status = 'ACTIVE'))
                       else :penaltyId in (select p.id
                                           from terms.penalties p
                                           where (exists(select 1
                                                         from product_contract.contract_details cd
                                                                  join product.product_penalties pp2
                                                                       on pp2.product_detail_id = cd.product_detail_id
                                                         where pp2.penalty_id = p.id
                                                           and pp2.status = 'ACTIVE'
                                                           and cd.contract_id = :productContractId
                                                           and (
                                                             :executionDate >= cd.start_date and
                                                             :executionDate < coalesce((select min(start_date)
                                                                                        from product_contract.contract_details cd1
                                                                                        where cd1.contract_id = cd.contract_id
                                                                                          and cd1.start_date > cd.start_date),
                                                                                       date(:executionDate) + 1)
                                                             ))
                                               or exists(select 1
                                                         from product_contract.contract_details cd
                                                                  join product.product_penalty_groups ppg
                                                                       on ppg.product_detail_id = cd.product_detail_id
                                                                           and ppg.status = 'ACTIVE'
                                                                  join terms.penalty_group_details pgd
                                                                       on pgd.penalty_group_id = ppg.penalty_group_id
                                                                  join terms.penalty_group_penalties pgp
                                                                       on pgp.penalty_group_detail_id = pgd.id
                                                                           and pgp.penalty_id = p.id
                                                                           and pgp.status = 'ACTIVE'
                                                         where cd.contract_id = :productContractId
                                                           and (
                                                             :executionDate >= cd.start_date and
                                                             :executionDate < coalesce((select min(start_date)
                                                                                        from product_contract.contract_details cd1
                                                                                        where cd1.contract_id = cd.contract_id
                                                                                          and cd1.start_date > cd.start_date),
                                                                                       date(:executionDate) + 1)
                                                             )
                                                           and (
                                                             :executionDate >= pgd.start_date and
                                                             :executionDate < coalesce((select min(start_date)
                                                                                        from terms.penalty_group_details pgd3
                                                                                        where pgd3.penalty_group_id = pgd.penalty_group_id
                                                                                          and pgd3.start_date > pgd.start_date),
                                                                                       date(:executionDate) + 1)
                                                             )))
                                             and p.status = 'ACTIVE'
                                             and (
                                               (:penaltyPayer = 'CUSTOMER' and p.penalty_party_receiver && '{CUSTOMER}')
                                                   or (:penaltyPayer = 'EPRES' and p.penalty_party_receiver && '{ENERGO_PRO}')
                                               )
                                             and (:penaltyId is null or p.id = :penaltyId)
                                             and exists(select 1
                                                        from terms.penalty_action_types pat
                                                        where pat.penalty_id = p.id
                                                          and pat.action_type_id = :actionTypeId
                                                          and pat.status = 'ACTIVE')) end as is_valid
                        """)
    boolean isPenaltyValidForAction(@Param("serviceContractId") Long serviceContractId,
                                    @Param("productContractId") Long productContractId,
                                    @Param("executionDate") LocalDate executionDate,
                                    @Param("penaltyPayer") String penaltyPayer,
                                    @Param("penaltyId") Long penaltyId,
                                    @Param("actionTypeId") Long actionTypeId);

    @Query(
            nativeQuery = true,
            value =
                    """
                            select coalesce(max('true'),'false') as is_connected from terms.penalties pn
                                where pn.id = :penaltyid and
                                 (exists (select 1
                                          from product.products p
                                               join product.product_details pd
                                                 on pd.product_id = p.id
                                                and p.status = 'ACTIVE'
                                               join product.product_penalties pp
                                                 on pp.product_detail_id = pd.id
                                                and pp.penalty_id = pn.id
                                                and pp.status = 'ACTIVE'
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
                                                           join product.product_penalty_groups ppg
                                                             on ppg.product_detail_id = pd.id
                                                            and ppg.status = 'ACTIVE'
                                                           join terms.penalty_groups pg 
                                                             on ppg.penalty_group_id = pg.id
                                                            and pg.status = 'ACTIVE'
                                                           join terms.penalty_group_details pgd
                                                             on pgd.penalty_group_id = ppg.id
                                                           join terms.penalty_group_penalties pgp 
                                                             on pgp.penalty_group_detail_id = pgd.id
                                                            and pgp.penalty_id =  pn.id
                                                            and pgp.status = 'ACTIVE'
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
                                                   join service.service_penalties sp
                                                     on sp.service_detail_id = sd.id
                                                    and sp.penalty_id = pn.id
                                                    and sp.status = 'ACTIVE'                   
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
                                                   join service.service_penalties sp
                                                     on sp.service_detail_id = sd.id
                                                    and sp.penalty_id = pn.id
                                                    and sp.status = 'ACTIVE'
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
                                                           join service.service_penalty_groups spg
                                                             on spg.service_detail_id = sd.id
                                                            and spg.status = 'ACTIVE'
                                                           join terms.penalty_groups pg
                                                             on spg.penalty_group_id = pg.id
                                                            and pg.status = 'ACTIVE'
                                                           join terms.penalty_group_details pgd
                                                             on pgd.penalty_group_id = spg.id
                                                           join terms.penalty_group_penalties pgp
                                                             on pgp.penalty_group_detail_id = pgd.id
                                                            and pgp.penalty_id =  pn.id
                                                            and pgp.status = 'ACTIVE'                      
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
                                                           join service.service_penalty_groups spg
                                                             on spg.service_detail_id = sd.id
                                                            and spg.status = 'ACTIVE'
                                                           join terms.penalty_groups pg
                                                             on spg.penalty_group_id = pg.id
                                                            and pg.status = 'ACTIVE'
                                                           join terms.penalty_group_details pgd
                                                             on pgd.penalty_group_id = pg.id
                                                           join terms.penalty_group_penalties pgp
                                                             on pgp.penalty_group_detail_id = pgd.id
                                                            and pgp.penalty_id =  pn.id
                                                            and pgp.status = 'ACTIVE'
                                                           join service_order.orders o
                                                             on o.service_detail_id =  sd.id
                                                            and o.status = 'ACTIVE'
                                                            )   
                                                           
                                 )
                                                                                     """
    )
    boolean hasLockedConnection(
            @Param("penaltyid") Long id
    );

}
