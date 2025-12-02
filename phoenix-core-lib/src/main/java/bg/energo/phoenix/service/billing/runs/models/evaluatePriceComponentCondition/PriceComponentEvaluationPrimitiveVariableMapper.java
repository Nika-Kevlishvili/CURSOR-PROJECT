package bg.energo.phoenix.service.billing.runs.models.evaluatePriceComponentCondition;

import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum PriceComponentEvaluationPrimitiveVariableMapper {

    CUSTOMER_TYPE("cust.customer_type",
            PriceComponentEvaluationVariableValueType.HARDCODED,
            List.of(PriceComponentConditionVariableValue.LEGAL_ENTITY,
                    PriceComponentConditionVariableValue.PRIVATE_CUSTOMER),
            true),
    POD_VOLTAGE_LEVEL("podDet.voltage_level",
            PriceComponentEvaluationVariableValueType.HARDCODED,
            List.of(PriceComponentConditionVariableValue.LOW,
                    PriceComponentConditionVariableValue.MEDIUM,
                    PriceComponentConditionVariableValue.HIGH,
                    PriceComponentConditionVariableValue.MEDIUM_DIRECT_CONNECTED
            ),
            false),
    POD_COUNTRY("coalesce(podDet.country_id, -1)",
            PriceComponentEvaluationVariableValueType.DYNAMIC,
            Collections.emptyList(),
            false),
    PURPOSE_OF_CONSUMPTION("podDet.consumption_purpose",
            PriceComponentEvaluationVariableValueType.HARDCODED,
            List.of(PriceComponentConditionVariableValue.HOUSEHOLD,
                    PriceComponentConditionVariableValue.NON_HOUSEHOLD),
            false),
    POD_POPULATED_PLACE("coalesce(podDet.populated_place_id, -1)",
            PriceComponentEvaluationVariableValueType.DYNAMIC,
            Collections.emptyList(),
            false),
    POD_GRID_OP("pod.grid_operator_id",
            PriceComponentEvaluationVariableValueType.DYNAMIC,
            Collections.emptyList(),
            false),
    POD_PROVIDED_POWER("coalesce(podDet.provided_power, -1)",
            PriceComponentEvaluationVariableValueType.DYNAMIC,
            Collections.emptyList(),
            false),
    POD_MULTIPLIER("coalesce(podDet.multiplier, -1)",
            PriceComponentEvaluationVariableValueType.DYNAMIC,
            Collections.emptyList(),
            false),
    POD_MEASUREMENT_TYPE("podDet.measurement_type",
            PriceComponentEvaluationVariableValueType.HARDCODED,
            List.of(PriceComponentConditionVariableValue.SETTLEMENT_PERIOD,
                    PriceComponentConditionVariableValue.SLP),
            false),
    CONTRACT_TYPE("prodContDet.contract_type",
            PriceComponentEvaluationVariableValueType.HARDCODED,
            List.of(PriceComponentConditionVariableValue.COMBINED,
                    PriceComponentConditionVariableValue.SUPPLY_BALANCING,
                    PriceComponentConditionVariableValue.SUPPLY_ONLY,
                    PriceComponentConditionVariableValue.WITHOUT_SUPPLY),
            false),
    CONTRACT_CAMPAIGN("coalesce(prodContDet.campaign_id, -1)",
            PriceComponentEvaluationVariableValueType.DYNAMIC,
            Collections.emptyList(),
            true);


    private final String columnName;
    private final PriceComponentEvaluationVariableValueType valueType;
    private final List<PriceComponentConditionVariableValue> hardcodedValues;
    private final boolean calculateFromServiceContract;
}
