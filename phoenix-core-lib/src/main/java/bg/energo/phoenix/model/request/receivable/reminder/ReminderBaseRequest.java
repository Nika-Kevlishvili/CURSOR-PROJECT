package bg.energo.phoenix.model.request.receivable.reminder;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionType;
import bg.energo.phoenix.model.enums.receivable.reminder.TriggerForLiabilities;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReminderBaseRequest {
    @NotNull(message = "triggerForLiabilities-[triggerForLiabilities] should not be null;")
    private TriggerForLiabilities triggerForLiabilities;

    private Integer postponementInDays;

    @DecimalMin(value = "0.01", message = "dueAmountFrom-dueAmountFrom From minimum value is [{value}];")
    @DecimalMax(value = "9999999999.99", message = "dueAmountFrom-dueAmountFrom From maximum value if [{value}];")
    @Digits(integer = 10, fraction = 2, message = "dueAmountFrom-dueAmountFrom Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal dueAmountFrom;

    @DecimalMin(value = "0.01", message = "dueAmountTo-dueAmountTo To minimum value is [{value}];")
    @DecimalMax(value = "9999999999.99", message = "dueAmountTo-dueAmountTo Amount To maximum value if [{value}];")
    @Digits(integer = 10, fraction = 2, message = "dueAmountTo-dueAmountTo Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal dueAmountTo;

    private Long currencyId;

    @DuplicatedValuesValidator(fieldPath = "excludeLiabilitiesByPrefixes")
    private List<Long> excludeLiabilitiesByPrefixes;

    @DuplicatedValuesValidator(fieldPath = "onlyLiabilitiesWithPrefixes")
    private List<Long> onlyLiabilitiesWithPrefixes;

    @NotNull(message = "conditionType-[conditionType] should not be null;")
    private ReminderConditionType conditionType;

    private String conditions;

    @Size(min = 1, max = 120000, message = "listOfCustomers-[listOfCustomers] size should be between {min} and {max} characters;")
    @Pattern(regexp= "^[\\w–/-]+(,[\\w–/-]+)*$",message="listOfCustomers-[listOfCustomers] invalid format of customers;")
    private String listOfCustomers;

    @NotEmpty(message = "communicationChannels-[communicationChannels] should not be null or empty")
    private List<CommunicationChannel> communicationChannels;

    @NotNull(message = "purposeOfTheContactId-[purposeOfTheContactId] should not be null;")
    private Long purposeOfTheContactId;

    @DuplicatedValuesValidator(fieldPath = "periodicityIds")
    @NotNull(message = "periodicityIds-[periodicityIds] should not be null or empty;")
    private List<Long> periodicityIds;

    @DuplicatedValuesValidator(fieldPath = "templatesForReminderLetter")
    private List<Long> templatesForReminderLetter;

    private Long emailTemplateId;

    private Long smsTemplateId;

    private Long documentTemplateId;
}
