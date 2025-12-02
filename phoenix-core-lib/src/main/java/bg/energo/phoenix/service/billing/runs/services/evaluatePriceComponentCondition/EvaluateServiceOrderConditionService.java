package bg.energo.phoenix.service.billing.runs.services.evaluatePriceComponentCondition;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluateServiceOrderConditionService {
    @PersistenceContext
    private EntityManager em;

    public String replaceConditionWithDbValues(String condition) {
        String conditionPart = """
                with recursive
                    replacements_data as (select condition_text,
                                                 replacement_text,
                                                 row_number() over () as id
                                          from billing_run.product_contract_pc_condition_replacemets),
                    replacements as (select :condition as text_value,
                                            1          as iteration
                                
                                     union all
                                
                                     select replace(r.text_value, rd.condition_text, rd.replacement_text) as text_value,
                                            r.iteration + 1                                               as iteration
                                     from replacements r
                                              join replacements_data rd on rd.id = r.iteration
                                     where r.iteration <= (select count(*)
                                                           from billing_run.product_contract_pc_condition_replacemets)),
                    final_replacements as (select replace(replace(text_value, '$$', ' , '), '$', ' ') as replaced_text
                                           from replacements
                                           where iteration =
                                                 (select count(*) + 1 from billing_run.product_contract_pc_condition_replacemets))
                select replaced_text
                from final_replacements
                """;
        Query query = em.createNativeQuery(conditionPart);
        query.setParameter("condition", condition);
        try {
            String res = (String) query.getSingleResult();
            return res.replaceAll("region.id", "region_id"); //
        } catch (NoResultException e) {
            return "";
        }
    }

    public Integer evaluateConditions(Long serviceOrderId, Long podId, String conditionPart) {

        String finalQueryPart = """
                                        select 1
                                        from (select c_c.customer_type
                                                   , c_segments.segment_id
                                                   , c_preferences.preferences_id
                                                   , pod_d.country_id
                                                   , pod_d.populated_place_id
                                                   , pod_d.consumption_purpose
                                                   , pod_d.measurement_type
                                                   , nom_grd_ops.id                                               as grid_operator_id
                                                   , pod_d.voltage_level
                                                   , pod_d.provided_power
                                                   , pod_d.multiplier
                                                   , coalesce(customer_d.direct_debit, false) or p_c.direct_debit as direct_debit
                                                   , case
                                                         when p.pod_id is not null and pod_pod.status = 'ACTIVE' then true
                                                         else false
                                                end                                                               as active_power_supply
                                                   , pdap.pod_additional_param
                                                   , region.id                                                    as region_id
                                              from service_order.orders p_c
                                                       left join service_order.order_pods p_c_pods on
                                                  p_c_pods.order_id = p_c.id
                                                       left join pod.pod pod_pod on p_c_pods.pod_id = pod_pod.id
                                                       left join pod.pod_details pod_d on
                                                  pod_pod.last_pod_detail_id = pod_d.id
                                                       inner join service.service_details prod_d on
                                                  p_c.service_detail_id = prod_d.id
                                                       left join pod.pod_details_additional_params pdap
                                                                 on pdap.pod_detail_id = pod_d.id and pdap.status <> 'DELETED'
                                                       inner join customer.customer_details customer_d on customer_d.id = p_c.customer_detail_id
                                                       inner join customer.customers c_c on c_c.id = customer_d.customer_id
                                                       left join customer.customer_segments c_segments
                                                                 on c_segments.customer_detail_id = customer_d.id and c_segments.status = 'ACTIVE'
                                                       left join customer.customer_preferences c_preferences
                                                                 on c_preferences.customer_detail_id = customer_d.id and c_preferences.status = 'ACTIVE'
                                                       left join nomenclature.grid_operators nom_grd_ops on nom_grd_ops.id = pod_pod.grid_operator_id
                                                       left join receivable.power_supply_disconnection_pods p on p.pod_id = pod_pod.id
                                                       left join nomenclature.populated_places pop_p on pod_d.populated_place_id = pop_p.id
                                                       left join nomenclature.municipalities mun on pop_p.municipality_id = mun.id
                                                       left join nomenclature.regions region on mun.region_id = region.id
                                              where p_c.id = :orderId
                                                and (:podId is null or pod_pod.id = :podId)) a
                                        where 1 = 1
                                          and (
                                        """ + conditionPart + " ) limit 1";

        Query finalQuery;

        // Handle the null podId case by modifying the query
        if (podId == null) {
            // Replace the podId parameter placeholder with a direct SQL expression that always evaluates to true
            String modifiedQuery = finalQueryPart.replace("(:podId is null or pod_pod.id = :podId)", "true");

            finalQuery = em.createNativeQuery(modifiedQuery);
            finalQuery.setParameter("orderId", serviceOrderId);
        } else {
            finalQuery = em.createNativeQuery(finalQueryPart);
            finalQuery.setParameter("orderId", serviceOrderId);
            finalQuery.setParameter("podId", podId);
        }
        try {
            return (Integer) finalQuery.getSingleResult();
        } catch (NoResultException e) {
            return 0;
        }
    }

}
