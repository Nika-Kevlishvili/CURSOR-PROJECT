package bg.energo.phoenix.model.customAnotations.receivable.collectionChannel;

import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.TypeOfFile;
import bg.energo.phoenix.model.request.receivable.collectionChannel.CollectionChannelBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Objects;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {CollectionChannelBaseRequestValidator.PodMeasurementValidatorImpl.class})
public @interface CollectionChannelBaseRequestValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodMeasurementValidatorImpl implements ConstraintValidator<CollectionChannelBaseRequestValidator, CollectionChannelBaseRequest> {

        Class<?>[] groups;

        @Override
        public void initialize(CollectionChannelBaseRequestValidator constraintAnnotation) {
            this.groups = constraintAnnotation.groups();
        }

        @Override
        public boolean isValid(CollectionChannelBaseRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            boolean isOnEdit = groups != null && Arrays.asList(groups).contains(OnEdit.class);
            boolean skipPerformerValidation = isOnEdit && request.getPerformerId() == null;

            if (Objects.nonNull(request.getCustomerConditionType())) {
                if ((request.getCustomerConditionType().equals(CustomerConditionType.CUSTOMERS_UNDER_CONDITIONS) ||
                        request.getCustomerConditionType().equals(CustomerConditionType.ALL_CUSTOMERS)) &&
                        Objects.nonNull(request.getListOfCustomers())) {
                    errors.append("listOfCustomers-listOfCustomers must be null when All customers or Customer under conditions option is selected;");
                }

                if (request.getCustomerConditionType().equals(CustomerConditionType.LIST_OF_CUSTOMERS) && Objects.isNull(request.getListOfCustomers())) {
                    errors.append("listOfCustomers-listOfCustomers is mandatory when List of customer option is selected;");
                }

                if ((request.getCustomerConditionType().equals(CustomerConditionType.LIST_OF_CUSTOMERS) ||
                        request.getCustomerConditionType().equals(CustomerConditionType.ALL_CUSTOMERS)) &&
                        Objects.nonNull(request.getCondition())) {
                    errors.append("condition-condition must be null when All customers or List of customer option is selected;");
                }

                if (request.getCustomerConditionType().equals(CustomerConditionType.CUSTOMERS_UNDER_CONDITIONS) && Objects.isNull(request.getCondition())) {
                    errors.append("condition-condition is mandatory when Customer under conditions option is selected;");
                } else if (request.getCustomerConditionType().equals(CustomerConditionType.LIST_OF_CUSTOMERS) &&
                        Objects.isNull(request.getListOfCustomers())) {
                    errors.append("listOfCustomers-listOfCustomers must not be null when List of customers option is selected;");
                }
            }

            if (TypeOfFile.BANK_PARTNER.equals(request.getTypeOfFile()) && CollectionUtils.isEmpty(request.getBankIds()) && !request.isGlobalBank()) {
                errors.append("bankId-bankId or globalBank must not be null when type is Bank partner;");
            } else if ((!TypeOfFile.BANK_PARTNER.equals(request.getTypeOfFile()) || Objects.isNull(request.getDataSendingSchedule())) && Objects.nonNull(request.getNumberOfWorkingDays())) {
                errors.append("numberOfWorkingDays-number Of Working Days must be null when the Data sending schedule is not filled or type of file is not bank partner;");
            }

            if (Objects.nonNull(request.getNumberOfWorkingDays()) && Objects.isNull(request.getCalendarId())) {
                errors.append("calendarId-calendarId must not be null when Number of working date is filled;");
            } else if (Objects.isNull(request.getNumberOfWorkingDays()) && Objects.nonNull(request.getCalendarId())) {
                errors.append("calendarId-calendarId must be null when Number of working date is not filled;");
            }

            if (Objects.nonNull(request.getType())) {
                if (!request.getType().equals(CollectionChannelType.OFFLINE)) {
                    if (Objects.nonNull(request.getFolderForFileReceiving())) {
                        errors.append("folderForFileReceiving-folderForFileReceiving must be null when type is not offline;");
                    }

                    if (Objects.nonNull(request.getFolderForFileSending())) {
                        errors.append("folderForFileSending-folderForFileSending must be null when type is not offline;");
                    }

                    if (Objects.nonNull(request.getEmailForFileSending())) {
                        errors.append("emailForFileSending-emailForFileSending must be null when type is not offline;");
                    }

                    if (Objects.nonNull(request.getTypeOfFile())) {
                        errors.append("typeOfFile-typeOfFile must be null when type is Online;");
                    }

                    if (request.getBankIds() != null && !request.getBankIds().isEmpty()) {
                        errors.append("bankIds-bankIds must be empty when type is ONLINE;");
                    }
                } else if (request.getType().equals(CollectionChannelType.OFFLINE) && Objects.isNull(request.getTypeOfFile())) {
                    errors.append("typeOfFile-typeOfFile must not be null when type is Offline;");
                }
            }

            if (request.isGlobalBank() && CollectionUtils.isNotEmpty(request.getBankIds())) {
                errors.append("bankIds-bankIds must be null when globalBank is true;");
            }
            if (!skipPerformerValidation){
                if (request.getPerformerType() != null && request.getPerformerId() == null) {
                    errors.append("performer-Performer cannot be null;");
                } else if (request.getPerformerId() != null && request.getPerformerType() == null) {
                    errors.append("performerType-Performer Type cannot be null;");
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
