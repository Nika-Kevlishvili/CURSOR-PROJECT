package bg.energo.phoenix.repository.product.price.priceComponent;

import bg.energo.phoenix.billingRun.model.PriceComponentFormulaXValue;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentProjectionForIap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;


@Repository
public interface PriceComponentFormulaVariableRepository extends JpaRepository<PriceComponentFormulaVariable, Long> {
    @Query("""
            select pcfv
            from PriceComponentFormulaVariable pcfv
            join PriceComponent pc on pcfv.priceComponent.id = pc.id
            where pc.id in(:priceComponentIds)
            """)
    List<PriceComponentFormulaVariable> findAllByPriceComponentIdIn(List<Long> priceComponentIds);

    List<PriceComponentFormulaVariable> findByPriceComponent(PriceComponent priceComponent);

    List<PriceComponentFormulaVariable> findAllByPriceComponentIdOrderByIdAsc(Long priceComponentId);
    List<PriceComponentFormulaVariable> findAllByPriceComponentId(Long priceComponentId);

    List<PriceComponentFormulaVariable> findAllByIdIn(Set<Long> ids);

    @Modifying
    @Query("delete from PriceComponentFormulaVariable pcfv where pcfv.priceComponent.id = :priceComponentId")
    void deleteAllByPriceComponentId(Long priceComponentId);

    @Query(
            """
            select new bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentProjectionForIap(
                iap.id,
                pcvf.id,
                pc.id,
                pc.name,
                pcvf.variable,
                pcvf.description,
                pcvf.value,
                pcvf.valueFrom,
                pcvf.valueTo,
                pcvf.profileForBalancing.id
            )
            from InterimAdvancePayment iap
            join PriceComponent pc on iap.priceComponent.id = pc.id
            join PriceComponentFormulaVariable pcvf on pcvf.priceComponent.id =pc.id
                where iap.id in :ids
                and pc.status='ACTIVE'
            """
    )
    List<PriceComponentProjectionForIap> findAllByIapIds(Set<Long> ids);


    @Query(
            value = """
                    select pcfv.id from PriceComponentFormulaVariable pcfv
                    where pcfv.id in (:formulaVariableIds)
                    and exists(
                        select pc.id from ServicePriceComponentGroup spcg
                        join PriceComponentGroup pcg on spcg.priceComponentGroup.id = pcg.id
                            and pcg.status = 'ACTIVE'
                            and spcg.status = 'ACTIVE'
                        join PriceComponentGroupDetails pcgd on pcgd.priceComponentGroupId = pcg.id
                        join PriceComponentGroupPriceComponent pgp on pgp.priceComponentGroupDetailId = pcgd.id
                            and pgp.status = 'ACTIVE'
                        join PriceComponent pc on pc.id = pgp.priceComponentId and pc.status = 'ACTIVE'
                            where pcfv.priceComponent.id = pc.id
                            and spcg.serviceDetails.id = :serviceDetailId
                    )
                    """
    )
    List<Long> findAllBelongingToPriceComponentGroupOfServiceDetailAndIdIn(
            @Param("serviceDetailId") Long serviceDetailId,
            @Param("formulaVariableIds") List<Long> formulaVariableIds
    );

    @Query(nativeQuery = true, value = """
            select case when pcfv.value is null then cpc.value else pcfv.value end as value,
                   pcfv.formula_variable                                           as key
            from price_component.price_component_formula_variables pcfv
                     join service_order.orders o on o.id = :serviceOrderId
                     left join service_order.order_price_components cpc on cpc.price_component_formula_variable_id = pcfv.id
            where pcfv.price_component_id = :priceComponentId
            """)
    List<PriceComponentFormulaXValue> findAllByPriceComponentIdAndServiceOrderId(Long priceComponentId, Long serviceOrderId);

}
