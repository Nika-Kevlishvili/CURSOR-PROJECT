package bg.energo.phoenix.service.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlocking;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlockingExclusionPrefix;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.request.receivable.massOperationForBlocking.ExclusionByAmountRequest;
import bg.energo.phoenix.model.request.receivable.massOperationForBlocking.ReceivableBlockingCreateRequest;
import bg.energo.phoenix.model.request.receivable.massOperationForBlocking.ReceivableBlockingEditRequest;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.BlockingForBaseResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.BlockingReasonShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.PrefixesShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ReceivableBlockingResponse;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingExclusionPrefixesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReceivableBlockingMapper {

    private final CurrencyRepository currencyRepository;
    private final BlockingReasonRepository blockingReasonRepository;
    private final ReceivableBlockingExclusionPrefixesRepository exclusionPrefixesRepository;

    public ReceivableBlocking fromCreateRequestToEntity(ReceivableBlockingCreateRequest request) {

        ExclusionByAmountRequest exclusionByAmount = request.getExclusionByAmount();

        return ReceivableBlocking
                .builder()
                .name(request.getName().trim())
                .status(EntityStatus.ACTIVE)
                .customerConditions(request.getConditions())
                .blockingTypes(request.getReceivableBlockingTypes())
                .blockingConditionType(request.getReceivableBlockingConditionType())
                .lessThan(exclusionByAmount == null ? null : exclusionByAmount.getLessThan())
                .greaterThan(exclusionByAmount == null ? null : exclusionByAmount.getGreaterThan())
                .build();
    }

    public ReceivableBlocking fromEditRequestToEntity(ReceivableBlockingEditRequest request, ReceivableBlocking receivableBlocking) {
        ExclusionByAmountRequest exclusionByAmount = request.getExclusionByAmount();
        receivableBlocking.setName(receivableBlocking.getName().trim());
        receivableBlocking.setCustomerConditions(request.getConditions());
        receivableBlocking.setBlockingTypes(request.getReceivableBlockingTypes());
        receivableBlocking.setBlockingConditionType(request.getReceivableBlockingConditionType());
        receivableBlocking.setLessThan(exclusionByAmount == null ? null : exclusionByAmount.getLessThan());
        receivableBlocking.setGreaterThan(exclusionByAmount == null ? null : exclusionByAmount.getGreaterThan());
        return receivableBlocking;
    }

    public void fromBlockingReasonRequestToEntity(ReceivableBlocking receivableBlocking,
                                                  ReceivableBlockingReasonType reasonType,
                                                  LocalDate toDate,
                                                  LocalDate fromDate,
                                                  String additionalInformation,
                                                  Long reasonId
    ) {
        switch (reasonType) {
            case BLOCKED_FOR_PAYMENT -> {
                receivableBlocking.setBlockedForPayment(true);
                receivableBlocking.setBlockedForPaymentReasonId(reasonId);
                receivableBlocking.setBlockedForPaymentToDate(toDate);
                receivableBlocking.setBlockedForPaymentFromDate(fromDate);
                receivableBlocking.setBlockedForPaymentAdditionalInfo(additionalInformation);
            }
            case BLOCKED_FOR_REMINDER_LETTERS -> {
                receivableBlocking.setBlockedForReminderLetters(true);
                receivableBlocking.setBlockedForReminderLettersReasonId(reasonId);
                receivableBlocking.setBlockedForReminderLettersToDate(toDate);
                receivableBlocking.setBlockedForReminderLettersFromDate(fromDate);
                receivableBlocking.setBlockedForReminderLettersAdditionalInfo(additionalInformation);
            }
            case BLOCKED_FOR_SUPPLY_TERMINATION -> {
                receivableBlocking.setBlockedForSupplyTermination(true);
                receivableBlocking.setBlockedForSupplyTerminationReasonId(reasonId);
                receivableBlocking.setBlockedForSupplyTerminationToDate(toDate);
                receivableBlocking.setBlockedForSupplyTerminationFromDate(fromDate);
                receivableBlocking.setBlockedForSupplyTerminationAdditionalInfo(additionalInformation);
            }
            case BLOCKED_FOR_LIABILITIES_OFFSETTING -> {
                receivableBlocking.setBlockedForLiabilitiesOffsetting(true);
                receivableBlocking.setBlockedForLiabilitiesOffsettingReasonId(reasonId);
                receivableBlocking.setBlockedForLiabilitiesOffsettingToDate(toDate);
                receivableBlocking.setBlockedForLiabilitiesOffsettingFromDate(fromDate);
                receivableBlocking.setBlockedForLiabilitiesOffsettingAdditionalInfo(additionalInformation);
            }
            case BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS -> {
                receivableBlocking.setBlockedForCalculations(true);
                receivableBlocking.setBlockedForCalculationsReasonId(reasonId);
                receivableBlocking.setBlockedForCalculationsToDate(toDate);
                receivableBlocking.setBlockedForCalculationsFromDate(fromDate);
                receivableBlocking.setBlockedForCalculationsAdditionalInfo(additionalInformation);
            }
        }
    }

    public void removeBlockingReasonByType(ReceivableBlocking receivableBlocking,
                                           ReceivableBlockingReasonType reasonType
    ) {
        switch (reasonType) {
            case BLOCKED_FOR_PAYMENT -> {
                receivableBlocking.setBlockedForPayment(false);
                receivableBlocking.setBlockedForPaymentReasonId(null);
                receivableBlocking.setBlockedForPaymentToDate(null);
                receivableBlocking.setBlockedForPaymentFromDate(null);
                receivableBlocking.setBlockedForPaymentAdditionalInfo(null);
            }
            case BLOCKED_FOR_REMINDER_LETTERS -> {
                receivableBlocking.setBlockedForReminderLetters(false);
                receivableBlocking.setBlockedForReminderLettersReasonId(null);
                receivableBlocking.setBlockedForReminderLettersToDate(null);
                receivableBlocking.setBlockedForReminderLettersFromDate(null);
                receivableBlocking.setBlockedForReminderLettersAdditionalInfo(null);
            }
            case BLOCKED_FOR_SUPPLY_TERMINATION -> {
                receivableBlocking.setBlockedForSupplyTermination(false);
                receivableBlocking.setBlockedForSupplyTerminationReasonId(null);
                receivableBlocking.setBlockedForSupplyTerminationToDate(null);
                receivableBlocking.setBlockedForSupplyTerminationFromDate(null);
                receivableBlocking.setBlockedForSupplyTerminationAdditionalInfo(null);
            }
            case BLOCKED_FOR_LIABILITIES_OFFSETTING -> {
                receivableBlocking.setBlockedForLiabilitiesOffsetting(false);
                receivableBlocking.setBlockedForLiabilitiesOffsettingReasonId(null);
                receivableBlocking.setBlockedForLiabilitiesOffsettingToDate(null);
                receivableBlocking.setBlockedForLiabilitiesOffsettingFromDate(null);
                receivableBlocking.setBlockedForLiabilitiesOffsettingAdditionalInfo(null);
            }
            case BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS -> {
                receivableBlocking.setBlockedForCalculations(false);
                receivableBlocking.setBlockedForCalculationsReasonId(null);
                receivableBlocking.setBlockedForCalculationsToDate(null);
                receivableBlocking.setBlockedForCalculationsFromDate(null);
                receivableBlocking.setBlockedForCalculationsAdditionalInfo(null);
            }
        }
    }

    public ReceivableBlockingExclusionPrefix toRelateExclusionPrefix(Long prefixId, Long receivableBlockingId) {
        return ReceivableBlockingExclusionPrefix
                .builder()
                .receivableBlockingId(receivableBlockingId)
                .prefixId(prefixId)
                .status(ReceivableSubObjectStatus.ACTIVE)
                .build();
    }

    public ReceivableBlockingResponse fromEntityToPreviewResponse(ReceivableBlocking receivableBlocking) {
        return ReceivableBlockingResponse
                .builder()
                .id(receivableBlocking.getId())
                .name(receivableBlocking.getName())
                .blockingTypes(receivableBlocking.getBlockingTypes())
                .prefixes(fetchPrefixesAndMapToResponse(receivableBlocking.getId()))
                .blockingStatus(receivableBlocking.getBlockingStatus())
                .status(receivableBlocking.getStatus())
                .blockingConditionType(receivableBlocking.getBlockingConditionType())
                .listOfCustomers(receivableBlocking.getListOfCustomers())
                .lessThan(receivableBlocking.getLessThan())
                .greaterThan(receivableBlocking.getGreaterThan())
                .currency(fetchCurrencyAndMapToResponse(receivableBlocking.getCurrencyId()))
                .isBlockingForPayment(receivableBlocking.getBlockedForPayment())
                .blockingForPayment(
                        new BlockingForBaseResponse(
                                receivableBlocking.getBlockedForPaymentFromDate(),
                                receivableBlocking.getBlockedForPaymentToDate(),
                                fetchBlockingReasonAndMapToResponse(receivableBlocking.getBlockedForPaymentReasonId()),
                                receivableBlocking.getBlockedForPaymentAdditionalInfo()
                        )
                )
                .isBlockForReminderLetters(receivableBlocking.getBlockedForReminderLetters())
                .blockingForReminderLetters(
                        new BlockingForBaseResponse(
                                receivableBlocking.getBlockedForReminderLettersFromDate(),
                                receivableBlocking.getBlockedForReminderLettersToDate(),
                                fetchBlockingReasonAndMapToResponse(receivableBlocking.getBlockedForReminderLettersReasonId()),
                                receivableBlocking.getBlockedForReminderLettersAdditionalInfo()
                        )
                )
                .isBlockForCalculation(receivableBlocking.getBlockedForCalculations())
                .blockingForCalculation(
                        new BlockingForBaseResponse(
                                receivableBlocking.getBlockedForCalculationsFromDate(),
                                receivableBlocking.getBlockedForCalculationsToDate(),
                                fetchBlockingReasonAndMapToResponse(receivableBlocking.getBlockedForCalculationsReasonId()),
                                receivableBlocking.getBlockedForCalculationsAdditionalInfo()
                        )
                )
                .isBlockForLiabilitiesOffsetting(receivableBlocking.getBlockedForLiabilitiesOffsetting())
                .blockingForLiabilitiesOffsetting(
                        new BlockingForBaseResponse(
                                receivableBlocking.getBlockedForLiabilitiesOffsettingFromDate(),
                                receivableBlocking.getBlockedForLiabilitiesOffsettingToDate(),
                                fetchBlockingReasonAndMapToResponse(receivableBlocking.getBlockedForLiabilitiesOffsettingReasonId()),
                                receivableBlocking.getBlockedForLiabilitiesOffsettingAdditionalInfo()
                        )
                )
                .isBlockForSupplyTermination(receivableBlocking.getBlockedForSupplyTermination())
                .blockingForSupplyTermination(
                        new BlockingForBaseResponse(
                                receivableBlocking.getBlockedForSupplyTerminationFromDate(),
                                receivableBlocking.getBlockedForSupplyTerminationToDate(),
                                fetchBlockingReasonAndMapToResponse(receivableBlocking.getBlockedForSupplyTerminationReasonId()),
                                receivableBlocking.getBlockedForSupplyTerminationAdditionalInfo()
                        )
                )
                .build();
    }

    private CurrencyShortResponse fetchCurrencyAndMapToResponse(Long currencyId) {
        Optional<Currency> currencyOptional = currencyRepository
                .findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE, NomenclatureItemStatus.DELETED));

        return currencyOptional
                .map(CurrencyShortResponse::new)
                .orElse(null);
    }

    private BlockingReasonShortResponse fetchBlockingReasonAndMapToResponse(Long reasonId) {
        Optional<BlockingReason> blockingReasonOptional = blockingReasonRepository
                .findByIdAndStatus(reasonId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE, NomenclatureItemStatus.DELETED));

        return blockingReasonOptional
                .map(blockingReason -> new BlockingReasonShortResponse(blockingReason.getId(), blockingReason.getName()))
                .orElse(null);
    }

    private List<PrefixesShortResponse> fetchPrefixesAndMapToResponse(Long blockingId) {
        Optional<List<PrefixesShortResponse>> exclusionPrefixOptional = exclusionPrefixesRepository.findPrefixesByBlockingId(blockingId, ReceivableSubObjectStatus.ACTIVE);
        return exclusionPrefixOptional.orElse(Collections.emptyList());
    }

}
