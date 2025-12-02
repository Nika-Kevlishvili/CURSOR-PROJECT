package bg.energo.phoenix.model.response.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.TypeOfFile;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Data
public class CollectionChannelResponse {
    private Long id;

    private String name;

    private CollectionChannelType type;

    private ShortResponse collectionPartnerId;

    private String numberOfIncomeAccount;

    private ShortResponse currencyId;

    private CustomerConditionType conditionType;

    private String condition;

    private List<ConditionInfo> conditionsInfo;

    private String listOfCustomers;

    private BigDecimal lessThan;

    private BigDecimal greaterThan;

    private TypeOfFile typeOfFile;

    private Boolean isGlobalBank;

    private String dataSendingSchedule;

    private String dataReceivingSchedule;

    private Integer numberOfWorkingDays;

    private ShortResponse calendarId;

    private Integer waitingPeriodToleranceInHours;

    private String folderForFileReceiving;

    private String folderForFileSending;

    private String emailForFileSending;

    private EntityStatus status;

    private List<ShortResponse> bankIds;

    private List<ShortResponse> excludeLiabilitiesByPrefix;

    private List<ShortResponse> priorityLiabilitiesByPrefix;

    private CollectionChannelEmployeeResponse employee;

    private Boolean combineLiabilities;

    public CollectionChannelResponse(CollectionChannel collectionChannel) {
        this.id = collectionChannel.getId();
        this.name = collectionChannel.getName();
        this.type = collectionChannel.getType();
        this.numberOfIncomeAccount = collectionChannel.getNumberOfIncomeAccount();
        this.conditionType = collectionChannel.getConditionType();
        this.condition = collectionChannel.getCondition();
        this.listOfCustomers = collectionChannel.getListOfCustomers();
        this.lessThan = collectionChannel.getLessThan();
        this.greaterThan = collectionChannel.getGreaterThan();
        this.typeOfFile = collectionChannel.getTypeOfFile();
        this.isGlobalBank = collectionChannel.getIsGlobalBank();
        this.dataSendingSchedule = collectionChannel.getDataSendingSchedule();
        this.dataReceivingSchedule = collectionChannel.getDataReceivingSchedule();
        this.numberOfWorkingDays = collectionChannel.getNumberOfWorkingDays();
        this.waitingPeriodToleranceInHours = collectionChannel.getWaitingPeriodToleranceInHours();
        this.folderForFileReceiving = collectionChannel.getFolderForFileReceiving();
        this.folderForFileSending = collectionChannel.getFolderForFileSending();
        this.emailForFileSending = collectionChannel.getEmailForFileSending();
        this.status = collectionChannel.getStatus();
        this.combineLiabilities = !Objects.isNull(collectionChannel.getCombineLiabilities()) && collectionChannel.getCombineLiabilities();
    }
}
