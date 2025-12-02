package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.CcyRestrictions;
import bg.energo.phoenix.billingRun.model.EqualMonthlyData;
import bg.energo.phoenix.billingRun.model.KwhRestrictions;
import bg.energo.phoenix.billingRun.model.PriceComponentFormulaXValue;
import bg.energo.phoenix.billingRun.model.entity.BillingRunContracts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BillingRunContractsRepository extends JpaRepository<BillingRunContracts, Long> {

    @Query(value = """
            select brc.*
            from billing_run.run_contracts brc
            where brc.run_id=:runId
            and brc.processing_status=:processingStatus
            order by brc.contract_id
            limit :lim
            """,nativeQuery = true)
    List<BillingRunContracts> findAllByRunIdAndProcessingStatus(Long runId, String processingStatus,Integer lim);

    @Query(value = """
            select
                 case when pcfv.value is null then cpc.value else pcfv.value end  as value,
                 pcfv.formula_variable as key 
               from price_component.price_component_formula_variables pcfv 
                    left join product_contract.contract_price_components cpc on cpc.price_component_formula_variable_id  = pcfv.id and cpc.contract_detail_id = :contractDetailId
                    where pcfv.price_component_id  = :priceComponentId 
              """, nativeQuery = true)
    List<PriceComponentFormulaXValue> getPriceComponentXValuesForProductContracts(@Param("contractDetailId") Long contractDetailId, @Param("priceComponentId") Long priceComponentId);

    @Query(value = """
            select
                 case when pcfv.value is null then cpc.value else pcfv.value end  as value,
                 pcfv.formula_variable as key 
               from price_component.price_component_formula_variables pcfv 
                    left join service_contract.contract_price_components cpc on cpc.price_component_formula_variable_id  = pcfv.id  and cpc.contract_detail_id = :contractDetailId
                    where pcfv.price_component_id  = :priceComponentId
              """, nativeQuery = true)
    List<PriceComponentFormulaXValue> getPriceComponentXValuesForServiceContracts(@Param("contractDetailId") Long contractDetailId, @Param("priceComponentId") Long priceComponentId);


    @Query(value = """
            select COALESCE(afvbspkrr.value_from,0) valueFrom,COALESCE(afvbspkrr.value_to,2147483647) valueTo from price_component.am_for_volumes_by_scale_kwh_restriction_ranges afvbspkrr 
            inner join price_component.am_for_volumes_by_scales afvbsp on afvbspkrr.am_for_volumes_by_scale_id  = afvbsp.id 
            inner join price_component.application_models am on am.id  = afvbsp.application_model_id 
            where am.price_component_id  = :priceComponentId and afvbsp.status ='ACTIVE' and afvbspkrr.status ='ACTIVE' and am.status ='ACTIVE' and afvbsp.restriction_of_application_based_on_volume =true""", nativeQuery = true)
    List<KwhRestrictions> getScaleRestrictionByKwh(@Param("priceComponentId") Long priceComponentId);

    @Query(value = """
            select COALESCE(afvbspkrr.value_from,0) valueFrom,COALESCE(afvbspkrr.value_to,2147483647) valueTo from price_component.am_for_volumes_by_settlement_period_kwh_restriction_ranges afvbspkrr  
            inner join price_component.am_for_volumes_by_settlement_periods afvbsp on afvbspkrr.am_for_volumes_by_settlement_period_id  = afvbsp.id  
            inner join price_component.application_models am on am.id  = afvbsp.application_model_id  
            where am.price_component_id  = :priceComponentId and afvbsp.status ='ACTIVE' and afvbspkrr.status ='ACTIVE' and am.status ='ACTIVE' and afvbsp.restriction_of_application_based_on_volume =true """, nativeQuery = true)
    List<KwhRestrictions> getSettlementRestrictionByKwh(@Param("priceComponentId") Long priceComponentId);


    @Query(value = """
            select afvbspcrr.value_from valueFrom,afvbspcrr.value_to valueTo,afvbspcrr.currency_id currencyId from price_component.am_for_volumes_by_settlement_period_ccy_restriction_ranges afvbspcrr   
            inner join price_component.am_for_volumes_by_settlement_periods afvbsp on afvbspcrr.am_for_volumes_by_settlement_period_id  = afvbsp.id   
            inner join price_component.application_models am on am.id  = afvbsp.application_model_id   
            where am.price_component_id  = :priceComponentId and afvbsp.status ='ACTIVE' and afvbspcrr.status ='ACTIVE' and am.status ='ACTIVE' and afvbsp.restriction_of_application_based_on_values   = true    
             and afvbspcrr.value_from is not null and afvbspcrr.value_to is not null """, nativeQuery = true)
    List<CcyRestrictions> getSettlementRestrictionByCcy(@Param("priceComponentId") Long priceComponentId);

    @Query(value = """
            select afvbspcrr.value_from valueFrom,afvbspcrr.value_to valueTo,afvbspcrr.currency_id currencyId from price_component.am_for_volumes_by_scale_ccy_restriction_ranges  afvbspcrr    
            inner join price_component.am_for_volumes_by_scales afvbsp on afvbspcrr.am_for_volumes_by_scale_id  = afvbsp.id    
            inner join price_component.application_models am on am.id  = afvbsp.application_model_id    
            where am.price_component_id  = :priceComponentId and afvbsp.status ='ACTIVE' and afvbspcrr.status ='ACTIVE' and am.status ='ACTIVE' and afvbsp.restriction_of_application_based_on_values   = true  
              and afvbspcrr.value_from is not null and afvbspcrr.value_to is not null """, nativeQuery = true)
    List<CcyRestrictions> getScaleRestrictionByCcy(@Param("priceComponentId") Long priceComponentId);

    @Query(value = """
            select afvbsp.restriction_of_application_based_on_volume_percent  from  price_component.am_for_volumes_by_scales afvbsp     
            inner join price_component.application_models am on am.id  = afvbsp.application_model_id     
            where am.price_component_id  = :priceComponentId and afvbsp.status ='ACTIVE' and am.status ='ACTIVE' and afvbsp.restriction_of_application_based_on_volume  =true       """, nativeQuery = true)
    BigDecimal getScaleRestrictionPercent(@Param("priceComponentId") Long priceComponentId);

    @Query(value = """
            select afvbsp.restriction_of_application_based_on_volume_percent  from  price_component.am_for_volumes_by_settlement_periods afvbsp 
            inner join price_component.application_models am on am.id  = afvbsp.application_model_id 
            where am.price_component_id  = :priceComponentId and afvbsp.status ='ACTIVE' and am.status ='ACTIVE' and afvbsp.restriction_of_application_based_on_volume  =true  """, nativeQuery = true)
    BigDecimal getSettlementRestrictionPercent(@Param("priceComponentId") Long priceComponentId);

    @Modifying
    @Transactional
    @Query(value = """
            update billing_run.bg_invoice_slots set status =:status, error_message =:message where bg_invoice_slot_id = :invoiceSlotId""", nativeQuery = true)
    void updateBgInvoiceSlotStatus(@Param("invoiceSlotId") Long invoiceSlotId, @Param("status") String status, @Param("message") String message);

    @Modifying
    @Transactional
    @Query(value = """
            update billing_run.sv_invoice_slots set status =:status, error_message =:message where sv_invoice_slot_id = :invoiceSlotId""", nativeQuery = true)
    void updateSvInvoiceSlotStatus(@Param("invoiceSlotId") Long invoiceSlotId, @Param("status") String status, @Param("message") String message);




    @Query(value = """
            select case when sd.installment_number_from is not null and sd.installment_number_to  is not null then
            	case when cd.equal_monthly_installment_number between  sd.installment_number_from and sd.installment_number_to then cd.equal_monthly_installment_number 
            	else sd.installment_number end 
            else sd.installment_number end  as installmentNumber,
            
             case when sd.amount_from  is not null and sd.amount_to  is not null then
            	case when cd.equal_monthly_installment_amount  between  sd.amount_from and sd.amount_to then cd.equal_monthly_installment_amount  
            	else sd.amount  end 
            else sd.amount end  as installmentAmount,
            c.id as currencyId,
            c.alt_currency_id as altCurrencyId,
            c.alt_ccy_exchange_rate altExchangeRate
            from product_contract.contract_details cd 
            inner join product.product_details sd on cd.product_detail_id  = sd.id 
            inner join nomenclature.currencies c  on c.id  = sd.currency_id 
            where cd.id  =:contractDetailId and sd.equal_monthly_installments_activation  = true""", nativeQuery = true)
    EqualMonthlyData getProductContractEqualMonthlyData(@Param("contractDetailId") Long contractDetailId);




    @Query(value = """
            select case when sd.installment_number_from is not null and sd.installment_number_to  is not null then
            	case when cd.equal_monthly_installment_number between  sd.installment_number_from and sd.installment_number_to then cd.equal_monthly_installment_number 
            	else sd.installment_number end 
            else sd.installment_number end  as installmentNumber,
            
             case when sd.amount_from  is not null and sd.amount_to  is not null then
            	case when cd.equal_monthly_installment_amount  between  sd.amount_from and sd.amount_to then cd.equal_monthly_installment_amount  
            	else sd.amount  end 
            else sd.amount end  as installmentAmount,
            c.id as currencyId,
            c.alt_currency_id as altCurrencyId,
            c.alt_ccy_exchange_rate altExchangeRate
            from service_contract.contract_details cd 
            inner join service.service_details sd on cd.service_detail_id = sd.id 
            inner join nomenclature.currencies c  on c.id  = sd.currency_id 
            where cd.id  =:contractDetailId and sd.equal_monthly_installments_activation  = true""", nativeQuery = true)
    EqualMonthlyData getServiceContractEqualMonthlyData(@Param("contractDetailId") Long contractDetailId);


}
