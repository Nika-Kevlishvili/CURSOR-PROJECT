package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocumentInfoResponse;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsResults;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.CustomersForDPSMiddleResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DisconnectionPowerSupplyRequestsResultsRepository extends JpaRepository<DisconnectionPowerSupplyRequestsResults, Long> {

    @Query(
            nativeQuery = true,
            value = """
                    select
                            result.customers as customer,
                            result.contracts as contracts,
                            result.alt_recipient_inv_customers as altRecipientInvCustomer,
                            result.billing_groups as billingGroups,
                            result.pod_identifier as podIdentifier,
                            result.is_highest_consumption as isHighestConsumption,
                            result.liabilities_in_billing_group as liabilitiesInBillingGroup,
                            result.liabilities_in_pod as liabilitiesInPod,
                            result.pod_id as podId,
                            result.customer_id as customerId,
                            result.liability_amount_customer as liabilityAmountCustomer,
                            result.existing_customer_receivables as existingCustomerReceivables,
                            result.customer_number as customerNumber,
                            result.invoice_number as invoiceNumber,
                            result.is_checked as isChecked
                             from receivable.power_supply_disconnection_request_results result 
                             where result.power_supply_disconnection_request_id = :disconnectionPowerSupplyRequestId
                             and
                             (:prompt is null or (:searchBy = 'ALL' and (lower(result.customers) like :prompt
                                                                    or
                                                                  lower(result.customer_number) like :prompt 
                                                                   or
                                                                  lower(result.contracts) like :prompt 
                                                                   or
                                                                  lower(result.billing_groups) like :prompt 
                                                                   or
                                                                  lower(result.pod_identifier) like :prompt 
                                                                   or
                                                                  lower(result.liabilities_in_billing_group) like :prompt
                                                                  or
                                                                  lower(result.liabilities_in_pod) like :prompt
                                                                   or
                                                                  lower(result.invoice_number) like :prompt 
                                                                   )
                                               )
                                               or (
                                                   (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(result.customers) like :prompt)
                                                    or
                                                   (:searchBy = 'CUSTOMER_NUMBER' and text(result.customer_number) like :prompt)
                                                    or
                                                   (:searchBy = 'CONTRACT_NUMBER' and lower(result.contracts) like :prompt)
                                                    or
                                                   (:searchBy = 'BILLING_GROUP_NUMBER' and lower(result.billing_groups) like :prompt)
                                                    or
                                                   (:searchBy = 'POD_IDENTIFIER' and lower(result.pod_identifier) like :prompt)
                                                    or
                                                   (:searchBy = 'LIABILITY_NUMBER' and lower(result.liabilities_in_billing_group) like :prompt)
                                                   or
                                                   (:searchBy = 'LIABILITY_NUMBER' and lower(result.liabilities_in_pod) like :prompt)
                                                    or
                                                   (:searchBy = 'OUTGOING_DOCUMENT_NUMBER' and lower(result.invoice_number) like :prompt)
                                               )                           
                      ) 
                      and 
                     (:liabilityAmountFrom is null or result.liability_amount_customer >=  :liabilityAmountFrom)
                      and 
                     (:liabilityAmountTo is null or  result.liability_amount_customer <=  :liabilityAmountTo) 
                      and 
                     (:isHighestConsumption is null or  text(result.is_highest_consumption) = text(:isHighestConsumption)) order by customer
                     """)
    Page<CustomersForDPSMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("isHighestConsumption") Boolean isHighestConsumption,
            @Param("disconnectionPowerSupplyRequestId") Long disconnectionPowerSupplyRequestId,
            @Param("liabilityAmountFrom") BigDecimal liabilityAmountFrom,
            @Param("liabilityAmountTo") BigDecimal liabilityAmountTo,
            Pageable pageable
    );

    @Query(value = """
                    SELECT p.identifier as PODIdentifier,
                    result.customer_number as CustomerNumber,
                    c.identifier as CustomerIdentifier,
                    CASE
                        WHEN c.customer_type = 'PRIVATE_CUSTOMER'
                        THEN concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                        ELSE concat(cd.name, ' ', lf.name)
            END as CustomerNameComb,
                    cd.name as CustomerName,
                    cd.middle_name as CustomerMiddleName,
                    cd.last_name as CustomerSurname,
                    pd.measurement_type as MeasurementType,
                    dr.name as Reason,
                    psdr.power_supply_disconnection_date as DisconnectionDate,
                    concat_ws(
                        nullif(concat_ws(' ',
                            case
                                when pd.foreign_address is true then concat(pd.district_foreign, ',')
                                else concat(d.name, ',')
                            end,
                            case
                                when pd.foreign_address is true then
                                    case
                                        when cd.foreign_residential_area_type is not null
                                            then concat(pd.foreign_residential_area_type, ' ', pd.residential_area_foreign)
                                        else pd.residential_area_foreign
                                    end
                                else
                                    case
                                        when ra.type is not null
                                            then concat(ra.type, ' ', ra.name)
                                        else ra.name
                                    end
                            end
                        ), ''),
                        nullif(concat_ws(' ',
                            case when pd.foreign_address is true then pd.foreign_street_type else s.type end,
                            case when pd.foreign_address is true then pd.street_foreign else s.name end,
                            cd.street_number
                        ), ''),
                        nullif(concat('бл. ', pd.block), 'бл. '),
                        nullif(concat('вх. ', pd.entrance), 'вх. '),
                        nullif(concat('ет. ', pd.floor), 'ет. '),
                        nullif(concat('ап. ', pd.apartment), 'ап. '),
                        nullif(pd.address_additional_info, '')
                    ) as PODAddressComb
                FROM receivable.power_supply_disconnection_requests psdr
                JOIN receivable.power_supply_disconnection_request_results result ON result.power_supply_disconnection_request_id = psdr.id
                JOIN customer.customers c ON c.id = result.customer_id
                JOIN customer.customer_details cd ON cd.id = c.last_customer_detail_id
                JOIN pod.pod p ON p.id = result.pod_id
                JOIN pod.pod_details pd ON pd.id = p.last_pod_detail_id
                JOIN nomenclature.disconnection_reasons dr ON psdr.disconnection_reason_id = dr.id
                LEFT JOIN nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                LEFT JOIN nomenclature.districts d on pd.district_id = d.id
                LEFT JOIN nomenclature.residential_areas ra on pd.residential_area_id = ra.id
                LEFT JOIN nomenclature.streets s on pd.street_id = s.id
                WHERE psdr.id = :requestId
            """, nativeQuery = true)
    List<DisconnectionPowerSupplyRequestsDocumentInfoResponse> getPodImplForDocument(@Param("requestId") Long requestId);
}
