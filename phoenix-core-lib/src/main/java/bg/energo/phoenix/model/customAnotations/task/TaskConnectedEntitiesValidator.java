package bg.energo.phoenix.model.customAnotations.task;

import bg.energo.phoenix.model.enums.task.ConnectedEntityType;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.request.task.CreateTaskRequest;
import bg.energo.phoenix.model.request.task.TaskConnectedEntity;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {TaskConnectedEntitiesValidator.TaskConnectedEntitiesValidatorImpl.class})
public @interface TaskConnectedEntitiesValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class TaskConnectedEntitiesValidatorImpl implements ConstraintValidator<TaskConnectedEntitiesValidator, CreateTaskRequest> {
        @Override
        public boolean isValid(CreateTaskRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder violations = new StringBuilder();

            TaskConnectionType connectionType = request.getConnectionType();
            if (Objects.isNull(connectionType)) {
                return true;
            }

            List<TaskConnectedEntity> connectedEntities = request.getConnectedEntities();

            if (connectionType != TaskConnectionType.INTERNAL) {
                if (CollectionUtils.isEmpty(connectedEntities)) {
                    violations.append("connectedEntities-Connected entities must not be empty;");
                    return false;
                }
            } else {
                return true;
            }

            if (connectedEntities.stream().anyMatch(entity -> Objects.isNull(entity.getEntityType()))) {
                return true;
            }

            List<TaskConnectedEntity> connectedCustomers = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.CUSTOMER))
                    .toList();

            List<TaskConnectedEntity> connectedServiceOrders = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.SERVICE_ORDER))
                    .toList();

            List<TaskConnectedEntity> connectedGoodsOrder = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.GOODS_ORDER))
                    .toList();

            List<TaskConnectedEntity> connectedProductContract = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.PRODUCT_CONTRACT))
                    .toList();

            List<TaskConnectedEntity> connectedServiceContracts = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.SERVICE_CONTRACT))
                    .toList();

            List<TaskConnectedEntity> connectedCommunication = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.COMMUNICATION))
                    .toList();

            List<TaskConnectedEntity> connectedInternals = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.INTERNAL))
                    .toList();

            List<TaskConnectedEntity> connectedBillings = connectedEntities
                    .stream()
                    .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.BILLING))
                    .toList();

            boolean customersPresents = !connectedCustomers.isEmpty();
            boolean serviceOrdersPresents = !connectedServiceOrders.isEmpty();
            boolean goodsOrdersPresents = !connectedGoodsOrder.isEmpty();
            boolean productContractsPresents = !connectedProductContract.isEmpty();
            boolean serviceContractsPresents = !connectedServiceContracts.isEmpty();
            boolean communicationsPresents = !connectedCommunication.isEmpty();
            boolean internalsPresents = !connectedInternals.isEmpty();
            boolean billingsPresent = !connectedBillings.isEmpty();

            switch (connectionType) {
                case CUSTOMER -> {
                    if (serviceOrdersPresents ||
                            goodsOrdersPresents ||
                            productContractsPresents ||
                            serviceContractsPresents ||
                            communicationsPresents ||
                            internalsPresents ||
                            billingsPresent
                    ) {
                        violations.append("You are able to choose only one type of connected sub objects;");
                    }
                }
                case CONTRACT_ORDER -> {
                    if (customersPresents ||
                            communicationsPresents ||
                            internalsPresents ||
                            billingsPresent
                    ) {
                        violations.append("You are able to choose only one type of connected sub objects;");
                    }
                }
                case COMMUNICATION -> {
                    if (customersPresents ||
                            serviceOrdersPresents ||
                            goodsOrdersPresents ||
                            productContractsPresents ||
                            serviceContractsPresents ||
                            internalsPresents ||
                            billingsPresent
                    ) {
                        violations.append("You are able to choose only one type of connected sub objects;");
                    }
                }
                case BILLING -> {
                    if (customersPresents ||
                            serviceOrdersPresents ||
                            goodsOrdersPresents ||
                            productContractsPresents ||
                            serviceContractsPresents ||
                            communicationsPresents ||
                            internalsPresents
                    ) {
                        violations.append("You are able to choose only one type of connected sub objects;");
                    }
                }
            }

            Map<ConnectedEntityType, Set<Long>> uniqueIdsHolder = new HashMap<>();
            for (int i = 0; i < connectedEntities.size(); i++) {
                TaskConnectedEntity connectedEntity = connectedEntities.get(i);

                if (!uniqueIdsHolder.containsKey(connectedEntity.getEntityType())) {
                    uniqueIdsHolder.put(connectedEntity.getEntityType(), new HashSet<>());
                }

                if (!uniqueIdsHolder.get(connectedEntity.getEntityType()).add(connectedEntity.getId())) {
                    violations.append("connectedEntities[%s].id-Duplicated id for same connection Type;".formatted(i));
                }
            }

            if (!violations.isEmpty()) {
                isValid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
