package bg.energo.phoenix.util.contract.goods;

import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;

import java.util.List;
import java.util.Objects;

public class GoodsOrderStatusChainUtil {
    /**
     * Checks if the status can be changed manually by the user.
     *
     * @param current current status
     * @param target  target status
     * @return true if the status can be changed manually by the user, false otherwise
     */
    public static boolean canBeChangedManually(GoodsOrderStatus current, GoodsOrderStatus target) {
        return switch (current) {
            case REQUESTED -> List.of(GoodsOrderStatus.CONFIRMED, GoodsOrderStatus.REFUSED).contains(target);
            case CONFIRMED, AWAITING_PAYMENT -> Objects.equals(GoodsOrderStatus.REFUSED, target);
            case PAID -> List.of(GoodsOrderStatus.IN_EXECUTION, GoodsOrderStatus.REFUSED).contains(target);
            case IN_EXECUTION -> List.of(GoodsOrderStatus.COMPLETED, GoodsOrderStatus.REFUSED).contains(target);
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
    public static boolean canBeChangedAutomatically(GoodsOrderStatus current, GoodsOrderStatus target) {
        return switch (current) {
            case CONFIRMED -> Objects.equals(GoodsOrderStatus.AWAITING_PAYMENT, target);
            case AWAITING_PAYMENT -> Objects.equals(GoodsOrderStatus.PAID, target);
            default -> false;
        };
    }
}
