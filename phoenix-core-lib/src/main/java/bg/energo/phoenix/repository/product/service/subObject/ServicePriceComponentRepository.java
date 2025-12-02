package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.documentModels.contract.response.PriceComponentResponse;
import bg.energo.phoenix.model.entity.product.service.ServicePriceComponent;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePriceComponentRepository extends JpaRepository<ServicePriceComponent, Long> {

    List<ServicePriceComponent> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);

    @Query(nativeQuery = true, value = """

                        with recursive
                parameter_data as (select pc.id                             as price_component_id,
                                          pc.price_formula,
                                          cast(pc.price_formula as varchar) as processed_formula,
                                          0                                 as iteration
                                   from price_component.price_components pc
                                   where pc.id in (select pc_inner.id
                                                   from price_component.price_components pc_inner
                                                            join service.service_price_components ppc
                                                                 on pc_inner.id = ppc.price_component_id and ppc.status = 'ACTIVE'
                                                            join service.service_details pd on ppc.service_detail_id = pd.id
                                                            join service_contract.contract_details cd on pd.id = cd.service_detail_id
                                                            join service_contract.contracts c on cd.contract_id = c.id
                                                   where c.id = :id
                                                     and cd.version_id = :versionId
                                                     and pc_inner.contract_template_tag is not null

                                                   union

                                                   select pc_inner.id
                                                   from price_component.price_components pc_inner
                                                            join price_component.price_component_group_details pcgd
                                                                 on pc_inner.price_component_group_detail_id = pcgd.id
                                                            join price_component.price_component_groups pcg
                                                                 on pcgd.price_component_group_id = pcg.id
                                                            join service.service_price_component_groups ppcg
                                                                 on ppcg.price_component_group_id = pcg.id and ppcg.status = 'ACTIVE'
                                                            join service.service_details pd on ppcg.service_detail_id = pd.id
                                                            join service_contract.contract_details cd on pd.id = cd.service_detail_id
                                                            join service_contract.contracts c on cd.contract_id = c.id
                                                   where c.id = :id
                                                     and cd.version_id = :versionId
                                                     and pc_inner.contract_template_tag is not null)

                                   union all

                                   select pd.price_component_id,
                                          pd.price_formula,
                                          regexp_replace(
                                                  pd.processed_formula,
                                                  '\\$([0-9]+)\\$',
                                                  (select ppd.name
                                                   from prices.price_parameters pp
                                                            join prices.price_parameter_details ppd
                                                                 on ppd.id = pp.last_price_parameter_detail_id
                                                   where pp.id = substring(pd.processed_formula, '\\$([0-9]+)\\$')::bigint),
                                                  'g'
                                          ),
                                          pd.iteration + 1
                                   from parameter_data pd
                                   where pd.processed_formula ~ '\\$[0-9]+\\$'
                                     and pd.iteration < 10),

                final_formulas as (select distinct on (price_component_id) price_component_id,
                                                                           processed_formula
                                   from parameter_data
                                   order by price_component_id, iteration desc),

                main_query as (select pc.contract_template_tag              as tag,
                                      cur.print_name                        as currencyprintname,
                                      cur.abbreviation                      as currencyabr,
                                      cur.full_name                         as currencyfullname,
                                      replace(
                                              coalesce(ff.processed_formula, pc.price_formula),
                                              '$', ''
                                      )                                     as price,
                                      pc.price_in_words                     as pricewithwords,
                                      pc.name                               as pricecomponentname,
                                      pc.invoice_and_template_text          as pricecomponentnametemplates,

                                      x1.description                        as x1desc,
                                      text(coalesce(cpcx1.value, x1.value)) as x1value,

                                      x2.description                        as x2desc,
                                      text(coalesce(cpcx2.value, x2.value)) as x2value,

                                      x3.description                        as x3desc,
                                      text(coalesce(cpcx3.value, x3.value)) as x3value,

                                      x4.description                        as x4desc,
                                      text(coalesce(cpcx4.value, x4.value)) as x4value,

                                      x5.description                        as x5desc,
                                      text(coalesce(cpcx5.value, x5.value)) as x5value,

                                      x6.description                        as x6desc,
                                      text(coalesce(cpcx6.value, x6.value)) as x6value,

                                      x7.description                        as x7desc,
                                      text(coalesce(cpcx7.value, x7.value)) as x7value,

                                      x8.description                        as x8desc,
                                      text(coalesce(cpcx8.value, x8.value)) as x8value
                               from price_component.price_components pc
                                        join service.service_price_components ppc
                                             on pc.id = ppc.price_component_id and ppc.status = 'ACTIVE'
                                        join service.service_details pd on ppc.service_detail_id = pd.id
                                        join service_contract.contract_details cd on pd.id = cd.service_detail_id
                                        join service_contract.contracts c on cd.contract_id = c.id
                                        join nomenclature.currencies cur on pc.currency_id = cur.id
                                        left join final_formulas ff on ff.price_component_id = pc.id

                                        left join price_component.price_component_formula_variables x1
                                                  on pc.id = x1.price_component_id and x1.formula_variable = 'X1'
                                        left join service_contract.contract_price_components cpcx1
                                                  on cd.id = cpcx1.contract_detail_id
                                                      and x1.id = cpcx1.price_component_formula_variable_id and
                                                     cpcx1.status = 'ACTIVE'

                                        left join price_component.price_component_formula_variables x2
                                                  on pc.id = x2.price_component_id and x2.formula_variable = 'X2'
                                        left join service_contract.contract_price_components cpcx2
                                                  on cd.id = cpcx2.contract_detail_id
                                                      and x2.id = cpcx2.price_component_formula_variable_id and
                                                     cpcx2.status = 'ACTIVE'

                                        left join price_component.price_component_formula_variables x3
                                                  on pc.id = x3.price_component_id and x3.formula_variable = 'X3'
                                        left join service_contract.contract_price_components cpcx3
                                                  on cd.id = cpcx3.contract_detail_id
                                                      and x3.id = cpcx3.price_component_formula_variable_id and
                                                     cpcx3.status = 'ACTIVE'

                                        left join price_component.price_component_formula_variables x4
                                                  on pc.id = x4.price_component_id and x4.formula_variable = 'X4'
                                        left join service_contract.contract_price_components cpcx4
                                                  on cd.id = cpcx4.contract_detail_id
                                                      and x4.id = cpcx4.price_component_formula_variable_id and
                                                     cpcx4.status = 'ACTIVE'

                                        left join price_component.price_component_formula_variables x5
                                                  on pc.id = x5.price_component_id and x5.formula_variable = 'X5'
                                        left join service_contract.contract_price_components cpcx5
                                                  on cd.id = cpcx5.contract_detail_id
                                                      and x5.id = cpcx5.price_component_formula_variable_id and
                                                     cpcx5.status = 'ACTIVE'

                                        left join price_component.price_component_formula_variables x6
                                                  on pc.id = x6.price_component_id and x6.formula_variable = 'X6'
                                        left join service_contract.contract_price_components cpcx6
                                                  on cd.id = cpcx6.contract_detail_id
                                                      and x6.id = cpcx6.price_component_formula_variable_id and
                                                     cpcx6.status = 'ACTIVE'

                                        left join price_component.price_component_formula_variables x7
                                                  on pc.id = x7.price_component_id and x7.formula_variable = 'X7'
                                        left join service_contract.contract_price_components cpcx7
                                                  on cd.id = cpcx7.contract_detail_id
                                                      and x7.id = cpcx7.price_component_formula_variable_id and
                                                     cpcx7.status = 'ACTIVE'

                                        left join price_component.price_component_formula_variables x8
                                                  on pc.id = x8.price_component_id and x8.formula_variable = 'X8'
                                        left join service_contract.contract_price_components cpcx8
                                                  on cd.id = cpcx8.contract_detail_id
                                                      and x8.id = cpcx8.price_component_formula_variable_id and
                                                     cpcx8.status = 'ACTIVE'

                               where c.id = :id
                                 and cd.version_id = :versionId
                                 and pc.contract_template_tag is not null),

                group_query as (select pc.contract_template_tag              as tag,
                                       cur.print_name                        as currencyprintname,
                                       cur.abbreviation                      as currencyabr,
                                       cur.full_name                         as currencyfullname,
                                       replace(
                                               coalesce(ff.processed_formula, pc.price_formula),
                                               '$', ''
                                       )                                     as price,
                                       pc.price_in_words                     as pricewithwords,
                                       pc.name                               as pricecomponentname,
                                       pc.invoice_and_template_text          as pricecomponentnametemplates,

                                       x1.description                        as x1desc,
                                       text(coalesce(cpcx1.value, x1.value)) as x1value,

                                       x2.description                        as x2desc,
                                       text(coalesce(cpcx2.value, x2.value)) as x2value,

                                       x3.description                        as x3desc,
                                       text(coalesce(cpcx3.value, x3.value)) as x3value,

                                       x4.description                        as x4desc,
                                       text(coalesce(cpcx4.value, x4.value)) as x4value,

                                       x5.description                        as x5desc,
                                       text(coalesce(cpcx5.value, x5.value)) as x5value,

                                       x6.description                        as x6desc,
                                       text(coalesce(cpcx6.value, x6.value)) as x6value,

                                       x7.description                        as x7desc,
                                       text(coalesce(cpcx7.value, x7.value)) as x7value,

                                       x8.description                        as x8desc,
                                       text(coalesce(cpcx8.value, x8.value)) as x8value
                                from price_component.price_components pc
                                         join price_component.price_component_group_details pcgd
                                              on pc.price_component_group_detail_id = pcgd.id
                                         join price_component.price_component_groups pcg on pcgd.price_component_group_id = pcg.id
                                         join service.service_price_component_groups ppcg
                                              on ppcg.price_component_group_id = pcg.id and ppcg.status = 'ACTIVE'
                                         join service.service_details pd on ppcg.service_detail_id = pd.id
                                         join service_contract.contract_details cd on pd.id = cd.service_detail_id
                                         join service_contract.contracts c on cd.contract_id = c.id
                                         join nomenclature.currencies cur on pc.currency_id = cur.id
                                         left join final_formulas ff on ff.price_component_id = pc.id

                                         left join price_component.price_component_formula_variables x1
                                                   on pc.id = x1.price_component_id and x1.formula_variable = 'X1'
                                         left join service_contract.contract_price_components cpcx1
                                                   on cd.id = cpcx1.contract_detail_id
                                                       and x1.id = cpcx1.price_component_formula_variable_id and
                                                      cpcx1.status = 'ACTIVE'

                                         left join price_component.price_component_formula_variables x2
                                                   on pc.id = x2.price_component_id and x2.formula_variable = 'X2'
                                         left join service_contract.contract_price_components cpcx2
                                                   on cd.id = cpcx2.contract_detail_id
                                                       and x2.id = cpcx2.price_component_formula_variable_id and
                                                      cpcx2.status = 'ACTIVE'

                                         left join price_component.price_component_formula_variables x3
                                                   on pc.id = x3.price_component_id and x3.formula_variable = 'X3'
                                         left join service_contract.contract_price_components cpcx3
                                                   on cd.id = cpcx3.contract_detail_id
                                                       and x3.id = cpcx3.price_component_formula_variable_id and
                                                      cpcx3.status = 'ACTIVE'

                                         left join price_component.price_component_formula_variables x4
                                                   on pc.id = x4.price_component_id and x4.formula_variable = 'X4'
                                         left join service_contract.contract_price_components cpcx4
                                                   on cd.id = cpcx4.contract_detail_id
                                                       and x4.id = cpcx4.price_component_formula_variable_id and
                                                      cpcx4.status = 'ACTIVE'

                                         left join price_component.price_component_formula_variables x5
                                                   on pc.id = x5.price_component_id and x5.formula_variable = 'X5'
                                         left join service_contract.contract_price_components cpcx5
                                                   on cd.id = cpcx5.contract_detail_id
                                                       and x5.id = cpcx5.price_component_formula_variable_id and
                                                      cpcx5.status = 'ACTIVE'

                                         left join price_component.price_component_formula_variables x6
                                                   on pc.id = x6.price_component_id and x6.formula_variable = 'X6'
                                         left join service_contract.contract_price_components cpcx6
                                                   on cd.id = cpcx6.contract_detail_id
                                                       and x6.id = cpcx6.price_component_formula_variable_id and
                                                      cpcx6.status = 'ACTIVE'

                                         left join price_component.price_component_formula_variables x7
                                                   on pc.id = x7.price_component_id and x7.formula_variable = 'X7'
                                         left join service_contract.contract_price_components cpcx7
                                                   on cd.id = cpcx7.contract_detail_id
                                                       and x7.id = cpcx7.price_component_formula_variable_id and
                                                      cpcx7.status = 'ACTIVE'

                                         left join price_component.price_component_formula_variables x8
                                                   on pc.id = x8.price_component_id and x8.formula_variable = 'X8'
                                         left join service_contract.contract_price_components cpcx8
                                                   on cd.id = cpcx8.contract_detail_id
                                                       and x8.id = cpcx8.price_component_formula_variable_id and
                                                      cpcx8.status = 'ACTIVE'

                                where c.id = :id
                                  and cd.version_id = :versionId
                                  and pc.contract_template_tag is not null)

            select *
            from main_query
            union
            select *
            from group_query
            """)
    List<PriceComponentResponse> fetchPriceComponentsForDocument(Long id, Long versionId);

}
