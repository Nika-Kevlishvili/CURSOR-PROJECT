package bg.energo.phoenix.repository.contract.action;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.documentModels.action.PenaltyDocumentResponse;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.request.contract.pod.ActionPenaltyModel;
import bg.energo.phoenix.model.response.contract.action.ActionListResponse;
import bg.energo.phoenix.model.response.contract.action.ActionResponse;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResponse;
import bg.energo.phoenix.model.response.contract.action.calculation.PodToActionMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    Optional<Action> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);

    @Query(
            nativeQuery = true,
            value = """
                    select
                        ac.id as id,
                        ac.create_date as createDate,
                        ac.status as status,
                        ac.action_status as actionStatus,
                        at.name as actionTypeName,
                        ac.notice_receiving_date as noticeReceivingDate,
                        ac.execution_date as executionDate,
                        (select coalesce(max('YES'), 'NO') from receivable.customer_liabilities al
                            where al.action_id = ac.id and al.status = 'ACTIVE' limit 1) as penaltyClaimed,
                        case when ac.penalty_claim_amount is not null
                            then concat(ac.penalty_claim_amount, ' ', (select c.name from nomenclature.currencies c where c.id = ac.penalty_claim_currency_id))
                            end as penaltyClaimAmount,
                        ac.penalty_claim_amount as penaltyClaimAmountValue,
                        ac.penalty_payer as penaltyPayer,
                        concat(c.identifier, ' (', cd.name, ')') as customerName,
                        case when ac.product_contract_id is not null
                            then (select concat(pc.contract_number, '/', to_char(pc.create_date, 'DD.MM.YY')) from product_contract.contracts pc where pc.id = ac.product_contract_id)
                            when ac.service_contract_id is not null
                            then (select concat(sc.contract_number, '/', to_char(sc.create_date, 'DD.MM.YY')) from service_contract.contracts sc where sc.id = ac.service_contract_id)
                            end as contractNumber,
                        case when :actionPodsDirection = 'ASC' then vap.name
                            when :actionPodsDirection = 'DESC' then vap.name_desc
                            else vap.name end as podIdentifiers,
                        p.name as penaltyName,
                        t.name as terminationName,
                        ac.without_penalty as withoutPenalty,
                        ac.without_auto_termination as withoutAutoTermination
                    from action.actions ac
                    join nomenclature.action_types at on ac.action_type_id = at.id
                    join customer.customers c on ac.customer_id = c.id
                    join customer.customer_details cd on c.last_customer_detail_id = cd.id
                    left join terms.penalties p on ac.penalty_id = p.id
                    left join product.terminations t on ac.termination_id = t.id
                    left join action.vw_action_pods vap on vap.action_id = ac.id
                        where ((:statuses) is null or text(ac.status) in (:statuses))
                        and ((:actionStatuses) is null or text(ac.action_status) in (:actionStatuses))
                        and ((:actionTypeIds) is null or ac.action_type_id in (:actionTypeIds))
                        and (date(:createDateFrom) is null or date(ac.create_date) >= date(:createDateFrom))
                        and (date(:createDateTo) is null or date(ac.create_date) <= date(:createDateTo))
                        and (date(:noticeReceivingDateFrom) is null or ac.notice_receiving_date >= date(:noticeReceivingDateFrom))
                        and (date(:noticeReceivingDateTo) is null or ac.notice_receiving_date <= date(:noticeReceivingDateTo))
                        and (date(:executionDateFrom) is null or ac.execution_date >= date(:executionDateFrom))
                        and (date(:executionDateTo) is null or ac.execution_date <= date(:executionDateTo))
                        and (:penaltyClaimed is null or :penaltyClaimed = (
                            select coalesce(max('true'), 'false') from action.action_liabilities al
                            where al.action_id = ac.id and al.status = 'ACTIVE' limit 1
                        ))
                        and (:calculatedPenaltyFrom is null or ac.calculated_penalty_amount >= :calculatedPenaltyFrom)
                        and (:calculatedPenaltyTo is null or ac.calculated_penalty_amount <= :calculatedPenaltyTo)
                        and ((:currencyIds) is null or ac.calculated_penalty_currency_id in (:currencyIds) or ac.penalty_claim_currency_id in (:currencyIds))
                        and (:claimedAmountFrom is null or ac.penalty_claim_amount >= :claimedAmountFrom)
                        and (:claimedAmountTo is null or ac.penalty_claim_amount <= :claimedAmountTo)
                        and ((:penaltyPayers) is null or text(ac.penalty_payer) in (:penaltyPayers))
                        and (
                            :prompt is null or (
                                :searchBy = 'ALL' and (
                                    text(ac.id) like :prompt
                                    or lower(c.identifier) like :prompt
                                    or (
                                        ac.product_contract_id is not null and exists(
                                            select 1 from product_contract.contracts pc
                                            where pc.id = ac.product_contract_id
                                            and lower(pc.contract_number) like :prompt
                                        )
                                        or (
                                        ac.service_contract_id is not null and exists(
                                            select 1 from service_contract.contracts sc
                                            where sc.id = ac.service_contract_id
                                            and lower(sc.contract_number) like :prompt
                                        )
                                        )
                                    )
                                    or lower(vap.name) like :prompt
                                    or lower(t.name) like :prompt
                                    or lower(p.name) like :prompt
                                    or lower(ac.additional_info) like :prompt
                                )
                            )
                            or (
                                (:searchBy = 'ID' and text(ac.id) like :prompt)
                                or (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                or (:searchBy = 'CONTRACT_NUMBER' and (
                                        ac.product_contract_id is not null and exists(
                                            select 1 from product_contract.contracts pc
                                            where pc.id = ac.product_contract_id
                                            and lower(pc.contract_number) like :prompt
                                        )
                                        or (
                                        ac.service_contract_id is not null and exists(
                                            select 1 from service_contract.contracts sc
                                            where sc.id = ac.service_contract_id
                                            and lower(sc.contract_number) like :prompt
                                        )
                                        )
                                    ))
                                or (:searchBy = 'POD_IDENTIFIER' and lower(vap.name) like :prompt)
                                or (:searchBy = 'PENALTY_NAME' and lower(p.name) like :prompt)
                                or (:searchBy = 'TERMINATION_NAME' and lower(t.name) like :prompt)
                                or (:searchBy = 'ADDITIONAL_INFO' and lower(ac.additional_info) like :prompt)
                            )
                        )
                    """,
            countQuery = """
                            select count(1)
                            from action.actions ac
                            join nomenclature.action_types at on ac.action_type_id = at.id
                            join customer.customers c on ac.customer_id = c.id
                            join customer.customer_details cd on c.last_customer_detail_id = cd.id
                            left join terms.penalties p on ac.penalty_id = p.id
                            left join product.terminations t on ac.termination_id = t.id
                            left join action.vw_action_pods vap on vap.action_id = ac.id
                                where ((:statuses) is null or text(ac.status) in (:statuses))
                                and ((:actionStatuses) is null or text(ac.action_status) in (:actionStatuses))
                                and ((:actionTypeIds) is null or ac.action_type_id in (:actionTypeIds))
                                and (date(:createDateFrom) is null or date(ac.create_date) >= date(:createDateFrom))
                                and (date(:createDateTo) is null or date(ac.create_date) <= date(:createDateTo))
                                and (date(:noticeReceivingDateFrom) is null or ac.notice_receiving_date >= date(:noticeReceivingDateFrom))
                                and (date(:noticeReceivingDateTo) is null or ac.notice_receiving_date <= date(:noticeReceivingDateTo))
                                and (date(:executionDateFrom) is null or ac.execution_date >= date(:executionDateFrom))
                                and (date(:executionDateTo) is null or ac.execution_date <= date(:executionDateTo))
                                and (:penaltyClaimed is null or :penaltyClaimed = (
                                    select coalesce(max('true'), 'false') from action.action_liabilities al
                                    where al.action_id = ac.id and al.status = 'ACTIVE' limit 1
                                ))
                                and (:calculatedPenaltyFrom is null or ac.calculated_penalty_amount >= :calculatedPenaltyFrom)
                                and (:calculatedPenaltyTo is null or ac.calculated_penalty_amount <= :calculatedPenaltyTo)
                                and ((:currencyIds) is null or ac.calculated_penalty_currency_id in (:currencyIds) or ac.penalty_claim_currency_id in (:currencyIds))
                                and (:claimedAmountFrom is null or ac.penalty_claim_amount >= :claimedAmountFrom)
                                and (:claimedAmountTo is null or ac.penalty_claim_amount <= :claimedAmountTo)
                                and ((:penaltyPayers) is null or text(ac.penalty_payer) in (:penaltyPayers))
                                and (
                                    :prompt is null or (
                                        :searchBy = 'ALL' and (
                                            text(ac.id) like :prompt
                                            or lower(c.identifier) like :prompt
                                            or (
                                                ac.product_contract_id is not null and exists(
                                                    select 1 from product_contract.contracts pc
                                                    where pc.id = ac.product_contract_id
                                                    and lower(pc.contract_number) like :prompt
                                                )
                                                or (
                                                ac.service_contract_id is not null and exists(
                                                    select 1 from service_contract.contracts sc
                                                    where sc.id = ac.service_contract_id
                                                    and lower(sc.contract_number) like :prompt
                                                )
                                                )
                                            )
                                            or lower(vap.name) like :prompt
                                            or lower(t.name) like :prompt
                                            or lower(p.name) like :prompt
                                            or lower(ac.additional_info) like :prompt
                                        )
                                    )
                                    or (
                                        (:searchBy = 'ID' and text(ac.id) like :prompt)
                                        or (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                        or (:searchBy = 'CONTRACT_NUMBER' and (
                                                ac.product_contract_id is not null and exists(
                                                    select 1 from product_contract.contracts pc
                                                    where pc.id = ac.product_contract_id
                                                    and lower(pc.contract_number) like :prompt
                                                )
                                                or (
                                                ac.service_contract_id is not null and exists(
                                                    select 1 from service_contract.contracts sc
                                                    where sc.id = ac.service_contract_id
                                                    and lower(sc.contract_number) like :prompt
                                                )
                                                )
                                            ))
                                        or (:searchBy = 'POD_IDENTIFIER' and lower(vap.name) like :prompt)
                                        or (:searchBy = 'PENALTY_NAME' and lower(p.name) like :prompt)
                                        or (:searchBy = 'TERMINATION_NAME' and lower(t.name) like :prompt)
                                        or (:searchBy = 'ADDITIONAL_INFO' and lower(ac.additional_info) like :prompt)
                                    )
                                )
                    """
    )
    Page<ActionListResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("statuses") List<String> statuses,
            @Param("actionStatuses") List<String> actionStatuses,
            @Param("actionTypeIds") List<Long> actionTypeIds,
            @Param("createDateFrom") LocalDate createDateFrom,
            @Param("createDateTo") LocalDate createDateTo,
            @Param("noticeReceivingDateFrom") LocalDate noticeReceivingDateFrom,
            @Param("noticeReceivingDateTo") LocalDate noticeReceivingDateTo,
            @Param("executionDateFrom") LocalDate executionDateFrom,
            @Param("executionDateTo") LocalDate executionDateTo,
            @Param("penaltyClaimed") String penaltyClaimed,
            @Param("calculatedPenaltyFrom") BigDecimal calculatedPenaltyFrom,
            @Param("calculatedPenaltyTo") BigDecimal calculatedPenaltyTo,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("claimedAmountFrom") BigDecimal claimedAmountFrom,
            @Param("claimedAmountTo") BigDecimal claimedAmountTo,
            @Param("penaltyPayers") List<String> penaltyPayers,
            @Param("actionPodsDirection") String actionPodsDirection,
            Pageable pageable
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.action.ActionResponse(
                        ac,
                        at.name,
                        case
                            when ac.productContractId is not null
                                then (
                                    select concat(pc.contractNumber, '/', to_char(pc.createDate, 'DD.MM.YY'))
                                    from ProductContract pc where pc.id = ac.productContractId
                                )
                            when ac.serviceContractId is not null
                                then (
                                    select concat(sc.contractNumber, '/', to_char(sc.createDate, 'DD.MM.YY'))
                                    from ServiceContracts sc where sc.id = ac.serviceContractId
                                )
                            end,
                        cu.name,
                        cu2.name,
                        p.name,
                        t.name,
                        c,
                        cd,
                        exists (
                            select 1 from ActionLiability al
                                where al.actionId = ac.id
                                and al.status = 'ACTIVE'
                        ),
                        lf.name
                    )
                    from Action ac
                    join ActionType at on ac.actionTypeId = at.id
                    join Customer c on ac.customerId = c.id
                    join CustomerDetails cd on cd.id = c.lastCustomerDetailId
                    left join Currency cu on cu.id = ac.calculatedPenaltyCurrencyId
                    left join Currency cu2 on cu2.id = ac.penaltyClaimCurrencyId
                    left join Penalty p on p.id = ac.penaltyId
                    left join Termination t on t.id = ac.terminationId
                    left join LegalForm lf on cd.legalFormId = lf.id
                        where ac.id = :id
                    """
    )
    ActionResponse getActionResponse(
            @Param("id") Long id
    );


    @Query(
            value = """
                    select ac.id from Action ac
                        where (:id is null or ac.id <> :id)
                        and ac.customerId = :customerId
                        and ac.serviceContractId = :serviceContractId
                        and ac.actionTypeId = :actionTypeId
                        and ac.noticeReceivingDate = :noticeReceivingDate
                        and ac.executionDate = :executionDate
                        and (:penaltyId is null or ac.penaltyId = :penaltyId)
                        and (:withoutPenalty is null or ac.withoutPenalty = :withoutPenalty)
                        and ac.penaltyPayer = :penaltyPayer
                        and ac.status = 'ACTIVE'
                    """
    )
    Long findActionWithSameParametersForServiceContract(
            @Param("id") Long id,
            @Param("customerId") Long customerId,
            @Param("serviceContractId") Long serviceContractId,
            @Param("actionTypeId") Long actionTypeId,
            @Param("noticeReceivingDate") LocalDate noticeReceivingDate,
            @Param("executionDate") LocalDate executionDate,
            @Param("penaltyId") Long penaltyId,
            @Param("withoutPenalty") Boolean withoutPenalty,
            @Param("penaltyPayer") ActionPenaltyPayer penaltyPayer
    );


    @Query(
            nativeQuery = true,
            value = """
                    select
                        ac.id as actionId,
                        (
                            select STRING_AGG(CAST(pod_id as varchar), ';') as pods
                            from action.action_pods ap
                                where action_id = ac.id and status = 'ACTIVE'
                                group by action_id
                        ) as pods
                    from action.actions ac
                        where (:id is null or ac.id <> :id)
                        and ac.customer_id = :customerId
                        and ac.product_contract_id = :productContractId
                        and ac.action_type_id = :actionTypeId
                        and ac.notice_receiving_date = :noticeReceivingDate
                        and ac.execution_date = :executionDate
                        and (:penaltyId is null or ac.penalty_id = :penaltyId)
                        and (:withoutPenalty is null or ac.without_penalty = :withoutPenalty)
                        and text(ac.penalty_payer) = :penaltyPayer
                        and ac.status = 'ACTIVE'
                    """
    )
    List<PodToActionMap> findActionsWithSameParametersForProductContract(
            @Param("id") Long id,
            @Param("customerId") Long customerId,
            @Param("productContractId") Long productContractId,
            @Param("actionTypeId") Long actionTypeId,
            @Param("noticeReceivingDate") LocalDate noticeReceivingDate,
            @Param("executionDate") LocalDate executionDate,
            @Param("penaltyId") Long penaltyId,
            @Param("withoutPenalty") Boolean withoutPenalty,
            @Param("penaltyPayer") String penaltyPayer
    );


    @Query(
            value = """
                    select ac from Action ac
                        where (:productContractId is null or ac.productContractId = :productContractId)
                        and (:serviceContractId is null or ac.serviceContractId = :serviceContractId)
                        and ac.actionTypeId = :actionTypeId
                        and ac.status in (:statuses)
                    order by ac.executionDate asc, ac.createDate asc
                    """
    )
    List<Action> findByContractAndActionType(
            @Param("productContractId") Long productContractId,
            @Param("serviceContractId") Long serviceContractId,
            @Param("actionTypeId") Long actionTypeId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            nativeQuery = true,
            value = """
                    select coalesce(max('false'),'true')
                        from product_contract.contracts c
                        join product_contract.contract_details cd on cd.contract_id = c.id
                        join product_contract.contract_pods cp on cp.contract_detail_id = cd.id
                        join pod.pod_details pd on cp.pod_detail_id =  pd.id
                        join pod.pod p on pd.pod_id = p.id
                            where c.status = 'ACTIVE'
                            and cp.status = 'ACTIVE'
                            and p.status  = 'ACTIVE'
                            and c.id = :productContractId
                            and p.id not in (
                                select ap.pod_id from action.actions a
                                    join action.action_pods ap on ap.action_id = a.id
                                        where a.product_contract_id = c.id
                                        and a.status = 'ACTIVE'
                                        and ap.status = 'ACTIVE'
                                        and a.action_type_id = :actionTypeId
                                union
                                select id from pod.pod p
                                where status = 'ACTIVE' and p.id in :podIds
                            )
                    """
    )
    boolean allContractPodsAreCoveredByActions(
            @Param("productContractId") Long productContractId,
            @Param("actionTypeId") Long actionTypeId,
            @Param("podIds") List<Long> podIds
    );


    @Query(
            value = """
                    select ac from Action ac
                        where ac.status = 'ACTIVE'
                        and ac.executionDate <= current_date
                        and ac.actionStatus = 'AWAITING'
                    """
    )
    List<Action> findEligibleActionsForStatusTransitionByExecutionDate();


    @Query(
            nativeQuery = true,
            value = """
                    with action_pods as (select string_agg(text(pod_id), ',') as pods,
                                                ap.action_id                  as action_id
                                         from action.action_pods ap
                                         where status = 'ACTIVE'
                                         group by ap.action_id),
                         penalty_action_types as (select string_agg(text(pat.action_type_id), ',') as action_types,
                                                         pat.penalty_id                            as penalty_id
                                                  from terms.penalty_action_types pat
                                                  where pat.status = 'ACTIVE'
                                                  group by pat.penalty_id)
                    select ac.id                            as actionId,
                           ac.action_type_id                as actionTypeId,
                           ap.pods                          as actionPods,
                           ac.product_contract_id           as productContractId,
                           ac.service_contract_id           as serviceContractId,
                           ac.execution_date                as executionDate,
                           ac.termination_id                as actionTerminationId,
                           text(ac.penalty_payer)           as actionPenaltyPayer,
                           ac.dont_allow_auto_penalty_claim as actionDontAllowAutoPenaltyClaim,
                           ac.penalty_claim_amount          as actionPenaltyClaimedAmount,
                           ac.penalty_claim_currency_id     as actionPenaltyClaimCurrency,
                           ac.claim_amount_manually_entered as actionClaimAmountManuallyEntered,
                           p.id                             as penaltyId,
                           text(p.applicability)            as penaltyApplicability,
                           pat.action_types                 as penaltyActionTypeId,
                           p.amount_calculation_formula     as penaltyFormula,
                           p.currency_id                    as penaltyCurrencyId,
                           p.min_amount                     as penaltyLowerLimit,
                           p.max_amount                     as penaltyUpperLimit,
                           p.automatic_submission           as penaltyAutomaticSubmission
                    from action.actions ac
                             join terms.penalties p on p.id = ac.penalty_id
                             left join action_pods ap on ac.id = ap.action_id
                             left join penalty_action_types pat on pat.penalty_id = p.id
                    where ac.status = 'ACTIVE'
                      and ac.execution_date <= date(current_date)
                      and ac.do_not_allow_auto_penalty_claim = false
                      and not exists (select 1
                                      from receivable.customer_liabilities al
                                      where al.action_id = ac.id
                                        and al.status = 'ACTIVE')
                    """,
            countQuery = """
                    select count(1)
                    from action.actions ac
                    join terms.penalties p on p.id = ac.penalty_id
                        where ac.status = 'ACTIVE'
                        and ac.execution_date <= date(current_date)
                        and ac.do_not_allow_auto_penalty_claim = false
                        and not exists (
                            select 1 from receivable.customer_liabilities al
                                where al.action_id = ac.id
                                and al.status = 'ACTIVE'
                        )
                    """
    )
    Page<ActionPenaltyCalculationResponse> findEligibleNonClaimedActionsForPenaltyCalculation(
            Pageable pageable
    );


    @Query(
            value = """
                    select a.id from Action a
                    join Penalty p on a.penaltyId = p.id and a.status = 'ACTIVE' and p.status = 'ACTIVE'
                        where a.productContractId = :contractId
                        and a.actionTypeId = :actionTypeId
                        and p.applicability = :penaltyApplicability
                        and exists (
                            select 1 from ActionLiability al
                                where al.actionId = a.id
                                and al.status = 'ACTIVE'
                        )
                        and (:actionIdToExclude is null or a.id <> :actionIdToExclude)
                    """
    )
    List<Long> findPenaltyClaimedActionsWithProductContractAndActionTypeAndPenaltyApplicability(
            @Param("contractId") Long contractId,
            @Param("actionTypeId") Long actionTypeId,
            @Param("penaltyApplicability") PenaltyApplicability penaltyApplicability,
            @Param("actionIdToExclude") Long actionIdToExclude
    );


    @Query(
            value = """
                    select a.id from Action a
                    join Penalty p on a.penaltyId = p.id and a.status = 'ACTIVE' and p.status = 'ACTIVE'
                        where a.serviceContractId = :contractId
                        and a.actionTypeId = :actionTypeId
                        and p.applicability = :penaltyApplicability
                        and exists (
                            select 1 from ActionLiability al
                                where al.actionId = a.id
                                and al.status = 'ACTIVE'
                        )
                        and (:actionIdToExclude is null or a.id <> :actionIdToExclude)
                    """
    )
    List<Long> findPenaltyClaimedActionsWithServiceContractAndActionTypeAndPenaltyApplicability(
            @Param("contractId") Long contractId,
            @Param("actionTypeId") Long actionTypeId,
            @Param("penaltyApplicability") PenaltyApplicability penaltyApplicability,
            @Param("actionIdToExclude") Long actionIdToExclude
    );


    @Query("""
            select new bg.energo.phoenix.model.CacheObject(ac.id,ac.additionalInfo)
            from Action ac
            where ac.productContractId=:contractId
            and ac.customerId=:customerId
            and ac.actionTypeId in (:typeIds)
            and ac.executionDate=:deactivationDate
            and ac.status='ACTIVE'
            """)
        //Todo this is removed for testing purposes should be added in future.
//    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObject> searchWithContractTermination(Long contractId, Long customerId, LocalDate deactivationDate, List<Long> typeIds);

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(ac.id,ac.additionalInfo)
            from Action ac
            join ActionPod acp on acp.actionId=ac.id
            where ac.productContractId=:contractId
            and ac.customerId=:customerId
            and ac.actionTypeId in (:typeIds)
            and ac.executionDate=:deactivationDate
            and ac.status='ACTIVE'
            and acp.status='ACTIVE'
            and acp.podId=:podDetailId
            """)
        //Todo this is removed for testing purposes should be added in future.
