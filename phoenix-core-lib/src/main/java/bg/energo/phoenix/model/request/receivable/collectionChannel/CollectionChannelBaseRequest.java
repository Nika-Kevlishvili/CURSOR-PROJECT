package bg.energo.phoenix.model.request.receivable.collectionChannel;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.product.applicationModel.RRuleValidator;
import bg.energo.phoenix.model.customAnotations.receivable.collectionChannel.CollectionChannelBaseRequestValidator;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.TypeOfFile;
import bg.energo.phoenix.model.enums.task.PerformerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
@CollectionChannelBaseRequestValidator
public class CollectionChannelBaseRequest {

    @NotBlank(message = "name-name must not blank;")
    @Size(min = 1, max = 512, message = "name-name should be between {min} and {max} characters;")
    private String name;

    @NotNull(message = "type-type must not be null;")
    private CollectionChannelType type;

    private Long performerId;
    private PerformerType performerType;

    @NotNull(message = "collectionPartnerId-collectionPartnerId must not be null;")
    private Long collectionPartnerId;

    @NotBlank(message = "numberOfIncomeAccount-numberOfIncomeAccount must not be null;")
    @Size(min = 1, max = 512, message = "numberOfIncomeAccount-number Of Income Account should be between {min} and {max} characters;")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "numberOfIncomeAccount-Allowed symbols in number Of Income Account are: A-Z a-z 0-9;")
    private String numberOfIncomeAccount;

    @NotNull(message = "currencyId-currencyId must not be null;")
    private Long currencyId;

    @NotNull(message = "customerConditionType-customerConditionType must not be null;")
    private CustomerConditionType customerConditionType;

    private String condition;

    @Size(min = 1, max = 8196, message = "listOfCustomers-List of Customers should be between {min} and {max} characters.")
    private String listOfCustomers;

    @DuplicatedValuesValidator(fieldPath = "prefixNomenclatureIds")
    private List<Long> excludeLiabilitiesByPrefix;

    private @Valid ExcludeLiabilitiesByAmount excludeLiabilitiesByAmount;

    @DuplicatedValuesValidator(fieldPath = "prefixNomenclatureIds")
    private List<Long> priorityLiabilitiesByPrefix;

    private TypeOfFile typeOfFile;

    private List<Long> bankIds;

    private boolean globalBank;

    @Size(min = 1, max = 2048, message = "dataSendingSchedule-data Sending Schedule should be between {min} and {max} characters;")
    @RRuleValidator
    private String dataSendingSchedule;

    @Size(min = 1, max = 2048, message = "dataReceivingSchedule-data Receiving Schedule should be between {min} and {max} characters;")
    @RRuleValidator
    private String dataReceivingSchedule;

    @Max(value = 999, message = "numberOfWorkingDays-Number of working days for waiting payment should be max {value};")
    @Min(value = 1, message = "numberOfWorkingDays-Number of working days for waiting payment should be min {value};")
    private Integer numberOfWorkingDays;

    private Long calendarId;

    @Max(value = 99, message = "waitingPeriodToleranceInHours-Waiting period tolerance in hours should be max {value};")
    @Min(value = 1, message = "waitingPeriodToleranceInHours-Waiting period tolerance in hours should be min {value};")
    private Integer waitingPeriodToleranceInHours;

    @Size(min = 1, max = 2048, message = "folderForFileReceiving-folder For File Receiving should be between {min} and {max} characters;")
    private String folderForFileReceiving;

    @Size(min = 1, max = 2048, message = "folderForFileSending-folder For File Sending should be between {min} and {max} characters;")
    private String folderForFileSending;

    @Size(min = 1, max = 2048, message = "emailForFileSending-email For File Sending should be between {min} and {max} characters;")
    private String emailForFileSending;

    private Boolean combineLiabilities;

}
