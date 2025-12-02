package bg.energo.phoenix.repository.activity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.activity.SystemActivity;
import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import bg.energo.phoenix.model.response.activity.SystemActivityListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemActivityRepository extends JpaRepository<SystemActivity, Long> {

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.SystemActivityListResponse(
                               a.id,
                               a2.name,
                               sa.name,
                               case when text(a.connectionType) in ('PRODUCT_CONTRACT', 'SERVICE_CONTRACT') then 'CONTRACT'
                                    when text(a.connectionType) in ('GOODS_ORDER', 'SERVICE_ORDER') then 'ORDER'
                                    else text(a.connectionType) end as connectionType,
                               a.createDate,
                               a.status
                        )
                        from SystemActivity a
                        join Activity a2 on a.activityId = a2.id
                        left join SubActivity sa on a.subActivityId = sa.id
                            where a.status in :status
                            and (coalesce(:subActivityIds, '0') = '0' or a.subActivityId in :subActivityIds)
                            and (coalesce(:connectionTypes, '0') = '0' or a.connectionType in :connectionTypes)
                            and (:prompt is null
                                or (:searchBy = 'ALL'
                                    and (
                                        text(a.id) like :prompt
                                        or lower(text(a.fields)) like :prompt
                                        or text(a.connectionType) = 'PRODUCT_CONTRACT' and exists(
                                            select 1 from ProductContract c
                                            join ProductContractActivity ca on ca.contractId = c.id
                                                where c.status = 'ACTIVE'
                                                and ca.status = 'ACTIVE'
                                                and ca.systemActivityId = a.id
                                                and lower(c.contractNumber) like :prompt
                                        )
                                        or text(a.connectionType) = 'SERVICE_CONTRACT' and exists(
                                            select 1 from ServiceContracts sc
                                            join ServiceContractActivity sca on sca.contractId = sc.id
                                                where sc.status = 'ACTIVE'
                                                and sca.status = 'ACTIVE'
                                                and sca.systemActivityId = a.id
                                                and lower(sc.contractNumber) like :prompt
                                        )
                                        or text(a.connectionType) = 'TASK' and exists(
                                            select 1 from Task t
                                            join TaskActivity ta on ta.taskId =  t.id
                                            where t.status = 'ACTIVE'
                                                and ta.status =  'ACTIVE'
                                                and ta.systemActivityId = a.id
                                                and text(t.id) like :prompt
                                        )
                                        or text(a.connectionType) = 'SERVICE_ORDER' and exists(
                                            select 1 from ServiceOrder so
                                            join ServiceOrderActivity oa on oa.orderId =  so.id
                                            where so.status = 'ACTIVE'
                                                and oa.status =  'ACTIVE'
                                                and oa.systemActivityId = a.id
                                                and lower(so.orderNumber) like :prompt
                                        )
                                        or text(a.connectionType) = 'GOODS_ORDER' and exists(
                                            select 1 from GoodsOrder go
                                            join GoodsOrderActivity oa on oa.orderId =  go.id
                                            where go.status = 'ACTIVE'
                                                and oa.status =  'ACTIVE'
                                                and oa.systemActivityId = a.id
                                                and lower(go.orderNumber) like :prompt
                                        )
                                    )
                                )
                                or ((:searchBy = 'ID' and text(a.id) like :prompt))
                                or (:searchBy = 'JSON_DATA' and lower(text(a.fields)) like :prompt)
                                or (:searchBy = 'CONTRACT_NUMBER'
                                    and (
                                        (text(a.connectionType) = 'PRODUCT_CONTRACT'
                                            and exists(select 1 from ProductContract c
                                            join ProductContractActivity ca on ca.contractId = c.id
                                            where c.status = 'ACTIVE'
                                                and ca.status = 'ACTIVE'
                                                and ca.systemActivityId =  a.id
                                                and lower(c.contractNumber) like :prompt
                                        )
                                    )
                                    or (text(a.connectionType) = 'SERVICE_CONTRACT'
                                            and exists(select 1 from ServiceContracts sc
                                            join ServiceContractActivity ca on ca.contractId = sc.id
                                                where sc.status = 'ACTIVE'
                                                and ca.status = 'ACTIVE'
                                                and ca.systemActivityId = a.id
                                                and lower(sc.contractNumber) like :prompt)
                                    )
                                )
                                )
                                or (:searchBy = 'ORDER_NUMBER'
                                    and (
                                        (text(a.connectionType) = 'SERVICE_ORDER' and
                                         exists(
                                            select 1 from ServiceOrder so
                                            join ServiceOrderActivity oa on oa.orderId =  so.id
                                            where so.status = 'ACTIVE'
                                                and oa.status =  'ACTIVE'
                                                and oa.systemActivityId = a.id
                                                and lower(so.orderNumber) like :prompt
                                        )
                                    )
                                    or (text(a.connectionType) = 'GOODS_ORDER'
                                             and exists(
                                            select 1 from GoodsOrder go
                                            join GoodsOrderActivity oa on oa.orderId =  go.id
                                            where go.status = 'ACTIVE'
                                                and oa.status =  'ACTIVE'
                                                and oa.systemActivityId = a.id
                                                and lower(go.orderNumber) like :prompt)
                                    )
                                )
                                )
                                or (:searchBy = 'TASK_ID' and text(a.connectionType) = 'TASK'
                                    and exists(select 1 from Task t
                                        join TaskActivity ta on ta.taskId =  t.id
                                        where t.status = 'ACTIVE'
                                            and ta.status = 'ACTIVE'
                                            and ta.systemActivityId = a.id
                                            and text(t.id) like :prompt
                                        )
                                    )
                              
                            )
                    """
    )
    Page<SystemActivityListResponse> list(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchField,
            @Param("status") List<EntityStatus> status,
            @Param("subActivityIds") List<Long> subActivityIds,
            @Param("connectionTypes") List<SystemActivityConnectionType> connectionTypes,
            Pageable pageable
    );


    Optional<SystemActivity> findByIdAndStatusIn(Long id, List<EntityStatus> status);


    @Query(value = "select nextval('activity.activity_id_seq')", nativeQuery = true)
    Long getNextSequenceValue();


    @Query("select a.connectionType from SystemActivity a where a.id = :id")
    Optional<SystemActivityConnectionType> findConnectionTypeById(Long id);

    @Modifying
    @Query("""
            update SystemActivity
            set fields = :fields
            where id = :id
            """)
    void editActivity(Long id, List<SystemActivityJsonField> fields);
}