//    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObject> searchWithPod(Long contractId, Long customerId, Long podDetailId, LocalDate deactivationDate, List<Long> typeIds);

    @Query("""
            select new bg.energo.phoenix.model.request.contract.pod.ActionPenaltyModel(ac.id,ac.penaltyId,acp.podId)
            from Action ac
            join ActionPod acp on acp.actionId=ac.id
            where ac.productContractId=:contractId
            and ac.customerId=:customerId
            and ac.actionTypeId in (:typeIds)
            and ac.executionDate=:deactivationDate
            and ac.status='ACTIVE'
            and acp.status='ACTIVE'
            and acp.podId in :podIds
            """)
        //Todo this is removed for testing purposes should be added in future.
//    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<ActionPenaltyModel> searchWithAnyPod(Long contractId, Long customerId, Collection<Long> podIds, LocalDate deactivationDate, List<Long> typeIds);

    @Query("""
                select ac from Action ac
                 where ac.productContractId=:contractId
                 and ac.customerId=:customerId
                 and ac.actionTypeId in (:typeIds)
                 and ac.executionDate=:deactivationDate
                 and ac.status='ACTIVE'
                 and ac.penaltyId in (:penaltyIds)
                 and ac.actionStatus='AWAITING'
            """)
    List<Action> fetchActionsForCustomerAndPenalty(Long contractId, Long customerId, LocalDate deactivationDate, Collection<Long> penaltyIds, List<Long> typeIds);

    @Query("""
                select new bg.energo.phoenix.model.request.contract.pod.ActionPenaltyModel(a.id,a.penaltyId,ap.podId)
                from Action a
                join ActionPod ap on ap.actionId=a.id
                where a.id in (
                select a2.id
                from Action a2
                join ActionPod ap2 on ap2.actionId=a2.id
                where a2.productContractId=:contractId
                and a2.customerId=:customerId
                and a2.executionDate=:deactivationDate
                and a2.noticeReceivingDate=:deactivationDate
                and a2.penaltyId in (:penaltyIds)
                and  a2.actionTypeId in (:typeIds)
                and ap2.podId in (:podIds)
                and ap2.status='ACTIVE'
                )
                 and a.status='ACTIVE'
            """)
    List<ActionPenaltyModel> fetchActionsForCustomerAndPenaltyAndPod(Long contractId, Long customerId, LocalDate deactivationDate, Collection<Long> penaltyIds, List<Long> typeIds, Collection<Long> podIds);

    @Query(nativeQuery = true, value = """
            with cc_address_formatter as (select cc.id,
                                                 case
                                                     when cc.foreign_address = true then cc.populated_place_foreign
                                                     else pp.name end                                           as populated_place,
                                                 case
                                                     when cc.foreign_address = true then cc.zip_code_foreign
                                                     else zc.zip_code end                                       as zip_code,
                                                 case
                                                     when cc.foreign_address = true then cc.district_foreign
                                                     else distr.name end                                        as district,
                                                 case
                                                     when cc.foreign_address = true then
                                                         replace(text(cc.foreign_residential_area_type), '_', ' ')
                                                     else replace(text(cc.residential_area_type), '_', ' ') end as ra_type,
                                                 case
                                                     when cc.foreign_address = true then cc.residential_area_foreign
                                                     else ra.name end                                           as ra_name,
                                                 case
                                                     when cc.foreign_address = true then
                                                         cc.foreign_street_type
                                                     else cc.street_type end                                    as street_type,
                                                 case
                                                     when cc.foreign_address = true then cc.street_foreign
                                                     else str.name end                                          as street,
                                                 cc.street_number,
                                                 cc.block,
                                                 cc.entrance,
                                                 cc.floor,
                                                 cc.apartment,
                                                 cc.address_additional_info,
                                                 case
                                                     when cc.foreign_address = false then
                                                         concat_ws(', ',
                                                                   nullif(distr.name, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cc.residential_area_type), '_', ' '),
                                                                                    ra.name), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cc.street_type, str.name, cc.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cc.block), 'бл. '),
                                                                   nullif(concat('вх. ', cc.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cc.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cc.apartment), 'ап. '),
                                                                   cc.address_additional_info
                                                         )
                                                     else
                                                         concat_ws(', ',
                                                                   nullif(cc.district_foreign, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cc.foreign_residential_area_type), '_', ' '),
                                                                                    cc.residential_area_foreign), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cc.street_type, cc.street_foreign, cc.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cc.block), 'бл. '),
                                                                   nullif(concat('вх. ', cc.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cc.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cc.apartment), 'ап. '),
                                                                   cc.address_additional_info
                                                         )
                                                     end                                                        as formatted_address
                                          from customer.customer_communications cc
                                                   left join nomenclature.districts distr on cc.district_id = distr.id
                                                   left join nomenclature.zip_codes zc on cc.zip_code_id = zc.id
                                                   left join nomenclature.residential_areas ra on cc.residential_area_id = ra.id
                                                   left join nomenclature.streets str on cc.street_id = str.id
                                                   left join nomenclature.populated_places pp on cc.populated_place_id = pp.id),
                 product_contract as (select pc.id                                      as ct_id,
                                             pcd.id                                     as ct_d_id,
                                             pcd.customer_detail_id,
                                             pcd.customer_communication_id_for_contract as comm_id,
                                             pc.contract_number,
                                             cast(pc.create_date as date)               as contract_date,
                                             text(pcd.contract_type)                    as contract_type,
                                             pd.printing_name                           as product_name,
                                             pc.termination_date                        as termination_date,
                                             pc.termination_date + interval '1 day'     as termination_date_plus_one,
                                             pc.contract_term_end_date                  as contract_term_end_date
                                      from product_contract.contracts pc
                                               join product_contract.contract_details pcd
                                                    on pc.id = pcd.contract_id and pcd.start_date = (select icd.start_date
                                                                                                     from product_contract.contract_details icd
                                                                                                     where icd.contract_id = pc.id
                                                                                                       and icd.start_date <= current_date
                                                                                                     order by icd.start_date desc
                                                                                                     limit 1)
                                               join product.product_details pd on pcd.product_detail_id = pd.id),
                 service_contract as (select sc.id                                      as ct_id,
                                             scd.id                                     as detail_id,
                                             scd.customer_detail_id,
                                             scd.customer_communication_id_for_contract as comm_id,
                                             sc.contract_number,
                                             cast(sc.create_date as date)               as contract_date,
                                             text(scd.type)                             as contract_type,
                                             sd.printing_name                           as product_name,
                                             sc.termination_date                        as termination_date,
                                             sc.termination_date + interval '1 day'     as termination_date_plus_one,
                                             sc.contract_term_end_date                  as contract_term_end_date
                                      from service_contract.contracts sc
                                               join service_contract.contract_details scd
                                                    on sc.id = scd.contract_id and scd.start_date = (select icd.start_date
                                                                                                     from service_contract.contract_details icd
                                                                                                     where icd.contract_id = sc.id
                                                                                                       and icd.start_date <= current_date
                                                                                                     order by icd.start_date desc
                                                                                                     limit 1)
                                               join service.service_details sd on sd.id = scd.service_detail_id),
                 segment_info as (select cd.id                                                          as customer_detail_id,
                                         array_agg(distinct seg.name) filter (where seg.id is not null) as customer_segments
                                  from customer.customer_details cd
                                           left join customer.customer_segments cs
                                                     on cd.id = cs.customer_detail_id and cs.status = 'ACTIVE'
                                           join nomenclature.segments seg on cs.segment_id = seg.id
                                  group by cd.id),
                 cd_address_formatter as (select cd.id,
                                                 case
                                                     when cd.foreign_address = true then cd.populated_place_foreign
                                                     else pp.name end                                          as populated_place,
                                                 case
                                                     when cd.foreign_address = true then cd.zip_code_foreign
                                                     else zc.zip_code end                                      as zip_code,
                                                 case
                                                     when cd.foreign_address = true then cd.district_foreign
                                                     else distr.name end                                       as district,
                                                 case
                                                     when cd.foreign_address = true
                                                         then replace(text(cd.foreign_residential_area_type), '_', ' ')
                                                     else
                                                         replace(text(cd.residential_area_type), '_', ' ') end as ra_type,
                                                 case
                                                     when cd.foreign_address = true then cd.residential_area_foreign
                                                     else ra.name end                                          as ra_name,
                                                 case
                                                     when cd.foreign_address = true then cd.foreign_street_type
                                                     else cd.street_type end                                   as street_type,
                                                 case
                                                     when cd.foreign_address = true then cd.street_foreign
                                                     else str.name end                                         as street,
                                                 cd.street_number,
                                                 cd.block,
                                                 cd.entrance,
                                                 cd.floor,
                                                 cd.apartment,
                                                 cd.address_additional_info,
                                                 case
                                                     when cd.foreign_address = false then
                                                         concat_ws(', ',
                                                                   nullif(distr.name, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cd.residential_area_type), '_', ' '),
                                                                                    ra.name), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cd.street_type, str.name, cd.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cd.block), 'бл. '),
                                                                   nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cd.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                                   cd.address_additional_info
                                                         )
                                                     else
                                                         concat_ws(', ',
                                                                   nullif(cd.district_foreign, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cd.foreign_residential_area_type), '_', ' '),
                                                                                    cd.residential_area_foreign), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cd.foreign_street_type, cd.street_foreign,
                                                                                     cd.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cd.block), 'бл. '),
                                                                   nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cd.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                                   cd.address_additional_info
                                                         )
                                                     end                                                       as formatted_address
                                          from customer.customer_details cd
                                                   left join nomenclature.districts distr on cd.district_id = distr.id
                                                   left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
                                                   left join nomenclature.residential_areas ra on cd.residential_area_id = ra.id
                                                   left join nomenclature.streets str on cd.street_id = str.id
                                                   left join nomenclature.populated_places pp on cd.populated_place_id = pp.id),
                 customer_base as (select replace(text(c.customer_type), '_', ' '),
                                          cd.*,
                                          lf.name      as legal_form_name,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                                              else concat(cd.name, ' ', lf.name)
                                              end      as customer_name_comb,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ', cd.last_name_transl)
                                              else concat(cd.name_transl, ' ', lf.name)
                                              end      as customer_name_comb_trsl,
                                          c.identifier as customer_identifier,
                                          c.customer_number
                                   from customer.customer_details cd
                                            join customer.customers c on cd.customer_id = c.id
                                            left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id),
                 comm_contact_data as (select ccc.customer_communication_id      as commId,
                                              string_agg(text(ccc.id), ' ')      as contactIds,
                                              string_agg(ccc.contact_value, ';') as emails
                                       from customer.customer_communication_contacts ccc
                                       where ccc.status = 'ACTIVE'
                                         and ccc.contact_type = 'EMAIL'
                                       group by ccc.customer_communication_id)
            select cb.customer_name_comb                                                      as CustomerNameComb,
                   cb.customer_name_comb_trsl                                                 as CustomerNameCombTrsl,
                   cb.customer_identifier                                                     as CustomerIdentifier,
                   cb.customer_number                                                         as CustomerNumber,
                   translation.translate_text(customer.formatted_address,text('BULGARIAN'))                                                 as HeadquarterAddressComb,
                   customer.populated_place                                                   as HeadquarterPopulatedPlace,
                   customer.zip_code                                                          as HeadquarterZip,
                   customer.district                                                          as HeadquarterDistrict,
                   translation.translate_text(customer.ra_type ,text('BULGARIAN'))       as HeadquarterQuarterRaType,
                   customer.ra_name                                                           as HeadquarterQuarterRaName,
                   translation.translate_text(text(customer.street_type) ,text('BULGARIAN'))           as HeadquarterStrBlvdType,
                   customer.street                                                            as HeadquarterStrBlvdName,
                   customer.street_number                                                     as HeadquarterStrBlvdNumber,
                   customer.block                                                             as HeadquarterBlock,
                   customer.entrance                                                          as HeadquarterEntrance,
                   customer.floor                                                             as HeadquarterFloor,
                   customer.apartment                                                         as HeadquarterApartment,
                   customer.address_additional_info                                           as HeadquarterAdditionalInfo,
            
                   translation.translate_text(contr_cc.formatted_address ,text('BULGARIAN'))                                                as CommunicationAddressComb,
                   contr_cc.populated_place                                                   as CommunicationPopulatedPlace,
                   contr_cc.zip_code                                                          as CommunicationZip,
                   contr_cc.district                                                          as CommunicationDistrict,
                   translation.translate_text(contr_cc.ra_type,text('BULGARIAN'))                                                           as CommunicationQuarterRaType,
                   contr_cc.ra_name                                                           as CommunicationQuarterRaName,
                   translation.translate_text(text(contr_cc.street_type),text('BULGARIAN'))                                                       as CommunicationStrBlvdType,
                   contr_cc.street                                                            as CommunicationStrBlvdName,
                   contr_cc.street_number                                                     as CommunicationStrBlvdNumber,
                   contr_cc.block                                                             as CommunicationBlock,
                   contr_cc.entrance                                                          as CommunicationEntrance,
                   contr_cc.floor                                                             as CommunicationFloor,
                   contr_cc.apartment                                                         as CommunicationApartment,
                   contr_cc.address_additional_info                                           as CommunicationAdditionalInfo,
                   si.customer_segments                                                       as CustomerSegments,
                   coalesce(pc.contract_number, sc.contract_number)                           as ContractNumber,
                   coalesce(pc.contract_date, sc.contract_date)                               as ContractDate,
                   coalesce(pc.product_name, sc.product_name)                                 as ContractProductName,
                   coalesce(pc.contract_type, sc.contract_type)                               as ContractType,
                   coalesce(pc.termination_date, sc.termination_date)                         as ContractTerminationDate,
                   coalesce(pc.termination_date_plus_one, sc.termination_date_plus_one)       as ContractTerminationDatePlus1,
                   a.execution_date                                                           as ActionExecutionDate,
                   a.notice_receiving_date                                                    as ActionNoticeDate,
                   t.contract_clause_number                                                   as TerminationClauseNumber,
                   p.contract_clause_number                                                   as PenaltyClauseNumber,
                   a.penalty_claim_amount                                                     as PenaltyClaimAmount,
                   cur.name                                                                   as PenaltyClaimCurrency,
                   a.penalty_payer                                                            as PenaltyPayer,
                   nullif(
                           case
                               when t.notice_due_value_min is not null then concat('Value min: ', t.notice_due_value_min)
                               end
                               ||
                           case
                               when t.notice_due_value_min is not null and t.notice_due_value_max is not null then ', '
                               else ''
                               end
                               ||
                           case
                               when t.notice_due_value_max is not null then concat('Value max: ', t.notice_due_value_max)
                               end,
                           ''
                   )                                                                          as TerminationNoticePeriod,
                   translation.translate_text(text(t.notice_due_type ),text('BULGARIAN'))                                                         as TerminationNoticePeriodType,
                   cast((case
                             when t.auto_termination_from = 'EVENT_DATE' and t.event = 'EXPIRATION_OF_THE_NOTICE'
                                 then a.execution_date
                             when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE' and
                                  t.event = 'EXPIRATION_OF_THE_NOTICE' then
                                 date_trunc('month', a.execution_date) +
                                 interval '1 month' end) as date)                             as CalculatedTerminationDate,
                   cast((case
                             when t.auto_termination_from = 'EVENT_DATE' and t.event = 'EXPIRATION_OF_THE_NOTICE'
                                 then a.execution_date + interval '1 day'
                             when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE' and
                                  t.event = 'EXPIRATION_OF_THE_NOTICE' then
                                 date_trunc('month', a.execution_date) +
                                 interval '1 month' + interval '1 day' end) as date)          as CalculatedTerminationDatePlus1,
                   scd.id                                                                     as LastServiceContractDetailId,
                   pcd.id                                                                     as LastProductContractDetailId,
                   a.id                                                                       as ActionId,
                   cb.id                                                                      as CustomerDetailId,
                   pc.ct_d_id                                                                 as PcDetailId,
                   p.id                                                                       as PenaltyId,
                   a.create_date                                                              as CreateDate,
                   (case when t.event = 'EXPIRATION_OF_THE_NOTICE' then a.execution_date end) as PenaltyPaymentDueDate,
                   ccd.emails                                                                 as Emails,
                   ccd.commId                                                                 as CustomerCommunicationId
            from action.actions a
                     left join product_contract pc
                               on a.product_contract_id = pc.ct_id
                     left join product_contract.contract_details pcd
                               on pc.ct_id = pcd.contract_id and pcd.start_date = (select ipcd.start_date
                                                                                   from product_contract.contract_details ipcd
                                                                                            join customer.customer_details cusd on ipcd.customer_detail_id = cusd.id
                                                                                            join customer.customers cus
                                                                                                 on cusd.customer_id = cus.id and cus.id = a.customer_id
                                                                                   where ipcd.contract_id = pc.ct_id
                                                                                   order by ipcd.start_date desc
                                                                                   limit 1)
                     left join service_contract sc on a.service_contract_id = sc.ct_id
                     left join service_contract.contract_details scd
                               on sc.ct_id = scd.contract_id and scd.start_date = (select iscd.start_date
                                                                                   from service_contract.contract_details iscd
                                                                                            join customer.customer_details cusd on iscd.customer_detail_id = cusd.id
                                                                                            join customer.customers cus
                                                                                                 on cusd.customer_id = cus.id and cus.id = a.customer_id
                                                                                   where iscd.contract_id = sc.ct_id
                                                                                   order by iscd.start_date desc
                                                                                   limit 1)
                     join customer_base cb
                          on cb.id = coalesce(pc.customer_detail_id, sc.customer_detail_id) and cb.customer_id = a.customer_id
                     left join segment_info si on si.customer_detail_id = cb.id
                     left join cc_address_formatter contr_cc on contr_cc.id = coalesce(pc.comm_id, sc.comm_id)
                     left join cd_address_formatter customer on customer.id = cb.id
                     left join product.terminations t on a.termination_id = t.id
                     left join terms.penalties p on a.penalty_id = p.id
                     left join terms.penalty_payment_terms ppt on p.id = ppt.penalty_id and ppt.status = 'ACTIVE'
                     left join nomenclature.currencies cur on a.penalty_claim_currency_id = cur.id
                     left join comm_contact_data ccd on contr_cc.id = ccd.commId
            where a.id = :actionId
            """)
    PenaltyDocumentResponse fetchPenaltyDocResponseByActionId(Long actionId);

    @Query("""
            select ac from Action ac
            join ActionPod ap on ap.actionId=ac.id
            
            where ac.productContractId=:contractId
            and ap.podId=:podId
            and ac.status='ACTIVE'
            """)
    List<Action> findByPodAndContract(Long podId, Long contractId);
}
