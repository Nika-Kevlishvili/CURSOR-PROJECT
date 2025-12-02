package bg.energo.phoenix.util;

import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;

import java.util.List;
import java.util.Objects;

import static bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus.*;

public class ServiceOrderStatusChainUtil {

    /**
     * Checks if the status can be changed manually by the user.
     *
     * @param current current status
     * @param target  target status
     * @return true if the status can be changed manually by the user, false otherwise
     */
    public static boolean canBeChangedManually(ServiceOrderStatus current, ServiceOrderStatus target) {
        return switch (current) {
            case REQUESTED -> List.of(CONFIRMED, REFUSED).contains(target);
            case CONFIRMED, AWAITING_PAYMENT -> Objects.equals(REFUSED, target);
            case PAID -> List.of(IN_EXECUTION, REFUSED).contains(target);
            case IN_EXECUTION -> List.of(COMPLETED, REFUSED).contains(target);
            default -> false;
        };
    }


    /**
     * Checks if the status can be changed automatically by the system.
     *
     * @param current current status
     * @param target  target status
     * @return true if the status can be changed automatically by the system, false otherwise
     */
    public static boolean canBeChangedAutomatically(ServiceOrderStatus current, ServiceOrderStatus target) {
        return switch (current) {
            case CONFIRMED -> Objects.equals(AWAITING_PAYMENT, target);
            case AWAITING_PAYMENT -> Objects.equals(PAID, target);
            default -> false;
        };
    }

}
