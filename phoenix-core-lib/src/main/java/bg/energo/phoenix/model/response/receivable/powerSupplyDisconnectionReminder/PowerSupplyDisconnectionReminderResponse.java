package bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PowerSupplyDisconnectionReminderResponse {

    private Long id;

    private String reminderNumber;

    private PowerSupplyDisconnectionReminderStatus reminderStatus;

    private LocalDateTime customerSendDate;

    private BigDecimal liabilityAmountFrom;

    private BigDecimal liabilityAmountTo;

    private ShortResponse currencyResponse;

    private LocalDate liabilitiesMaxDueDate;

    private LocalDate disconnectionDate;

    private String excludedCustomerList;

    private EntityStatus status;

    private LocalDateTime creationDate;

    private List<PowerSupplyDcnReminderDocFileResponse> subFiles;

    private List<TaskShortResponse> tasks;

    private List<CommunicationChannel> communicationChannels;

    private ContractTemplateShortResponse documentTemplateResponse;

    private ContractTemplateShortResponse smsTemplateResponse;

    private ContractTemplateShortResponse emailTemplateResponse;

}
