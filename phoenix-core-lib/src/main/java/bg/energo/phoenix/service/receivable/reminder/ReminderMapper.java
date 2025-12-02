package bg.energo.phoenix.service.receivable.reminder;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.reminder.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.request.receivable.reminder.ReminderCreateRequest;
import bg.energo.phoenix.model.request.receivable.reminder.ReminderEditRequest;
import bg.energo.phoenix.model.response.nomenclature.customer.ContactPurposeShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse;
import bg.energo.phoenix.model.response.receivable.reminder.ReminderResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.repository.nomenclature.customer.ContactPurposeRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.reminder.ExcludeLiabilitiesByPrefixRepository;
import bg.energo.phoenix.repository.receivable.reminder.OnlyLiabilitiesWithPrefixRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderPeriodicityRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReminderMapper {

    private final CurrencyRepository currencyRepository;
    private final ContactPurposeRepository contactPurposeRepository;
    private final ReminderPeriodicityRepository reminderPeriodicityRepository;
    private final OnlyLiabilitiesWithPrefixRepository onlyLiabilitiesWithPrefixRepository;
    private final ExcludeLiabilitiesByPrefixRepository excludeLiabilitiesByPrefixRepository;

    public Reminder fromCreateRequestToEntity(ReminderCreateRequest request) {
        return Reminder
                .builder()
                .triggerForLiabilities(request.getTriggerForLiabilities())
                .postponementInDays(request.getPostponementInDays())
                .dueAmountFrom(request.getDueAmountFrom())
                .dueAmountTo(request.getDueAmountTo())
                .customerConditionType(request.getConditionType())
                .communicationChannels(CollectionUtils.isEmpty(request.getCommunicationChannels()) ? List.of(CommunicationChannel.EMAIL) : request.getCommunicationChannels())
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public ReminderPeriodicity relateToReminderPeriodicity(Long periodicityId, Long reminderId) {
        return ReminderPeriodicity
                .builder()
                .reminderId(reminderId)
                .periodicityId(periodicityId)
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public ExcludeLiabilitiesByPrefix relateToExcludeLiabilitiesByPrefix(Long prefixId, Long reminderId) {
        return ExcludeLiabilitiesByPrefix
                .builder()
                .reminderId(reminderId)
                .prefixId(prefixId)
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public OnlyLiabilitiesWithPrefix relateToOnlyLiabilitiesWithPrefix(Long prefixId, Long reminderId) {
        return OnlyLiabilitiesWithPrefix
                .builder()
                .reminderId(reminderId)
                .prefixId(prefixId)
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public ReminderLetterTemplate relateToReminderLetterTemplate(Long templateId, Long reminderId) {
        return ReminderLetterTemplate
                .builder()
                .reminderId(reminderId)
                .templateId(templateId)
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public ReminderResponse fromEntityToPreviewResponse(Reminder reminder) {
        return ReminderResponse
                .builder()
                .id(reminder.getId())
                .number(reminder.getNumber())
                .triggerForLiabilities(reminder.getTriggerForLiabilities())
                .postponementInDays(reminder.getPostponementInDays())
                .dueAmountFrom(reminder.getDueAmountFrom())
                .dueAmountTo(reminder.getDueAmountTo())
                .currency(fetchCurrencyAndMapToResponse(reminder.getCurrencyId()))
                .onlyLiabilitiesWithPrefixes(fetchOnlyLiabilitiesPrefixesAndMapToResponse(reminder.getId()))
                .excludeLiabilitiesByPrefixes(fetchExcludeLiabilitiesPrefixesAndMapToResponse(reminder.getId()))
                .customerConditionType(reminder.getCustomerConditionType())
                .listOfCustomers(reminder.getListOfCustomers())
                .communicationChannels(reminder.getCommunicationChannels())
                .contactPurpose(fetchContactPurposeAndMapToResponse(reminder.getContactPurposeId()))
                .periodicityIds(fetchPeriodicity(reminder.getId()))
                .status(reminder.getStatus())
                .build();
    }

    private CurrencyShortResponse fetchCurrencyAndMapToResponse(Long currencyId) {
        Optional<Currency> currencyOptional = currencyRepository
                .findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE, NomenclatureItemStatus.DELETED));

        return currencyOptional
                .map(CurrencyShortResponse::new)
                .orElse(null);
    }

    private ContactPurposeShortResponse fetchContactPurposeAndMapToResponse(Long contactPurposeId) {
        Optional<ContactPurpose> contactPurposeOptional = contactPurposeRepository
                .findByIdAndStatuses(contactPurposeId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE, NomenclatureItemStatus.DELETED));

        return contactPurposeOptional
                .map(ContactPurposeShortResponse::new)
                .orElse(null);
    }

    private List<PrefixesShortResponse> fetchOnlyLiabilitiesPrefixesAndMapToResponse(Long reminderId) {
        Optional<List<PrefixesShortResponse>> onlyLiabilitiesPrefixesOptional = onlyLiabilitiesWithPrefixRepository
                .findOnlyLiabilitiesPrefixesByReminderId(reminderId, EntityStatus.ACTIVE);

        return onlyLiabilitiesPrefixesOptional.orElse(null);
    }

    private List<PrefixesShortResponse> fetchExcludeLiabilitiesPrefixesAndMapToResponse(Long reminderId) {
        Optional<List<PrefixesShortResponse>> excludeLiabilitiesPrefixesOptional = excludeLiabilitiesByPrefixRepository
                .findExcludeLiabilitiesPrefixesByReminderId(reminderId, EntityStatus.ACTIVE);

        return excludeLiabilitiesPrefixesOptional.orElse(null);
    }

    private List<ShortResponse> fetchPeriodicity(Long reminderId) {
        Optional<List<ShortResponse>> reminderPeriodicityOptional = reminderPeriodicityRepository.findIdsByReminderIdAndStatus(reminderId, EntityStatus.ACTIVE);
        return reminderPeriodicityOptional.orElse(null);
    }

    public Reminder fromEditRequestToEntity(ReminderEditRequest request, Reminder reminder) {
        reminder.setTriggerForLiabilities(request.getTriggerForLiabilities());
        reminder.setPostponementInDays(request.getPostponementInDays());
        reminder.setDueAmountFrom(request.getDueAmountFrom());
        reminder.setDueAmountTo(request.getDueAmountTo());
        reminder.setCustomerConditionType(request.getConditionType());
        reminder.setCommunicationChannels(CollectionUtils.isEmpty(request.getCommunicationChannels()) ? List.of(CommunicationChannel.EMAIL) : request.getCommunicationChannels());
        reminder.setStatus(EntityStatus.ACTIVE);
        return reminder;
    }

}
