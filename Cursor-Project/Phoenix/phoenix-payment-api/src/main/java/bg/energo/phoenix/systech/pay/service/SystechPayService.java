package bg.energo.phoenix.systech.pay.service;

import bg.energo.phoenix.payment.PaymentLpfChannelConverter;
import bg.energo.phoenix.systech.liabilities.repository.SystechLiabilitiesProjection;
import bg.energo.phoenix.systech.liabilities.repository.SystechLiabilitiesRepository;
import bg.energo.phoenix.systech.liabilities.service.SystechOnlinePaymentCheckClient;
import bg.energo.phoenix.systech.pay.api.SystechPayResponse;
import bg.energo.phoenix.systech.pay.entities.SystechPaymentEntity;
import bg.energo.phoenix.systech.pay.repository.SystechPaymentRepository;
import bg.energo.phoenix.systech.receipt.entities.SystechReceiptEntity;
import bg.energo.phoenix.systech.receipt.repository.SystechReceiptRepository;
import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.service.BillingGroupHelperService;
import bg.energo.phoenix.service.PaymentCreationTransactionalService;
import bg.energo.phoenix.service.PaymentOffsettingTransactionalService;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SystechPayService {

    private static final String COLLECTION_CHANNEL_NAME = "Systech";

    private static final String MSG_INVALID_TID = "Невалиден номер на транзакция.";
    private static final String MSG_PAYMENT_ERROR = "Грешка при приемане на плащането.";
    private static final String MSG_SYSTEM_ERROR = "Системна грешка";

    private final SystechReceiptRepository receiptRepository;
    private final SystechPaymentRepository paymentRepository;
    private final SystechLiabilitiesRepository liabilitiesRepository;
    private final CollectionChannelRepository collectionChannelRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final PaymentCreationTransactionalService paymentCreationTransactionalService;
    private final PaymentOffsettingTransactionalService paymentOffsettingTransactionalService;
    private final BillingGroupHelperService billingGroupHelperService;
    private final SystechOnlinePaymentCheckClient onlinePaymentCheckClient;
    private final ObjectMapper objectMapper;

    public SystechPayService(
            SystechReceiptRepository receiptRepository,
            SystechPaymentRepository paymentRepository,
            SystechLiabilitiesRepository liabilitiesRepository,
            CollectionChannelRepository collectionChannelRepository,
            CurrencyRepository currencyRepository,
            CustomerRepository customerRepository,
            CustomerLiabilityRepository customerLiabilityRepository,
            PaymentCreationTransactionalService paymentCreationTransactionalService,
            PaymentOffsettingTransactionalService paymentOffsettingTransactionalService,
            BillingGroupHelperService billingGroupHelperService,
            SystechOnlinePaymentCheckClient onlinePaymentCheckClient,
            ObjectMapper objectMapper
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository");
        this.liabilitiesRepository = Objects.requireNonNull(liabilitiesRepository, "liabilitiesRepository");
        this.collectionChannelRepository = Objects.requireNonNull(collectionChannelRepository, "collectionChannelRepository");
        this.currencyRepository = Objects.requireNonNull(currencyRepository, "currencyRepository");
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
        this.customerLiabilityRepository = Objects.requireNonNull(customerLiabilityRepository, "customerLiabilityRepository");
        this.paymentCreationTransactionalService = Objects.requireNonNull(paymentCreationTransactionalService, "paymentCreationTransactionalService");
        this.paymentOffsettingTransactionalService = Objects.requireNonNull(paymentOffsettingTransactionalService, "paymentOffsettingTransactionalService");
        this.billingGroupHelperService = Objects.requireNonNull(billingGroupHelperService, "billingGroupHelperService");
        this.onlinePaymentCheckClient = Objects.requireNonNull(onlinePaymentCheckClient, "onlinePaymentCheckClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public SystechPayResponse pay(String tidRaw) {
        try {
            String tid = trimToNull(tidRaw);
            if (tid == null || tid.length() > 64) {
                return SystechPayResponse.error(2, MSG_INVALID_TID);
            }

            Optional<SystechReceiptEntity> receiptOpt = receiptRepository.findByTid(tid);
            if (receiptOpt.isEmpty()) {
                return SystechPayResponse.error(2, MSG_INVALID_TID);
            }
            SystechReceiptEntity receipt = receiptOpt.get();

            if (paymentRepository.existsByTid(tid)) {
                return SystechPayResponse.success();
            }

            Optional<CollectionChannel> channelOpt = collectionChannelRepository.findCollectionChanel(COLLECTION_CHANNEL_NAME);
            if (channelOpt.isEmpty()) {
                return SystechPayResponse.error(4, MSG_SYSTEM_ERROR);
            }
            CollectionChannel channel = channelOpt.get();
            Currency channelCurrency = resolveChannelCurrency(channel);
            if (channelCurrency == null) {
                return SystechPayResponse.error(4, MSG_SYSTEM_ERROR);
            }
            Long mainCurrencyId = resolveMainCurrencyId();
            if (mainCurrencyId == null) {
                return SystechPayResponse.error(4, MSG_SYSTEM_ERROR);
            }

            String customerNumber = receipt.getCustomerNumber();
            if (!liabilitiesRepository.existsValidCustomerNumber(customerNumber)) {
                return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
            }

            ParsedCustomerNumber parsed = parseCustomerNumber(customerNumber);
            Customer customer = resolveCustomer(parsed.customerNumber10());
            if (customer == null) {
                return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
            }

            List<String> requestedDocuments = splitDocumentNumbers(receipt.getDocumentNumberRaw());
            if (requestedDocuments.isEmpty()) {
                return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
            }

            List<SystechLiabilitiesProjection> eligible = liabilitiesRepository.findEligibleLiabilities(
                    channel.getId(), customerNumber
            );
            if (eligible == null || eligible.isEmpty()) {
                return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
            }

            BigDecimal receiptTotal = (receipt.getPrincipal() != null ? receipt.getPrincipal() : BigDecimal.ZERO)
                    .add(receipt.getInterest() != null ? receipt.getInterest() : BigDecimal.ZERO);

            List<SystechLiabilitiesProjection> matched = new ArrayList<>();
            for (String doc : requestedDocuments) {
                List<SystechLiabilitiesProjection> candidates = eligible.stream()
                        .filter(p -> matchesDoc(p, doc))
                        .toList();
                SystechLiabilitiesProjection found = resolveDocumentMatchForPay(
                        candidates,
                        doc,
                        receiptTotal,
                        requestedDocuments.size(),
                        tid,
                        channelCurrency,
                        mainCurrencyId
                );
                if (found == null) {
                    log.warn("Document {} not matched in eligible liabilities for TID {}", doc, tid);
                    return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
                }
                matched.add(found);
                eligible.remove(found);
            }

            if (requestedDocuments.size() > 1) {
                Boolean combineLiabilities = channel.getCombineLiabilities();
                if (combineLiabilities == null || !combineLiabilities) {
                    log.warn("Multiple documents but combineLiabilities is not enabled for TID {}", tid);
                    return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
                }
            }

            BigDecimal currentPrincipal = matched.stream()
                    .map(SystechLiabilitiesProjection::getCurrentAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal receiptPrincipal = receipt.getPrincipal();
            if (receiptPrincipal == null || currentPrincipal.compareTo(receiptPrincipal) != 0) {
                log.warn("Principal mismatch for TID {}: current={}, receipt={}", tid, currentPrincipal, receiptPrincipal);
                return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
            }

            BigDecimal currentInterest = calculateInterest(matched, channelCurrency, mainCurrencyId);
            BigDecimal receiptInterest = receipt.getInterest() != null ? receipt.getInterest() : BigDecimal.ZERO;
            if (currentInterest.compareTo(receiptInterest) != 0) {
                log.warn("Interest mismatch for TID {}: current={}, receipt={}", tid, currentInterest, receiptInterest);
                return SystechPayResponse.error(3, MSG_PAYMENT_ERROR);
            }

            LocalDate payDate = LocalDate.now(ZoneId.of("Europe/Sofia"));
            BigDecimal totalAmount = currentPrincipal.add(currentInterest);
            Long totalAmountInCoins = EPBPaymentCalculationUtils.convertToCoinAmount(totalAmount);

            Long contractBillingGroupId = resolveContractBillingGroupId(parsed.billingGroup4(), matched);

            Long paymentId = paymentCreationTransactionalService.createOnlinePayment(
                    totalAmountInCoins,
                    customer,
                    payDate,
                    billingGroupHelperService.fetchAccountPeriodId(payDate.atStartOfDay()),
                    channel,
                    contractBillingGroupId
            );
            if (paymentId == null || paymentId <= 0) {
                log.error("Failed to create payment for TID {}", tid);
                return SystechPayResponse.error(4, MSG_SYSTEM_ERROR);
            }

            log.info("Payment created for TID {} with paymentId {}", tid, paymentId);

            SystechPaymentEntity paymentEntity = SystechPaymentEntity.builder()
                    .tid(tid)
                    .receiptId(receipt.getId())
                    .paymentId(paymentId)
                    .amount(totalAmount)
                    .principal(currentPrincipal)
                    .interest(currentInterest)
                    .payDate(OffsetDateTime.now())
                    .build();
            paymentRepository.save(paymentEntity);

            List<Long> matchedLiabilityIds = matched.stream()
                    .map(SystechLiabilitiesProjection::getLiabilityId)
                    .toList();

            CompletableFuture.runAsync(() -> performOffsetting(
                    paymentId,
                    channel.getId(),
                    channel.getCurrencyId(),
                    customer.getId(),
                    parsed.billingGroup4(),
                    contractBillingGroupId,
                    matchedLiabilityIds,
                    payDate
            ));

            return SystechPayResponse.success();

        } catch (Exception e) {
            log.error("SystechPayService.pay", e);
            return SystechPayResponse.error(4, MSG_SYSTEM_ERROR);
        }
    }

    private void performOffsetting(
            Long paymentId,
            Long collectionChannelId,
            Long currencyId,
            Long customerId,
            String billingGroupNumber,
            Long contractBillingGroupId,
            List<Long> matchedLiabilityIds,
            LocalDate payDate
    ) {
        try {
            log.info("Starting offsetting for paymentId {} with {} liabilities", paymentId, matchedLiabilityIds.size());
            LocalDateTime offsetDate = payDate.atStartOfDay();
            ConcurrentHashMap<Long, BigDecimal> receivableAmounts = new ConcurrentHashMap<>();

            List<CheckLiabilityDetails> liveLiabilities = onlinePaymentCheckClient.check(
                    collectionChannelId, customerId, billingGroupNumber, payDate
            );

            Map<Long, CheckLiabilityDetails> liabilityMap = new HashMap<>();
            for (CheckLiabilityDetails detail : liveLiabilities) {
                liabilityMap.put(detail.getLiabilityId(), detail);
            }

            for (Long liabilityId : matchedLiabilityIds) {
                CheckLiabilityDetails current = liabilityMap.get(liabilityId);
                if (current == null) {
                    log.warn("Liability {} not found during offsetting for paymentId {}", liabilityId, paymentId);
                    continue;
                }

                Boolean principalCovered = paymentOffsettingTransactionalService.offsetLiabilityFullAmount(
                        paymentId, liabilityId, currencyId,
                        current.getCurrentAmount(), offsetDate, receivableAmounts
                );

                // online_payment_check returns lpf_def_curr in main currency, but current_amount /
                // amount_to_pay are already in collection-channel currency. Offset LPF in channel
                // currency (same as CashTerminal/EasyPay: total - principal), not raw lpf_def_curr.
                BigDecimal lfpInChannelCurrency = EPBPaymentCalculationUtils.calculateLatePaymentFineAmount(
                        current.getTotalAmount(),
                        current.getCurrentAmount()
                );

                if (Boolean.TRUE.equals(principalCovered)
                        && lfpInChannelCurrency != null
                        && lfpInChannelCurrency.compareTo(BigDecimal.ZERO) > 0) {

                    Long lfpLiabilityId = paymentOffsettingTransactionalService.createLatePaymentFineAndLiability(
                            liabilityId, payDate
                    );

                    if (lfpLiabilityId != null && lfpLiabilityId > 0) {
                        paymentOffsettingTransactionalService.offsetLiabilityFullAmount(
                                paymentId, lfpLiabilityId, currencyId,
                                lfpInChannelCurrency, offsetDate, receivableAmounts
                        );
                    } else {
                        log.warn("LPF liability creation returned invalid id for liabilityId {}", liabilityId);
                        receivableAmounts.merge(paymentId, lfpInChannelCurrency, BigDecimal::add);
                    }
                }
            }

            BigDecimal totalReceivable = receivableAmounts.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalReceivable.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Creating receivable for paymentId {} with amount {}", paymentId, totalReceivable);
                paymentOffsettingTransactionalService.createAndOffsetReceivable(
                        customerId, currencyId, paymentId, totalReceivable,
                        contractBillingGroupId, payDate
                );
            }

            log.info("Offsetting complete for paymentId {}", paymentId);

        } catch (Exception e) {
            log.error("Error during Bulgaria Post payment offsetting for paymentId: {}", paymentId, e);
        }
    }

    /**
     * Same document key can refer to more than one liability (e.g. duplicate outgoing doc from external system).
     * For a single-document receipt, the stored principal+interest matches exactly one of them.
     */
    private SystechLiabilitiesProjection resolveDocumentMatchForPay(
            List<SystechLiabilitiesProjection> candidates,
            String doc,
            BigDecimal receiptTotal,
            int requestedDocumentCount,
            String tid,
            Currency channelCurrency,
            Long mainCurrencyId
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (requestedDocumentCount > 1) {
            return candidates.get(0);
        }
        List<SystechLiabilitiesProjection> matchingTotal = new ArrayList<>();
        for (SystechLiabilitiesProjection p : candidates) {
            BigDecimal expected = expectedTotalForSingleLiability(p, channelCurrency, mainCurrencyId);
            if (receiptTotal != null && receiptTotal.compareTo(expected) == 0) {
                matchingTotal.add(p);
            }
        }
        if (matchingTotal.size() == 1) {
            return matchingTotal.get(0);
        }
        log.warn(
                "Ambiguous document match for TID {} doc={}: {} candidates, {} match receipt total {}",
                tid,
                doc,
                candidates.size(),
                matchingTotal.size(),
                receiptTotal
        );
        return null;
    }

    private BigDecimal expectedTotalForSingleLiability(
            SystechLiabilitiesProjection p,
            Currency channelCurrency,
            Long mainCurrencyId
    ) {
        BigDecimal principal = p != null && p.getCurrentAmount() != null
                ? p.getCurrentAmount()
                : BigDecimal.ZERO;
        BigDecimal interest = calculateInterest(List.of(p), channelCurrency, mainCurrencyId);
        return principal.add(interest);
    }

    /**
     * LPF/interest from {@code calculate_lfp_json} is always in the main currency ({@code calculate_lfp}
     * converts every period to it). Its 2-decimal total is converted once from the main currency to the
     * collection-channel currency ({@code convert_to_currency(lpf, main, channel, 2)}), exactly like
     * {@code GET /obligations} and {@code receivable.online_payment_check} (EasyPay), so PaidSum matches
     * Principal + Interest shown by obligations.
     */
    private BigDecimal calculateInterest(
            List<SystechLiabilitiesProjection> matched,
            Currency channelCurrency,
            Long mainCurrencyId
    ) {
        if (matched == null || matched.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Long channelCurrencyId = channelCurrency != null ? channelCurrency.getId() : null;

        BigDecimal sum = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (SystechLiabilitiesProjection p : matched) {
            Long liabilityId = p == null ? null : p.getLiabilityId();
            if (liabilityId == null) {
                continue;
            }

            String json = customerLiabilityRepository.calculateLatePaymentAndInterestRateForOnlinePayment(liabilityId, today);
            if (trimToNull(json) == null) {
                throw new IllegalStateException("Empty interest calculation JSON for liabilityId=" + liabilityId);
            }

            InterestCalculationResponseDTO dto;
            try {
                dto = objectMapper.readValue(json, InterestCalculationResponseDTO.class);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to parse interest calculation JSON for liabilityId=" + liabilityId, e);
            }

            BigDecimal calculatedInterest = PaymentLpfChannelConverter.toChannelCurrency(
                    dto,
                    mainCurrencyId,
                    channelCurrencyId,
                    (amount, fromId, toId) -> liabilitiesRepository.convertToCurrency(amount, fromId, toId, 2)
            );
            if (calculatedInterest != null) {
                sum = sum.add(calculatedInterest);
            }
        }
        return sum;
    }

    private Currency resolveChannelCurrency(CollectionChannel channel) {
        if (channel == null || channel.getCurrencyId() == null) {
            return null;
        }
        List<NomenclatureItemStatus> statuses = List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
        return currencyRepository.findCurrencyByIdAndStatuses(channel.getCurrencyId(), statuses).orElse(null);
    }

    /**
     * Current main currency, resolved like the obligations query ({@code main_ccy}, started, ACTIVE).
     */
    private Long resolveMainCurrencyId() {
        return currencyRepository.findMainCurrencyNowAndActive()
                .map(Currency::getId)
                .orElse(null);
    }

    private Customer resolveCustomer(String customerNumber10) {
        if (customerNumber10 == null) {
            return null;
        }
        return customerRepository
                .findByCustomerNumberAndStatus(Long.valueOf(customerNumber10), CustomerStatus.ACTIVE)
                .orElse(null);
    }

    private Long resolveContractBillingGroupId(String billingGroup4, List<SystechLiabilitiesProjection> matched) {
        if (billingGroup4 == null) {
            return null;
        }
        Long liabilityId = matched.stream()
                .map(SystechLiabilitiesProjection::getLiabilityId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (liabilityId == null) {
            return null;
        }
        return customerLiabilityRepository
                .findByIdAndStatus(liabilityId, EntityStatus.ACTIVE)
                .map(CustomerLiability::getContractBillingGroupId)
                .orElse(null);
    }

    private static boolean matchesDoc(SystechLiabilitiesProjection p, String doc) {
        if (p == null) {
            return false;
        }
        String requested = trimToNull(doc);
        if (requested == null) {
            return false;
        }
        return requested.equals(p.getLiabilityNumber())
                || requested.equals(normalizeDocId(p.getInvoiceNumber()))
                || requested.equals(normalizeDocId(p.getOutgoingDocumentNumber()))
                || requested.equals(normalizeDocId(p.getOutgoingDocumentFromExternalSystem()));
    }

    private static String normalizeDocId(String raw) {
        String v = trimToNull(raw);
        if (v == null) {
            return null;
        }
        if (v.contains("-")) {
            String[] parts = v.split("-");
            if (parts.length >= 2) {
                return trimToNull(parts[1]);
            }
        }
        return v;
    }

    private static List<String> splitDocumentNumbers(String documentNumberRaw) {
        if (documentNumberRaw == null) {
            return List.of();
        }
        if (!documentNumberRaw.contains(", ")) {
            return List.of(documentNumberRaw);
        }
        String[] parts = documentNumberRaw.split(", ");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String v = trimToNull(p);
            if (v != null) {
                out.add(v);
            }
        }
        return out;
    }

    private static ParsedCustomerNumber parseCustomerNumber(String customerNumber) {
        if (customerNumber == null) {
            return new ParsedCustomerNumber(null, null);
        }
        if (customerNumber.length() == 10) {
            return new ParsedCustomerNumber(customerNumber, null);
        }
        if (customerNumber.length() == 14) {
            return new ParsedCustomerNumber(customerNumber.substring(0, 10), customerNumber.substring(10));
        }
        return new ParsedCustomerNumber(customerNumber, null);
    }

    private record ParsedCustomerNumber(String customerNumber10, String billingGroup4) {
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
