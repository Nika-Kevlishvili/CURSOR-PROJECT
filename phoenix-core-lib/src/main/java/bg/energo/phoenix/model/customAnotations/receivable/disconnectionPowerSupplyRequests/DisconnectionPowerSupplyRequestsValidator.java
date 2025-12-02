package bg.energo.phoenix.model.customAnotations.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.DPSRequestsBaseRequest;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.CustomersForDPSResponse;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {DisconnectionPowerSupplyRequestsValidator.PodMeasurementValidatorImpl.class})
public @interface DisconnectionPowerSupplyRequestsValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodMeasurementValidatorImpl implements ConstraintValidator<DisconnectionPowerSupplyRequestsValidator, DPSRequestsBaseRequest> {
        @Override
        public boolean isValid(DPSRequestsBaseRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (Objects.nonNull(request.getGridOpDisconnectionFeePayDate()) && request.getGridOpDisconnectionFeePayDate().isBefore(LocalDate.now())) {
                errors.append("gridOpDisconnectionFeePayDate-gridOpDisconnectionFeePayDate must be current or future;");
            }

            if (Objects.nonNull(request.getPowerSupplyDisconnectionDate()) && request.getPowerSupplyDisconnectionDate().isBefore(LocalDate.now())) {
                errors.append("powerSupplyDisconnectionDate-powerSupplyDisconnectionDate must be current or future;");
            }

            if (Objects.nonNull(request.getLiabilityAmountFrom()) && Objects.nonNull(request.getLiabilityAmountTo())
                && request.getLiabilityAmountFrom().compareTo(request.getLiabilityAmountTo()) > 0) {
                errors.append("liabilityAmountFrom-liabilityAmountFrom can not be more than liabilityAmountTo;");
            }

            if (Objects.isNull(request.getLiabilityAmountFrom()) && Objects.isNull(request.getLiabilityAmountTo()) && Objects.nonNull(request.getCurrencyId())) {
                errors.append("currencyId-currencyId must be null when Liability amount from or Liability amount to is not defined;");
            }

            if ((Objects.nonNull(request.getLiabilityAmountFrom()) || Objects.nonNull(request.getLiabilityAmountTo())) && Objects.isNull(request.getCurrencyId())) {
                errors.append("currencyId-currencyId is mandatory when Liability amount from or Liability amount to is defined;");
            }

            CustomerConditionType conditionType = request.getConditionType();
            if (Objects.nonNull(conditionType)) {
                if ((conditionType.equals(CustomerConditionType.ALL_CUSTOMERS) || conditionType.equals(CustomerConditionType.CUSTOMERS_UNDER_CONDITIONS))
                    && Objects.nonNull(request.getListOfCustomer())) {
                    errors.append("listOfCustomer-listOfCustomer must be null when All customers or Customer under conditions option is selected;");
                }

                if (conditionType.equals(CustomerConditionType.LIST_OF_CUSTOMERS) && Objects.isNull(request.getListOfCustomer())) {
                    errors.append("listOfCustomer-listOfCustomer is mandatory when List of customers option is selected;");
                }

                if ((conditionType.equals(CustomerConditionType.LIST_OF_CUSTOMERS) ||
                     conditionType.equals(CustomerConditionType.ALL_CUSTOMERS)) &&
                    Objects.nonNull(request.getCondition())) {
                    errors.append("condition-condition must be null when All customers or List of customer option is selected;");
                }

                if (conditionType.equals(CustomerConditionType.CUSTOMERS_UNDER_CONDITIONS) && Objects.isNull(request.getCondition())) {
                    errors.append("condition-condition is mandatory when Customer under conditions option is selected;");
                }
            }

            List<CustomersForDPSResponse> pods = request.getPods();
            if (CollectionUtils.isNotEmpty(pods)) {
                DisconnectionRequestsStatus disconnectionRequestsStatus = request.getDisconnectionRequestsStatus();
                if (Objects.nonNull(disconnectionRequestsStatus)) {
                    for (CustomersForDPSResponse record : pods) {
                        if (disconnectionRequestsStatus.equals(DisconnectionRequestsStatus.EXECUTED)) {
                            if (Objects.isNull(record.getCustomers())) {
                                errors.append("pods.customers-customers can not be null;");
                            }
                            if (Objects.isNull(record.getContracts())) {
                                errors.append("pods.contracts-contracts can not be null;");
                            }
                            if (Objects.isNull(record.getBillingGroups())) {
                                errors.append("pods.billingGroups-billingGroups can not be null;");
                            }
                            if (Objects.isNull(record.getIsHighestConsumption())) {
                                errors.append("pods.isHighestConsumption-isHighestConsumption can not be null;");
                            }
                            if (Objects.isNull(record.getLiabilitiesInBillingGroup()) && Objects.isNull(record.getLiabilitiesInPod())) {
                                errors.append("pods.liabilitiesInBillingGroup-liabilitiesInBillingGroup or liabilitiesInPod must be provided;");
                            }
                            if (Objects.isNull(record.getPodId())) {
                                errors.append("pods.podId-podId can not be null;");
                            }
                            if (Objects.isNull(record.getCustomerId())) {
                                errors.append("pods.customerId-customerId can not be null;");
                            }
                            if (Objects.isNull(record.getExistingCustomerReceivables())) {
                                errors.append("pods.existingCustomerReceivables-existingCustomerReceivables can not be null;");
                            }
                            if (Objects.isNull(record.getPodIdentifier())) {
                                errors.append("pods.podIdentifier-podIdentifier can not be null;");
                            }
                            if (Objects.isNull(record.getInvoiceNumber())) {
                                errors.append("pods.invoiceNumber-invoiceNumber can not be null;");
                            }
                            if (Objects.isNull(record.getLiabilityAmountCustomer())) {
                                errors.append("pods.liabilityAmountCustomer-liabilityAmountCustomer can not be null;");
                            }
                            if (Objects.isNull(record.getCustomerNumber())) {
                                errors.append("pods.customerNumber-customerNumber can not be null;");
                            }
                        } else if (disconnectionRequestsStatus.equals(DisconnectionRequestsStatus.DRAFT)) {
                            if (Objects.isNull(record.getPodId())) {
                                errors.append("pods.podId-podId can not be null;");
                            }
                        }
                    }
                } else {
                    errors.append("disconnectionRequestsStatus-disconnectionRequestsStatus can not be null;");
                }
            } else {
                if (Boolean.FALSE.equals(request.getIsAllSelected()) && Boolean.FALSE.equals(request.getPodWithHighestConsumption())) {
                    errors.append("pods-pods can not be null;");
                }
            }

            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
