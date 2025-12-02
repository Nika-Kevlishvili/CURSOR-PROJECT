package bg.energo.phoenix.model.enums.contract.products;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;

import java.util.Objects;
import java.util.stream.Stream;

public enum ActivationDateComparisonResult {
    HAS_ACTIVATION_IN_BOTH_VERSION,
    HAS_ACTIVATION_IN_PREVIOUS_VERSION,
    HAS_ACTIVATION_IN_UPDATED_VERSION,
    DOES_NOT_HAVE_ACTIVATION_DATES;

    public static ActivationDateComparisonResult compareActivationDates(ContractPods previousPod, ContractPods updatedPod) {
        if (Stream.of(previousPod, updatedPod).allMatch(pod -> Objects.nonNull(pod.getActivationDate()))) {
            return ActivationDateComparisonResult.HAS_ACTIVATION_IN_BOTH_VERSION;
        }

        if (Stream.of(previousPod, updatedPod).allMatch(pod -> Objects.isNull(pod.getActivationDate()))) {
            return ActivationDateComparisonResult.DOES_NOT_HAVE_ACTIVATION_DATES;
        }

        if (Objects.nonNull(previousPod.getActivationDate())) {
            return ActivationDateComparisonResult.HAS_ACTIVATION_IN_PREVIOUS_VERSION;
        } else {
            return ActivationDateComparisonResult.HAS_ACTIVATION_IN_UPDATED_VERSION;
        }
    }
}
