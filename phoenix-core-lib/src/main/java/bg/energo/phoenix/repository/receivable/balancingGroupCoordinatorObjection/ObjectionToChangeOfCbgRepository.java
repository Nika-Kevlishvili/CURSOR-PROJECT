package bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbg;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgListingMiddleResponseForWithdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectionToChangeOfCbgRepository extends JpaRepository<ObjectionToChangeOfCbg, Long> {

    Optional<ObjectionToChangeOfCbg> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);

    Optional<ObjectionToChangeOfCbg> findByIdAndChangeOfCbgStatusInAndStatusIn(Long id, List<ChangeOfCbgStatus> changeOfCbgStatuses, List<EntityStatus> statuses);

    @Query(
            nativeQuery = true,
            value =
                    """
                                    select number,
                                    changeDate,
                                    gridOperator,
                                    changeStatus,
                                    numberOfPods,
                                    createDate,
                                    entityStatus,
                                    id
                                    from   
                                    (select distinct
                                        otcoc.change_of_cbg_number as number,
                                        otcoc.change_date as changeDate,
                                        go2.name AS gridOperator,
                                        otcoc.change_of_cbg_status changeStatus,
                                        count(distinct otcocpr.pod_id) AS numberOfPods,
                                        date(otcoc.create_date) AS createDate,
                                        otcoc.status as entityStatus,
                                        otcoc.id
                                    from
                                        receivable.objection_to_change_of_cbg otcoc
                                            join
                                        nomenclature.grid_operators go2 on otcoc.grid_operator_id = go2.id
                                            left join
                                        receivable.objection_to_change_of_cbg_process_results otcocpr on otcocpr.change_of_cbg_id = otcoc.id
                                            left join
                                        customer.customers c on otcocpr.customer_id = c.id
                                            left join
                                        pod.pod p on otcocpr.pod_id = p.id
                                    where
                                        ((:gridOperatorIds) is null or otcoc.grid_operator_id in :gridOperatorIds)
                                      and ((:status) is null or text(otcoc.change_of_cbg_status) in :status)
                                      and ((:entityStatus) is null or text(otcoc.status) in :entityStatus)
                                      and (cast(:createDateFrom as date) is null or date(otcoc.create_date) >= cast(:createDateFrom as date))
                                      and (cast(:createDateTo as date) is null or date(otcoc.create_date) <= cast(:createDateTo as date))
                                      and (date(:changeDateFrom) is null or date(otcoc.change_date) >= :changeDateFrom)
                                      and (date(:changeDateTo) is null or date(otcoc.change_date) <= :changeDateTo)
                                      and (:prompt is null or (text(:searchBy) = 'ALL' and (
                                        lower(text(otcoc.change_of_cbg_number)) like :prompt
                                            or
                                        lower(text(c.identifier)) like :prompt
                                            or
                                        lower(text(p.identifier)) like :prompt
                                        ))
                                        or (
                                               (text(:searchBy) = 'CHANGE_OF_CBG_NUMBER' and lower(text(otcoc.change_of_cbg_number)) like :prompt)
                                                   or
                                               (text(:searchBy) = 'POD_IDENTIFIER' and lower(text(P.identifier)) like :prompt)
                                                   or
                                               (text(:searchBy) = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                               ))
                                    group by
                                        otcoc.change_of_cbg_number,
                                        otcoc.change_date,
                                        go2.name,
                                        otcoc.change_of_cbg_status,
                                        date(otcoc.create_date),
                                        otcoc.id
                                    having
                                        (coalesce(:numberOfPodsFrom,'0') = '0' or count(distinct otcocpr.pod_id) >= :numberOfPodsFrom)
                                       and (coalesce(:numberOfPodsTo,'0') = '0' or count(distinct otcocpr.pod_id) <= :numberOfPodsTo) ) as tbl
                            """,


            countQuery = """
                            select count(tbl.id)
                            from   
                            (select distinct
                                otcoc.change_of_cbg_number as number,
                                otcoc.change_date as changeDate,
                                go2.name AS gridOperator,
                                otcoc.change_of_cbg_status changeStatus,
                                count(distinct otcocpr.pod_id) AS numberOfPods,
                                date(otcoc.create_date) AS createDate,
                                otcoc.status as entityStatus,
                                otcoc.id
                            from
                                receivable.objection_to_change_of_cbg otcoc
                                    join
                                nomenclature.grid_operators go2 on otcoc.grid_operator_id = go2.id
                                    left join
                                receivable.objection_to_change_of_cbg_process_results otcocpr on otcocpr.change_of_cbg_id = otcoc.id
                                    left join
                                customer.customers c on otcocpr.customer_id = c.id
                                    left join
                                pod.pod p on otcocpr.pod_id = p.id
                            where
                                ((:gridOperatorIds) is null or otcoc.grid_operator_id in :gridOperatorIds)
                              and ((:status) is null or text(otcoc.change_of_cbg_status) in :status)
                              and ((:entityStatus) is null or text(otcoc.status) in :entityStatus)
                              and (cast(:createDateFrom as date) is null or date(otcoc.create_date) >= cast(:createDateFrom as date))
                              and (cast(:createDateTo as date) is null or date(otcoc.create_date) <= cast(:createDateTo as date))
                              and (date(:changeDateFrom) is null or date(otcoc.change_date) >= :changeDateFrom)
                              and (date(:changeDateTo) is null or date(otcoc.change_date) <= :changeDateTo)
                              and (:prompt is null or (text(:searchBy) = 'ALL' and (
                                lower(text(otcoc.change_of_cbg_number)) like :prompt
                                    or
                                lower(text(c.identifier)) like :prompt
                                    or
                                lower(text(p.identifier)) like :prompt
                                ))
                                or (
                                       (text(:searchBy) = 'CHANGE_OF_CBG_NUMBER' and lower(text(otcoc.change_of_cbg_number)) like :prompt)
                                           or
                                       (text(:searchBy) = 'POD_IDENTIFIER' and lower(text(P.identifier)) like :prompt)
                                           or
                                       (text(:searchBy) = 'CUSTOMER_IDENTIFIER' and lower(text(c.identifier)) like :prompt)
                                       ))
                            group by
                                otcoc.change_of_cbg_number,
                                otcoc.change_date,
                                go2.name,
                                otcoc.change_of_cbg_status,
                                date(otcoc.create_date),
                                otcoc.id
                            having
                                (coalesce(:numberOfPodsFrom,'0') = '0' or count(distinct otcocpr.pod_id) >= :numberOfPodsFrom)
                               and (coalesce(:numberOfPodsTo,'0') = '0' or count(distinct otcocpr.pod_id) <= :numberOfPodsTo) ) as tbl
                    """
    )
    Page<ObjectionToChangeOfCbgListingMiddleResponse> filter(
            @Param("status") List<String> status,
            @Param("gridOperatorIds") List<Long> gridOperatorIds,
            @Param("createDateFrom") LocalDateTime createDateFrom,
            @Param("createDateTo") LocalDateTime createDateTo,
            @Param("changeDateFrom") LocalDate changeDateFrom,
            @Param("changeDateTo") LocalDate changeDateTo,
            @Param("entityStatus") List<String> entityStatus,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("numberOfPodsFrom") Integer numberOfPodsFrom,
            @Param("numberOfPodsTo") Integer numberOfPodsTo,
            Pageable pageable
    );


    @Query(
    nativeQuery = true,
    value = """
        select
                otcoc.change_of_cbg_number as number,
                otcoc.change_of_cbg_status changeStatus,
                otcoc.id 
            from
                receivable.objection_to_change_of_cbg otcoc
            where
              exists (
                select 1
                from receivable.objection_to_change_of_cbg_process_results t1
                where t1.change_of_cbg_id = otcoc.id
                  and not exists (
                      select 1
                      from receivable.objection_withdrawal_to_change_of_cbg_process_results t2
                       join receivable.objection_withdrawal_to_change_of_cbg owtcoc 
                       on t2.change_withdrawal_of_cbg_id = owtcoc.id
                      where t2.pod_id = t1.pod_id
                        and t2.is_checked = true
                        and owtcoc.withdrawal_change_of_cbg_status = 'SEND'
                  )
                     )
              and
                ((:status) is null or text(otcoc.change_of_cbg_status) in (:status))
              and ((:entityStatus) is null or text(otcoc.status) in (:entityStatus))
              and (:prompt is null or (text(:searchBy) = 'ALL' and (
                lower(text(otcoc.change_of_cbg_number)) like :prompt
                ))
                or ( (text(:searchBy) = 'CHANGE_OF_CBG_NUMBER' and lower(text(otcoc.change_of_cbg_number)) like :prompt)
                       ))
    """,
    countQuery = """
        select
            count(*)
        from
            receivable.objection_to_change_of_cbg otcoc
        where
          exists (
            select 1
            from receivable.objection_to_change_of_cbg_process_results t1
            where t1.change_of_cbg_id = otcoc.id
              and not exists (
                  select 1
                  from receivable.objection_withdrawal_to_change_of_cbg_process_results t2
                   join receivable.objection_withdrawal_to_change_of_cbg owtcoc 
                   on t2.change_withdrawal_of_cbg_id = owtcoc.id
                  where t2.pod_id = t1.pod_id
                    and t2.is_checked = true
                    and owtcoc.withdrawal_change_of_cbg_status = 'SEND'
              )
                 )
          and
            ((:status) is null or text(otcoc.change_of_cbg_status) in (:status))
          and ((:entityStatus) is null or text(otcoc.status) in (:entityStatus))
          and (:prompt is null or (text(:searchBy) = 'ALL' and (
            lower(text(otcoc.change_of_cbg_number)) like :prompt
            ))
            or ( (text(:searchBy) = 'CHANGE_OF_CBG_NUMBER' and lower(text(otcoc.change_of_cbg_number)) like :prompt)
                   ))
    """
    )
    Page<ObjectionToChangeOfCbgListingMiddleResponseForWithdrawal> findByStatusAndChangeOfCbgNumber(
            @Param("status") List<String> status,
            @Param("entityStatus") List<String> entityStatus,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    @Query("""
            select ctmp
            from ObjectionToChangeOfCbgTemplates temp
            join ContractTemplate ctmp on temp.templateId=ctmp.id
            where temp.objectionToChangeId = :objId
            and temp.status = 'ACTIVE'
     """)
    List<ContractTemplate> findRelatedContractTemplates(@Param("objId") Long objId);

}
