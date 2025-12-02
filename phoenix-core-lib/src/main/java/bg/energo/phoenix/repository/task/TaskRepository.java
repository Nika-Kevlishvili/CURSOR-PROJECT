package bg.energo.phoenix.repository.task;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.entity.task.TaskStage;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.enums.task.TaskStatus;
import bg.energo.phoenix.model.response.task.TaskListingMiddleResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import org.springframework.data.domain.Page;
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
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query(value = "select nextval('task.tasks_id_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    @Query("""
            select t from Task t
            join TaskStage ts on ts.taskId = t.id
            where ts.completionDate is null
            and ts.endDate < :date
            and t.status = 'ACTIVE'
            and cast(t.taskStatus as string) not in('COMPLETED', 'TERMINATED', 'OVERDUE')
            """)
    List<Task> findAllOverdueTasks(@Param("date") LocalDate date);

    @Query("""
            select t from Task t
            where t.id = :id
            and t.status in (:statuses)
            """)
    Optional<Task> findByIdAndStatusIn(@Param("id") Long id, @Param("statuses") List<EntityStatus> statuses);

    List<Task> findByIdInAndStatusInAndConnectionType(Collection<Long> id, Collection<EntityStatus> status, TaskConnectionType connectionType);

    @Query(
            nativeQuery = true,
            value = """
                    
                    
                    select tbl.id                     as id,
                                           tbl.task_type              as type,
                                           CASE WHEN text(tbl.current_status) = 'COMPLETED' THEN '' ELSE tbl.current_performer_name END  as currentPerformer,
                                           tbl.performer              as performer,
                                           tbl.start_date             as startDate,
                                           tbl.end_date               as endDate,
                                           tbl.completion_date        as completionDate,
                                           tbl.connection_type        as connectionType,
                                           tbl.status                 as status,
                                           tbl.create_date            as createDate,
                                           tbl.current_status         as currentStatus
                                    from (
                                        select t.id,
                                        tt.name as task_type,
                                        (select performer from task.task_stages ts
                                            where ts.task_id = t.id and ts.completion_date is null
                                            order by stage limit 1) as current_performer,
                                        coalesce((select am.display_name from task.task_stages ts
                                            join customer.account_managers am on am.id = ts.performer
                                            where ts.task_id = t.id and ts.completion_date is null order by stage limit 1),
                                        (select am.display_name from task.task_stages ts
                                            join customer.account_managers am on am.id = ts.current_performer_id
                                            where ts.task_id = t.id order by stage desc limit 1)) as current_performer_name,
                                        (case
                                            when :performerDirection = 'ASC' then vtsp.performer
                                            when :performerDirection = 'DESC' then vtsp.performer_desc
                                            else vtsp.performer end) as performer,
                                        (select start_date from task.task_stages ts
                                            where ts.task_id = t.id order by stage limit 1) as start_date,
                                        coalesce ((select end_date from task.task_stages ts
                                            where ts.task_id = t.id and ts.completion_date is null order by stage limit 1),
                                        (select end_date from task.task_stages ts
                                            where ts.task_id = t.id order by ts.stage desc limit 1)) as end_date,
                                        (select completion_date from task.task_stages ts
                                            where ts.task_id = t.id order by stage desc limit 1) as completion_date,
                                        t.connection_type,
                                        t.status,
                                        t.create_date,
                                        t.current_status,
                                        t.task_type_id
                                            from task.tasks t
                                            join nomenclature.task_types tt on t.task_type_id = tt.id
                                            join task.vw_task_stage_performers vtsp on vtsp.task_id = t.id) as tbl
                                                where text(tbl.status) in :statuses
                                                and ((:taskStatuses) is null or text(tbl.current_status) in :taskStatuses)
                                                and (date(:startDateFrom) is null or tbl.start_date >= date(:startDateFrom))
                                                and (date(:startDateTo) is null or tbl.start_date <= date(:startDateTo))
                                                and (date(:endDateFrom) is null or tbl.end_date >= date(:endDateFrom))
                                                and (date(:endDateTo) is null or tbl.end_date <= date(:endDateTo))
                                                and ((:connectionTypes) is null or text(tbl.connection_type) in (:connectionTypes))
                                                and ((:currentPerformers) is null or tbl.current_performer in (:currentPerformers))
                                                and ((:taskTypes) is null or tbl.task_type_id in (:taskTypes))
                                                and ((:performers) is null or exists (
                                                    select 1 from task.task_stages ts
                                                    where ts.task_id = tbl.id and ts.performer in :performers)
                                                )
                                                and (:searchBy is null or (
                                                    (:searchBy = 'ALL'
                                                        and (
                                                            text(tbl.id) = replace(:prompt, '%', '')
                                                            or position(lower(replace(:prompt, '%', '')) in lower(tbl.task_type)) > 0
                                                            or (tbl.connection_type = 'CUSTOMER' and exists (
                                                                select 1 from customer.customers c
                                                                join customer.customer_tasks ct on ct.customer_id = c.id
                                                                    where ct.task_id = tbl.id
                                                                    and lower(c.identifier) like :prompt
                                                                    and c.status = 'ACTIVE'
                                                                    and ct.status = 'ACTIVE'
                                                                )
                                                            )
                                                            or exists(select 1 from billing.billings b
                                                                join billing.billing_tasks bt on bt.billing_id = b.id
                                                               where b.status <> 'DELETED'
                                                                 and bt.status = 'ACTIVE'
                                                                 and bt.task_id = tbl.id
                                                                 and lower(b.billing_Number) like :prompt
                                                                )                          \s
                                                            or (tbl.connection_type = 'CONTRACT_ORDER' and (
                                                                exists (select 1 from product_contract.contracts c
                                                                    join product_contract.contract_tasks ct on ct.contract_id = c.id
                                                                        where ct.task_id = tbl.id
                                                                        and lower(c.contract_number) like :prompt
                                                                        and c.status = 'ACTIVE'
                                                                        and ct.status = 'ACTIVE'
                                                                )
                                                                or exists (select 1 from service_contract.contracts c
                                                                    join service_contract.contract_tasks ct on ct.contract_id = c.id
                                                                        where ct.task_id = tbl.id
                                                                        and lower(c.contract_number) like :prompt
                                                                        and c.status = 'ACTIVE'
                                                                        and ct.status = 'ACTIVE'
                                                                )
                                                                or exists(select 1 from service_order.orders o
                                                                    join service_order.order_tasks ot on ot.order_id = o.id
                                                                        where o.status = 'ACTIVE'
                                                                        and ot.status = 'ACTIVE'
                                                                        and ot.task_id = tbl.id
                                                                        and lower(o.order_Number) like :prompt
                                                                )
                                                                or exists(select 1 from goods_order.orders o
                                                                    join goods_order.order_tasks ot on ot.order_id = o.id
                                                                        where o.status = 'ACTIVE'
                                                                        and ot.status = 'ACTIVE'
                                                                        and ot.task_id = tbl.id
                                                                        and lower(o.order_Number) like :prompt
                                                                )
                                                                )
                                                            )
                                                        )
                                                    )
                                                    or (
                                                        (:searchBy = 'ID' and text(tbl.id) = replace(:prompt, '%', ''))
                                                        or (:searchBy = 'TASK_TYPE' and position(lower(replace(:prompt, '%', '')) in lower(tbl.task_type)) > 0)
                                                        or (:searchBy = 'CUSTOMER_IDENTIFIER'
                                                            and tbl.connection_type = 'CUSTOMER' and exists (
                                                                select 1 from customer.customers c
                                                                join customer.customer_tasks ct on ct.customer_id = c.id
                                                                    where ct.task_id = tbl.id
                                                                    and lower(c.identifier) like :prompt
                                                                    and c.status = 'ACTIVE'
                                                                    and ct.status = 'ACTIVE'
                                                            )
                                                        )
                                                       or (:searchBy = 'BILLING_NUMBER' and exists(select 1 from billing.billings b
                                                                  join billing.billing_tasks bt on bt.billing_id = b.id
                                                                 where b.status <> 'DELETED'
                                                                   and bt.status = 'ACTIVE'
                                                                   and bt.task_id = tbl.id
                                                                   and lower(b.billing_Number) like :prompt
                                                                )
                                                                )                \s
                                                        or (:searchBy = 'CONTRACT_NUMBER'
                                                            and tbl.connection_type = 'CONTRACT_ORDER' and
                                                             (exists (
                                                                select 1 from product_contract.contracts c
                                                                join product_contract.contract_tasks ct on ct.contract_id = c.id
                                                                    where ct.task_id = tbl.id
                                                                    and lower(c.contract_number) like :prompt
                                                                    and c.status = 'ACTIVE'
                                                                    and ct.status = 'ACTIVE'
                                                            )
                                                            or exists(
                                                                select 1 from service_contract.contracts c
                                                                join service_contract.contract_tasks ct on ct.contract_id = c.id
                                                                    where ct.task_id = tbl.id
                                                                    and lower(c.contract_number) like :prompt
                                                                    and c.status = 'ACTIVE'
                                                                    and ct.status = 'ACTIVE'
                                                            )
                                                            )
                                                        )
                                                        or (:searchBy = 'ORDER_NUMBER'
                                                            and tbl.connection_type = 'CONTRACT_ORDER' and
                                                             (exists (select 1 from service_order.orders o
                                                                join service_order.order_tasks ot on ot.order_id = o.id
                                                                    where o.status = 'ACTIVE'
                                                                    and ot.status = 'ACTIVE'
                                                                    and ot.task_id = tbl.id
                                                                    and lower(o.order_number) like :prompt
                                                            )
                                                            or exists(select 1 from goods_order.orders o
                                                                join goods_order.order_tasks ot on ot.order_id = o.id
                                                                    where o.status = 'ACTIVE'
                                                                    and ot.status = 'ACTIVE'
                                                                    and ot.task_id = tbl.id
                                                                    and lower(o.order_number) like :prompt
                                                            )
                                                            )
                                                        )
                                                    )
                                                )
                                          )
                                    order by :sortBy
                    """,
            countQuery = """
                              select count(tbl.id)
                                       from (
                                           select t.id,
                                           tt.name as task_type,
                                           (select performer from task.task_stages ts
                                               where ts.task_id = t.id and ts.completion_date is null
                                               order by stage limit 1) as current_performer,
                                           coalesce((select am.display_name from task.task_stages ts
                                               join customer.account_managers am on am.id = ts.performer
                                               where ts.task_id = t.id and ts.completion_date is null order by stage limit 1),
                                           (select am.display_name from task.task_stages ts
                                               join customer.account_managers am on am.id = ts.performer
                                               where ts.task_id = t.id order by stage desc limit 1)) as current_performer_name,
                                           (case
                                               when :performerDirection = 'ASC' then vtsp.performer
                                               when :performerDirection = 'DESC' then vtsp.performer_desc
                                               else vtsp.performer end) as performer,
                                           (select start_date from task.task_stages ts
                                               where ts.task_id = t.id order by stage limit 1) as start_date,
                                           coalesce ((select end_date from task.task_stages ts
                                               where ts.task_id = t.id and ts.completion_date is null order by stage limit 1),
                                           (select end_date from task.task_stages ts
                                               where ts.task_id = t.id order by ts.stage desc limit 1)) as end_date,
                                           (select completion_date from task.task_stages ts
                                               where ts.task_id = t.id order by stage desc limit 1) as completion_date,
                                           t.connection_type,
                                           t.status,
                                           t.create_date,
                                           t.current_status,
                                           t.task_type_id
                                               from task.tasks t
                                               join nomenclature.task_types tt on t.task_type_id = tt.id
                                               join task.vw_task_stage_performers vtsp on vtsp.task_id = t.id) as tbl
                                                   where text(tbl.status) in :statuses
                                                   and ((:taskStatuses) is null or text(tbl.current_status) in :taskStatuses)
                                                   and (date(:startDateFrom) is null or tbl.start_date >= date(:startDateFrom))
                                                   and (date(:startDateTo) is null or tbl.start_date <= date(:startDateTo))
                                                   and (date(:endDateFrom) is null or tbl.end_date >= date(:endDateFrom))
                                                   and (date(:endDateTo) is null or tbl.end_date <= date(:endDateTo))
                                                   and ((:connectionTypes) is null or text(tbl.connection_type) in (:connectionTypes))
                                                   and ((:currentPerformers) is null or tbl.current_performer in (:currentPerformers))
                                                   and ((:taskTypes) is null or tbl.task_type_id in (:taskTypes))
                                                   and ((:performers) is null or exists (
                                                       select 1 from task.task_stages ts
                                                       where ts.task_id = tbl.id and ts.performer in :performers)
                                                   )
                                                   and (:searchBy is null or (
                                                       (:searchBy = 'ALL'
                                                           and (
                                                               text(tbl.id) = replace(:prompt, '%', '')
                                                               or position(lower(replace(:prompt, '%', '')) in lower(tbl.task_type)) > 0
                                                               or (tbl.connection_type = 'CUSTOMER' and exists (
                                                                   select 1 from customer.customers c
                                                                   join customer.customer_tasks ct on ct.customer_id = c.id
                                                                       where ct.task_id = tbl.id
                                                                       and lower(c.identifier) like :prompt
                                                                       and c.status = 'ACTIVE'
                                                                       and ct.status = 'ACTIVE'
                                                                   )
                                                               )
                                                               or exists(select 1 from billing.billings b
                                                                   join billing.billing_tasks bt on bt.billing_id = b.id
                                                                  where b.status <> 'DELETED'
                                                                    and bt.status = 'ACTIVE'
                                                                    and bt.task_id = tbl.id
                                                                    and lower(b.billing_Number) like :prompt
                                                                   )                          \s
                                                               or (tbl.connection_type = 'CONTRACT_ORDER' and (
                                                                   exists (select 1 from product_contract.contracts c
                                                                       join product_contract.contract_tasks ct on ct.contract_id = c.id
                                                                           where ct.task_id = tbl.id
                                                                           and lower(c.contract_number) like :prompt
                                                                           and c.status = 'ACTIVE'
                                                                           and ct.status = 'ACTIVE'
                                                                   )
                                                                   or exists (select 1 from service_contract.contracts c
                                                                       join service_contract.contract_tasks ct on ct.contract_id = c.id
                                                                           where ct.task_id = tbl.id
                                                                           and lower(c.contract_number) like :prompt
                                                                           and c.status = 'ACTIVE'
                                                                           and ct.status = 'ACTIVE'
                                                                   )
                                                                   or exists(select 1 from service_order.orders o
                                                                       join service_order.order_tasks ot on ot.order_id = o.id
                                                                           where o.status = 'ACTIVE'
                                                                           and ot.status = 'ACTIVE'
                                                                           and ot.task_id = tbl.id
                                                                           and lower(o.order_Number) like :prompt
                                                                   )
                                                                   or exists(select 1 from goods_order.orders o
                                                                       join goods_order.order_tasks ot on ot.order_id = o.id
                                                                           where o.status = 'ACTIVE'
                                                                           and ot.status = 'ACTIVE'
                                                                           and ot.task_id = tbl.id
                                                                           and lower(o.order_Number) like :prompt
                                                                   )
                                                                   )
                                                               )
                                                           )
                                                       )
                                                       or (
                                                           (:searchBy = 'ID' and text(tbl.id) = replace(:prompt, '%', ''))
                                                           or (:searchBy = 'TASK_TYPE' and position(lower(replace(:prompt, '%', '')) in lower(tbl.task_type)) > 0)
                                                           or (:searchBy = 'CUSTOMER_IDENTIFIER'
                                                               and tbl.connection_type = 'CUSTOMER' and exists (
                                                                   select 1 from customer.customers c
                                                                   join customer.customer_tasks ct on ct.customer_id = c.id
                                                                       where ct.task_id = tbl.id
                                                                       and lower(c.identifier) like :prompt
                                                                       and c.status = 'ACTIVE'
                                                                       and ct.status = 'ACTIVE'
                                                               )
                                                           )
                                                          or (:searchBy = 'BILLING_NUMBER' and exists(select 1 from billing.billings b
                                                                     join billing.billing_tasks bt on bt.billing_id = b.id
                                                                    where b.status <> 'DELETED'
                                                                      and bt.status = 'ACTIVE'
                                                                      and bt.task_id = tbl.id
                                                                      and lower(b.billing_Number) like :prompt
                                                                   )
                                                                   )                \s
                                                           or (:searchBy = 'CONTRACT_NUMBER'
                                                               and tbl.connection_type = 'CONTRACT_ORDER' and
                                                                (exists (
                                                                   select 1 from product_contract.contracts c
                                                                   join product_contract.contract_tasks ct on ct.contract_id = c.id
                                                                       where ct.task_id = tbl.id
                                                                       and lower(c.contract_number) like :prompt
                                                                       and c.status = 'ACTIVE'
                                                                       and ct.status = 'ACTIVE'
                                                               )
                                                               or exists(
                                                                   select 1 from service_contract.contracts c
                                                                   join service_contract.contract_tasks ct on ct.contract_id = c.id
                                                                       where ct.task_id = tbl.id
                                                                       and lower(c.contract_number) like :prompt
                                                                       and c.status = 'ACTIVE'
                                                                       and ct.status = 'ACTIVE'
                                                               )
                                                               )
                                                           )
                                                           or (:searchBy = 'ORDER_NUMBER'
                                                               and tbl.connection_type = 'CONTRACT_ORDER' and
                                                                (exists (select 1 from service_order.orders o
                                                                   join service_order.order_tasks ot on ot.order_id = o.id
                                                                       where o.status = 'ACTIVE'
                                                                       and ot.status = 'ACTIVE'
                                                                       and ot.task_id = tbl.id
                                                                       and lower(o.order_number) like :prompt
                                                               )
                                                               or exists(select 1 from goods_order.orders o
                                                                   join goods_order.order_tasks ot on ot.order_id = o.id
                                                                       where o.status = 'ACTIVE'
                                                                       and ot.status = 'ACTIVE'
                                                                       and ot.task_id = tbl.id
                                                                       and lower(o.order_number) like :prompt
                                                               )
                                                               )
                                                           )
                                                       )
                                                   )
                                             )
                    """
    )
    Page<TaskListingMiddleResponse> filter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("performerDirection") String performerDirection,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            @Param("endDateFrom") LocalDate endDateFrom,
            @Param("endDateTo") LocalDate endDateTo,
            @Param("performers") List<Long> performers,
            @Param("connectionTypes") List<String> connectionTypes,
            @Param("currentPerformers") List<Long> currentPerformers,
            @Param("taskTypes") List<Long> taskTypes,
            @Param("statuses") List<String> statuses,
            @Param("taskStatuses") List<String> taskStatuses,
            @Param("sortBy") String sortBy,
            Pageable pageable
    );


    @Query(
            value = """
                    select count(t.id) > 0 from Task t
                    left join TaskActivity ta on ta.taskId = t.id
                    join SystemActivity sa on sa.id = ta.systemActivityId
                        where t.id = :id
                        and t.status = 'ACTIVE'
                        and ta.status = 'ACTIVE'
                        and sa.status = 'ACTIVE'
                    """
    )
    boolean hasConnectionToActivity(Long id);

    @Query("""
                     select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join ProductContractTask pct on t.id = pct.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and pct.status = 'ACTIVE'
                     and pct.contractId = :contractId
                     order by t.createDate
            """
    )
    List<TaskShortResponse> findProductContractActiveTasks(Long contractId);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
            join ServiceContractTask sct on sct.taskId = t.id
            join TaskType tt on t.taskTypeId = tt.id
            where t.status = 'ACTIVE'
            and sct.status = 'ACTIVE'
            and sct.contractId = :contractId
            order by t.createDate
            """)
    List<TaskShortResponse> findServiceContractActiveTasks(Long contractId);

    @Query("""
                     select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join CustomerTask ct on t.id = ct.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and ct.status = 'ACTIVE'
                     and ct.customerId = :customerId
                     order by t.createDate
            """)
    List<TaskShortResponse> findCustomerActiveTasks(Long customerId);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join GoodsOrderTask got on t.id = got.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and got.status = 'ACTIVE'
                     and got.orderId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findGoodsOrderActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join ServiceOrderTask sor on t.id = sor.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and sor.status = 'ACTIVE'
                     and sor.orderId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findServiceOrderActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join BillingRunTasks brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.billingId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findBillingRunActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join ReceivableBlockingTask brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.receivableBlockingId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findReceivableBlockingActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join CustomerAssessmentTasks cat on t.id = cat.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and cat.status = 'ACTIVE'
                     and cat.customerAssessmentId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findCustomerAssessmentActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join ReschedulingTasks rt on t.id = rt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and rt.status = 'ACTIVE'
                     and rt.reschedulingId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findReschedulingActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join PowerSupplyDisconnectionReminderTasks rt on t.id = rt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and rt.status = 'ACTIVE'
                     and rt.reminderId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findPowerSupplyDisconnectionReminderActiveTasks(Long id);

    @Query("""
                    select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                                 join ReconnectionOfThePowerSupplyTasks rt on t.id = rt.taskId
                                 join TaskType tt on t.taskTypeId = tt.id
                                 where t.status = 'ACTIVE'
                                 and rt.status = 'ACTIVE'
                                 and rt.reconnectionId = :id
                                 order by t.createDate
            """)
    List<TaskShortResponse> findReconnectionOfThePowerSupplyActiveTasks(Long id);

    @Query("""
                    select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                                 join PowerSupplyDcnCancellationTask rt on t.id = rt.taskId
                                 join TaskType tt on t.taskTypeId = tt.id
                                 where t.status = 'ACTIVE'
                                 and rt.status = 'ACTIVE'
                                 and rt.powerSupplyDcnCancellationId = :id
                                 order by t.createDate
            """)
    List<TaskShortResponse> findCancellationOfThePowerSupplyActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.withdrawalChangeOfCbgId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join PowerSupplyDcnCancellationTask brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.powerSupplyDcnCancellationId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findCancellationActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join DisconnectionPowerSupplyTask brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.powerSupplyDisconnectionId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findDisconnectionOfPowerSupplyActiveTasks(Long id);

    boolean existsByIdAndStatus(Long id, EntityStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join ObjectionToChangeOfCbgTasks objTask on t.id = objTask.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and objTask.status = 'ACTIVE'
                     and objTask.changeOfCbgId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findObjectionToChangeOfCbgActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join EmailCommunicationTask brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.emailCommunicationId = :id
                     order by t.createDate
            """
    )
    List<TaskShortResponse> findEmailCommunicationActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join LatePaymentFineTask brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.latePaymentFineId = :id
                     order by t.createDate
            """
    )
    List<TaskShortResponse> findLatePaymentFineActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join SmsCommunicationTasks sct on t.id = sct.taskId
                     join SmsCommunication sc on sct.smsCommunicationId=sc.id
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and sct.status = 'ACTIVE'
                     and sct.smsCommunicationId = :id
                     and sc.communicationChannel = :smsCommunicationChannel
                     order by t.createDate
            """
    )
    List<TaskShortResponse> findSmsCommunicationActiveTasks(Long id, SmsCommunicationChannel smsCommunicationChannel);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join SmsCommunicationTasks sct on t.id = sct.taskId
                     join SmsCommunication sc on sct.smsCommunicationId=sc.id
                     join SmsCommunicationCustomers scc on scc.smsCommunicationId=sc.id
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and sct.status = 'ACTIVE'
                     and scc.id = :id
                     and sc.communicationChannel = 'SMS'
                     order by t.createDate
            """)
    List<TaskShortResponse> findSmsCommunicationActiveTasksSingleSms(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join DisconnectionPowerSupplyRequestsTasks brt on t.id = brt.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and brt.status = 'ACTIVE'
                     and brt.powerSupplyDisconnectionRequestId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findDisconnectionPowerSupplyRequestActiveTasks(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskShortResponse(t, tt) from Task t
                     join DisconnectionPowerSupplyTask dpst on t.id = dpst.taskId
                     join TaskType tt on t.taskTypeId = tt.id
                     where t.status = 'ACTIVE'
                     and dpst.status = 'ACTIVE'
                     and dpst.powerSupplyDisconnectionId = :id
                     order by t.createDate
            """)
    List<TaskShortResponse> findDisconnectionPowerSupplyActiveTasks(Long id);

    @Query("""
            select ts from TaskStage ts
            join Task t on t.id=ts.taskId
            where ts.endDate=:now
            and cast(ts.taskStageStatus as string) not in('COMPLETED', 'OVERDUE')
            and cast(t.taskStatus as string) not in  ('TERMINATED','COMPLETED','OVERDUE')
            """)
    List<TaskStage> findAllExpiredTasks(LocalDate now);

    @Query("""
            select t from Task t
            where t.taskStatus=:taskStatus
            """)
    List<Task> findAllTaskByStatus(TaskStatus taskStatus);
}