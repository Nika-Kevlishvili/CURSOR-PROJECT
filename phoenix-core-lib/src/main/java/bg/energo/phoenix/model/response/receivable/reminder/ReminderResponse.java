package bg.energo.phoenix.model.response.receivable.reminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionType;
import bg.energo.phoenix.model.enums.receivable.reminder.TriggerForLiabilities;
import bg.energo.phoenix.model.response.nomenclature.customer.ContactPurposeShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ConditionInfoShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReminderResponse {
    private Long id;

    private String number;

    private TriggerForLiabilities triggerForLiabilities;

    private Integer postponementInDays;

    private BigDecimal dueAmountFrom;

    private BigDecimal dueAmountTo;

    private CurrencyShortResponse currency;

    private ReminderConditionType customerConditionType;

    private String conditions;

    private List<ConditionInfoShortResponse> conditionsInfo;

    private String listOfCustomers;

    private List<PrefixesShortResponse> excludeLiabilitiesByPrefixes;

    private List<PrefixesShortResponse> onlyLiabilitiesWithPrefixes;

    private List<CommunicationChannel> communicationChannels;

    private List<ShortResponse> periodicityIds;

    private ContactPurposeShortResponse contactPurpose;

    private EntityStatus status;

    private ContractTemplateShortResponse documentTemplateResponse;

    private ContractTemplateShortResponse smsTemplateResponse;

    private ContractTemplateShortResponse emailTemplateResponse;
}
