package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.entity.product.service.ServiceLinkedProduct;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceLinkedProductRepository extends JpaRepository<ServiceLinkedProduct, Long> {

    List<ServiceLinkedProduct> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);


    @Query(
            nativeQuery = true,
            value = """
                    select distinct coalesce(max(tbl.isvalid), 'true') from (
                        select
                            case when slp.obligatory = 'OBLIGATORY_CONDITION' and slp.allows_sales_under = 'CONCLUDED_CONTRACT'
                                then coalesce(
                                    (select distinct 'true' from product_contract.contracts c
                                    join product_contract.contract_details cd on cd.contract_id = c.id
                                        where cd.product_detail_id in (select pd.id from product.product_details pd where pd.product_id = slp.linked_product_id)
                                        and cd.customer_detail_id in (select cd.id from customer.customer_details cd where cd.customer_id = :customerId)
                                        and c.status = 'ACTIVE'
                                        and (
                                            (c.contract_status = 'SIGNED' and c.contract_sub_status in ('SIGNED_BY_CUSTOMER', 'SIGNED_BY_BOTH_SIDES', 'SPECIAL_PROCESSES'))
                                            or c.contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                        )
                                    ), 'false')
                            when slp.obligatory = 'OBLIGATORY_CONDITION' and slp.allows_sales_under = 'ACTIVATED_CONTRACT'
                                then coalesce(
                                    (select distinct 'true' from product_contract.contracts c
                                    join product_contract.contract_details cd on cd.contract_id = c.id
                                        where cd.product_detail_id in (select pd.id from product.product_details pd where pd.product_id = slp.linked_product_id)
                                        and cd.customer_detail_id in (select cd.id from customer.customer_details cd where cd.customer_id = :customerId)
                                        and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                        and c.status = 'ACTIVE'
                                    ), 'false')
                            end isvalid
                        from service.service_linked_products slp
                        where slp.service_detail_id = :serviceDetailId and slp.status = 'ACTIVE'
                        union
                        select
                            case when sls.obligatory = 'OBLIGATORY_CONDITION' and sls.allows_sales_under = 'CONCLUDED_CONTRACT'
                                then coalesce(
                                    (select distinct 'true' from service_contract.contracts c
                                    join service_contract.contract_details cd on cd.contract_id = c.id
                                        where cd.service_detail_id in (select sd.id from service.service_details sd where sd.service_id = sls.linked_service_id)
                                        and cd.customer_detail_id in (select cd.id from customer.customer_details cd where cd.customer_id = :customerId)
                                        and c.status = 'ACTIVE'
                                        and (
                                            (c.contract_status = 'SIGNED' and c.contract_sub_status in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES','SIGNED_BY_CUSTOMER'))
                                            or c.contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                        )
                                    ), 'false')
                            when sls.obligatory = 'OBLIGATORY_CONDITION' and sls.allows_sales_under = 'ACTIVATED_CONTRACT'
                                then coalesce(
                                    (select distinct 'true' from service_contract.contracts c
                                    join service_contract.contract_details cd on cd.contract_id = c.id
                                        where cd.service_detail_id in (select sd.id from service.service_details sd where sd.service_id = sls.linked_service_id)
                                        and cd.customer_detail_id in (select cd.id from customer.customer_details cd where cd.customer_id = :customerId)
                                        and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                        and c.status = 'ACTIVE'
                                    ), 'false')
                            end isvalid
                        from service.service_linked_services sls
                        where sls.service_detail_id = :serviceDetailId and sls.status = 'ACTIVE'
                        union
                        select max(tbl1.isvalid)
                                from (
                                    select
                                    case when slp.obligatory = 'AT_LEAST_ONE_CONDITION' and slp.allows_sales_under = 'CONCLUDED_CONTRACT'
                                        then coalesce(
                                            (select distinct 'true' from product_contract.contracts c
                                            join product_contract.contract_details cd on cd.contract_id = c.id
                                                where cd.product_detail_id in(select pd.id from product.product_details pd where pd.product_id = slp.linked_product_id)
                                                and cd.customer_detail_id in(select cd.id from customer.customer_details cd where cd.customer_id = :customerId )
                                                and c.status = 'ACTIVE'
                                                and (
                                                    (c.contract_status = 'SIGNED' and c.contract_sub_status in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES','SIGNED_BY_CUSTOMER'))
                                                    or c.contract_status in ('ENTERED_INTO_FORCE','ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
                                                )
                                            ),'false') end isvalid
                                            from service.service_linked_products slp
                                            where service_detail_id = :serviceDetailId and slp.status = 'ACTIVE'
                                    union
                                    select
                                    case when sls.obligatory = 'AT_LEAST_ONE_CONDITION' and sls.allows_sales_under = 'CONCLUDED_CONTRACT'
                                        then coalesce(
                                            (select distinct 'true' from service_contract.contracts c
                                            join service_contract.contract_details cd on cd.contract_id = c.id
                                                where cd.service_detail_id in(select sd.id from service.service_details sd where sd.service_id = sls.linked_service_id)
                                                and cd.customer_detail_id in(select cd.id from customer.customer_details cd where cd.customer_id = :customerId )
                                                and c.status = 'ACTIVE'
                                                and (
                                                    (c.contract_status = 'SIGNED' and c.contract_sub_status in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES','SIGNED_BY_CUSTOMER'))
                                                    or c.contract_status in ('ENTERED_INTO_FORCE','ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
                                                )
                                            ),'false') end isvalid
                                            from service.service_linked_services sls
                                            where service_detail_id = :serviceDetailId and sls.status = 'ACTIVE'
                                ) as tbl1 where isvalid is not null
                            union
                            select max(tbl2.isvalid)
                                from (
                                    select
                                    case when slp.obligatory = 'AT_LEAST_ONE_CONDITION' and slp.allows_sales_under = 'ACTIVATED_CONTRACT'
                                        then coalesce(
                                            (select distinct 'true' from product_contract.contracts c
                                            join product_contract.contract_details cd on cd.contract_id = c.id
                                                where cd.product_detail_id in (select pd.id from product.product_details pd where pd.product_id = slp.linked_product_id)
                                                and cd.customer_detail_id in (select cd.id from customer.customer_details cd where cd.customer_id = :customerId)
                                                and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                                and c.status = 'ACTIVE'
                                            ), 'false'
                                        ) end isvalid
                                    from service.service_linked_products slp
                                    where service_detail_id = :serviceDetailId and slp.status = 'ACTIVE'
                                    union
                                    select
                                    case when sls.obligatory = 'AT_LEAST_ONE_CONDITION' and sls.allows_sales_under = 'ACTIVATED_CONTRACT'
                                        then coalesce(
                                            (select distinct 'true' from service_contract.contracts c
                                            join service_contract.contract_details cd on cd.contract_id = c.id
                                                where cd.service_detail_id in (select sd.id from service.service_details sd where sd.service_id = sls.linked_service_id)
                                                and cd.customer_detail_id in (select cd.id from customer.customer_details cd where cd.customer_id = :customerId)
                                                and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                                                and c.status = 'ACTIVE'
                                            ), 'false'
                                        ) end isvalid
                                    from service.service_linked_services sls
                                    where service_detail_id = :serviceDetailId and sls.status = 'ACTIVE'
                                ) as tbl2 where isvalid is not null
                    ) as tbl where tbl.isvalid = 'false'
                    """
    )
    boolean canCreateContractWithServiceAndCustomer(
            @Param("serviceDetailId") Long serviceDetailId,
            @Param("customerId") Long customerId
    );

}
