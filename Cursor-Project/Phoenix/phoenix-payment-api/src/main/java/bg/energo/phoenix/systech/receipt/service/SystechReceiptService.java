package bg.energo.phoenix.systech.receipt.service;

import bg.energo.phoenix.payment.PaymentLpfChannelConverter;
import bg.energo.phoenix.payment.receipt.service.PaymentReceiptDocumentService;
import bg.energo.phoenix.systech.liabilities.repository.SystechLiabilitiesProjection;
import bg.energo.phoenix.systech.liabilities.repository.SystechLiabilitiesRepository;
import bg.energo.phoenix.systech.liabilities.repository.SystechObligationsProjection;
import bg.energo.phoenix.systech.receipt.api.SystechReceiptRequest;
import bg.energo.phoenix.systech.receipt.api.SystechReceiptResponse;
import bg.energo.phoenix.systech.receipt.entities.SystechReceiptEntity;
import bg.energo.phoenix.systech.receipt.repository.SystechReceiptRepository;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SystechReceiptService {

    private static final String COLLECTION_CHANNEL_NAME = "Systech";
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");
    private static final Pattern RECEIPT_TID_22_DIGITS = Pattern.compile("^\\d{22}$");

    private static final String MSG_UNAUTHORIZED = "Неоторизиран потребител.";
    private static final String MSG_INVALID_CUSTOMER_NUMBER = "Невалиден клиентски номер.";
    private static final String MSG_INVALID_DOCUMENT_NUMBER = "Невалиден номер на документ.";
    private static final String MSG_INVALID_CURRENCY = "Невалидна валута.";
    private static final String MSG_INVALID_EXCHANGE = "Невалиден обменен курс.";
    private static final String MSG_INVALID_PAID_SUM = "Невалидна сума за плащане.";
    private static final String MSG_INVALID_TID = "Невалиден номер на транзакция.";
    private static final String MSG_INVALID_POST_CODE = "Невалиден код на станцията.";
    private static final String MSG_SYSTEM_ERROR = "Системна грешка";

    private final CollectionChannelRepository collectionChannelRepository;
    private final SystechLiabilitiesRepository liabilitiesRepository;
    private final SystechReceiptRepository receiptRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final PaymentReceiptDocumentService receiptDocumentService;
    private final ObjectMapper objectMapper;

    public SystechReceiptService(
            CollectionChannelRepository collectionChannelRepository,
            SystechLiabilitiesRepository liabilitiesRepository,
            SystechReceiptRepository receiptRepository,
            CurrencyRepository currencyRepository,
            CustomerLiabilityRepository customerLiabilityRepository,
            PaymentReceiptDocumentService receiptDocumentService,
            ObjectMapper objectMapper
    ) {
        this.collectionChannelRepository = Objects.requireNonNull(collectionChannelRepository, "collectionChannelRepository");
        this.liabilitiesRepository = Objects.requireNonNull(liabilitiesRepository, "liabilitiesRepository");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository");
        this.currencyRepository = Objects.requireNonNull(currencyRepository, "currencyRepository");
        this.customerLiabilityRepository = Objects.requireNonNull(customerLiabilityRepository, "customerLiabilityRepository");
        this.receiptDocumentService = Objects.requireNonNull(receiptDocumentService, "receiptDocumentService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public SystechReceiptResponse receipt(SystechReceiptRequest request) {
        try {
            if (request == null) {
                return SystechReceiptResponse.error(9, MSG_SYSTEM_ERROR);
            }

            final String customerNumber = trimToNull(request.getCustomerNumber());
            final String documentNumberRaw = trimToNull(request.getDocumentNumber());
            final String currency = trimToNull(request.getCurrency());
            final BigDecimal exchange = request.getExchange();
            final BigDecimal paidSum = request.getPaidSum();
            final String tid = trimToNull(request.getTID());
            final String postCode = trimToNull(request.getPostCode());

            if (isInvalidCustomerNumber(customerNumber)) {
                return SystechReceiptResponse.error(2, MSG_INVALID_CUSTOMER_NUMBER);
            }
            if (documentNumberRaw == null) {
                log.error("SystechReceiptService.receipt - documentNumberRaw == null");
                return SystechReceiptResponse.error(3, MSG_INVALID_DOCUMENT_NUMBER);
            }
            if (currency == null || currency.length() > 4) {
                return SystechReceiptResponse.error(4, MSG_INVALID_CURRENCY);
            }
            if (exchange != null && exchange.compareTo(BigDecimal.ZERO) <= 0) {
                return SystechReceiptResponse.error(5, MSG_INVALID_EXCHANGE);
            }
            if (exchange != null && (exchange.precision() > 20 || exchange.scale() > 5)) {
                return SystechReceiptResponse.error(5, MSG_INVALID_EXCHANGE);
            }
            if (paidSum == null || paidSum.compareTo(BigDecimal.ZERO) <= 0) {
                return SystechReceiptResponse.error(6, MSG_INVALID_PAID_SUM);
            }
            if (!isValidSystechReceiptTid(tid)) {
                return SystechReceiptResponse.error(7, MSG_INVALID_TID);
            }
            if (postCode == null || postCode.length() > 256) {
                return SystechReceiptResponse.error(8, MSG_INVALID_POST_CODE);
            }

            if (receiptRepository.existsByTid(tid)) {
                return SystechReceiptResponse.error(7, MSG_INVALID_TID);
            }

            Optional<CollectionChannel> channelOptional = collectionChannelRepository.findCollectionChanel(COLLECTION_CHANNEL_NAME);
            if (channelOptional.isEmpty()) {
                return SystechReceiptResponse.error(9, MSG_SYSTEM_ERROR);
            }
            CollectionChannel channel = channelOptional.get();

            Currency channelCurrency = resolveChannelCurrency(channel);
            if (channelCurrency == null) {
                return SystechReceiptResponse.error(9, MSG_SYSTEM_ERROR);
            }
            if (!currencyEquals(channelCurrency, currency)) {
                return SystechReceiptResponse.error(4, MSG_INVALID_CURRENCY);
            }
            Long mainCurrencyId = resolveMainCurrencyId();
            if (mainCurrencyId == null) {
                return SystechReceiptResponse.error(9, MSG_SYSTEM_ERROR);
            }

            if (!liabilitiesRepository.existsValidCustomerNumber(customerNumber)) {
                return SystechReceiptResponse.error(2, MSG_INVALID_CUSTOMER_NUMBER);
            }

            List<String> requestedDocuments = splitDocumentNumbers(documentNumberRaw);
            if (requestedDocuments.isEmpty()) {
                log.error("SystechReceiptService.receipt - requestedDocuments.isEmpty()");
                return SystechReceiptResponse.error(3, MSG_INVALID_DOCUMENT_NUMBER);
            }

            // Re-evaluate obligations via the same query the post-office portal calls and require that
            // every requested document corresponds to an obligation row with AllowedForPayment = true.
            // In non-combine mode this enforces priority-prefix ordering (a non-priority liability has
            // AllowedForPayment = false and must not be paid until the priority one is settled).
            List<SystechObligationsProjection> obligations = liabilitiesRepository.findObligations(
                    customerNumber,
                    channel.getId()
            );
            if (!areRequestedDocumentsAllowedForPayment(obligations, requestedDocuments)) {
                log.error(
                        "SystechReceiptService.receipt - liability not allowed for payment, requestedDocuments={}",
                        requestedDocuments
                );
                return SystechReceiptResponse.error(3, MSG_INVALID_DOCUMENT_NUMBER);
            }

            List<SystechLiabilitiesProjection> eligible = liabilitiesRepository.findEligibleLiabilities(channel.getId(), customerNumber);
            log.info("SystechReceiptService.receipt - channel id  = {} customer number = {}", channel.getId(), customerNumber);
            if (eligible == null || eligible.isEmpty()) {
                log.error("SystechReceiptService.receipt - eligible == null || eligible.isEmpty()");
                return SystechReceiptResponse.error(3, MSG_INVALID_DOCUMENT_NUMBER);
            }

            if (Boolean.TRUE.equals(channel.getCombineLiabilities()) && requestedDocuments.size() != eligible.size()) {
                log.error(" eligible.size() = {}, requestedDocuments.size() = {}", eligible.size(), requestedDocuments.size());
                return SystechReceiptResponse.error(3, MSG_INVALID_DOCUMENT_NUMBER);
            }

            if (!Boolean.TRUE.equals(channel.getCombineLiabilities()) && requestedDocuments.size() > 1) {
                log.error("SystechReceiptService.receipt - requestedDocuments.size() > 1");
                return SystechReceiptResponse.error(3, MSG_INVALID_DOCUMENT_NUMBER);
            }

            List<SystechLiabilitiesProjection> matched = new ArrayList<>();
            for (String doc : requestedDocuments) {
                List<SystechLiabilitiesProjection> candidates = eligible.stream()
                        .filter(p -> matchesDoc(p, doc))
                        .toList();
                DocResolution resolution = resolveDocumentMatch(
                        candidates,
                        doc,
                        paidSum,
                        requestedDocuments.size(),
                        channelCurrency,
                        mainCurrencyId
                );
                if (resolution.projection() == null) {
                    if (resolution.invalidPaidSumForDupDoc()) {
                        log.error(
                                "SystechReceiptService.receipt - ambiguous document {}: no unique liability for paidSum {}",
                                doc,
                                paidSum
                        );
                        return SystechReceiptResponse.error(6, MSG_INVALID_PAID_SUM);
                    }
                    log.error("SystechReceiptService.receipt - found == null");
                    return SystechReceiptResponse.error(3, MSG_INVALID_DOCUMENT_NUMBER);
                }
                matched.add(resolution.projection());
                eligible.remove(resolution.projection());
            }
            log.info("SystechReceiptService.receipt - matched.size() = {}, matched = {}", matched.size(),
                    matched.stream()
                            .map(SystechLiabilitiesProjection::getCurrentAmount).toList());

            String resolvedCurrency = trimToNull(matched.get(0).getCurrency());
            // Currency already validated against collection channel currency + nomenclature.

            BigDecimal principal = matched.stream()
                    .map(SystechLiabilitiesProjection::getCurrentAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("principal -  {}", principal);

            Map<Long, BigDecimal> interestByLiabilityId = calculateInterestByLiability(matched, channelCurrency, mainCurrencyId);
            BigDecimal interest = interestByLiabilityId.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal another = BigDecimal.ZERO;

            // PaidSum must equal principal + interest, both in collection-channel currency.
            BigDecimal expectedTotal = principal.add(interest);
            log.info("expectedTotal -  {}", expectedTotal);
            if (paidSum.compareTo(expectedTotal) != 0) {
                log.error("paidSum -  {}, expectedTotal -  {}", paidSum, expectedTotal);
                return SystechReceiptResponse.error(6, MSG_INVALID_PAID_SUM);
            }

            ParsedCustomerNumber parsed = parseCustomerNumber(customerNumber);
            SystechReceiptEntity entity = SystechReceiptEntity.builder()
                    .customerNumber(customerNumber)
                    .billingGroupNumber(parsed.billingGroup4())
                    .documentNumberRaw(documentNumberRaw)
                    .currency(currency)
                    .exchange(exchange)
                    .paidSum(paidSum)
                    .tid(tid)
                    .postCode(postCode)
                    .principal(principal)
                    .interest(interest)
                    .build();
            receiptRepository.save(entity);

            var receipts = receiptDocumentService.buildReceiptPayload(
                    SystechReceiptLiabilityMapper.toContexts(matched),
                    channelCurrency,
                    interestByLiabilityId,
                    channel.getTemplateId()
            );

            return SystechReceiptResponse.success(
                    resolvedCurrency == null ? channelCurrency.getName() : resolvedCurrency,
                    exchange,
                    principal,
                    interest,
                    another,
                    receipts
            );
        } catch (Exception e) {
            log.error("SystechReceiptService.receipt", e);
            return SystechReceiptResponse.error(9, MSG_SYSTEM_ERROR);
        }
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

    private static boolean currencyEquals(Currency channelCurrency, String requestCurrency) {
        if (channelCurrency == null) {
            return false;
        }
        String req = trimToNull(requestCurrency);
        if (req == null) {
            return false;
        }
        String name = trimToNull(channelCurrency.getName());
        String abbr = trimToNull(channelCurrency.getAbbreviation());
        return req.equalsIgnoreCase(name) || req.equalsIgnoreCase(abbr);
    }

    private record DocResolution(SystechLiabilitiesProjection projection, boolean invalidPaidSumForDupDoc) {
    }

    /**
     * When several active liabilities share the same document key (e.g. same outgoing doc from external system),
     * {@code findFirst} would always pick the same row. For a single-document payment, the expected total
     * (principal + interest) uniquely identifies which liability the customer is paying.
     */
    private DocResolution resolveDocumentMatch(
            List<SystechLiabilitiesProjection> candidates,
            String doc,
            BigDecimal paidSum,
            int requestedDocumentCount,
            Currency channelCurrency,
            Long mainCurrencyId
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return new DocResolution(null, false);
        }
        if (candidates.size() == 1) {
            return new DocResolution(candidates.get(0), false);
        }
        if (requestedDocumentCount > 1) {
            // Combined payment: duplicate doc strings are resolved in order (aligned with eligible ordering).
            return new DocResolution(candidates.get(0), false);
        }
        List<SystechLiabilitiesProjection> matchingPaid = new ArrayList<>();
        for (SystechLiabilitiesProjection p : candidates) {
            BigDecimal expected = expectedTotalForSingleLiability(p, channelCurrency, mainCurrencyId);
            if (paidSum != null && paidSum.compareTo(expected) == 0) {
                matchingPaid.add(p);
            }
        }
        if (matchingPaid.size() == 1) {
            return new DocResolution(matchingPaid.get(0), false);
        }
        log.error(
                "Ambiguous document match for doc={}: {} candidates, {} match paidSum {}",
                doc,
                candidates.size(),
                matchingPaid.size(),
                paidSum
        );
        return new DocResolution(null, true);
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
    private Map<Long, BigDecimal> calculateInterestByLiability(
            List<SystechLiabilitiesProjection> matched,
            Currency channelCurrency,
            Long mainCurrencyId
    ) {
        if (matched == null || matched.isEmpty()) {
            return Map.of();
        }

        Long channelCurrencyId = channelCurrency != null ? channelCurrency.getId() : null;

        Map<Long, BigDecimal> interestByLiabilityId = new HashMap<>();
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
                interestByLiabilityId.put(liabilityId, calculatedInterest);
            }
        }
        return interestByLiabilityId;
    }

    private BigDecimal calculateInterest(
            List<SystechLiabilitiesProjection> matched,
            Currency channelCurrency,
            Long mainCurrencyId
    ) {
        return calculateInterestByLiability(matched, channelCurrency, mainCurrencyId).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean areRequestedDocumentsAllowedForPayment(
            List<SystechObligationsProjection> obligations,
            List<String> requestedDocuments
    ) {
        if (obligations == null || obligations.isEmpty()
                || requestedDocuments == null || requestedDocuments.isEmpty()) {
            return false;
        }
        for (String doc : requestedDocuments) {
            boolean allowed = obligations.stream()
                    .filter(o -> obligationDocumentMatches(o, doc))
                    .anyMatch(SystechObligationsProjection::isAllowedForPayment);
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private static boolean obligationDocumentMatches(
            SystechObligationsProjection obligation,
            String requestedDoc
    ) {
        if (obligation == null) {
            return false;
        }
        String docNumber = trimToNull(obligation.getDocumentNumber());
        String req = trimToNull(requestedDoc);
        if (docNumber == null || req == null) {
            return false;
        }
        if (req.equals(docNumber)) {
            return true;
        }
        // Combine-liabilities obligation rows carry a comma-separated DocumentNumber.
        for (String part : docNumber.split(", ")) {
            if (req.equals(trimToNull(part))) {
                return true;
            }
        }
        return false;
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

    /**
     * PHN-2628: TID on {@code GET /receipt} must be exactly 22 digits; the first 16 are {@code yyyyMMddHHmmssff}
     * with calendar-valid day-of-month (incl. leap years) and time fields in range. No “real timestamp” vs
     * request-time check. Remaining 6 digits are not interpreted beyond the overall 22-digit constraint.
     */
    static boolean isValidSystechReceiptTid(String tid) {
        if (tid == null || !RECEIPT_TID_22_DIGITS.matcher(tid).matches()) {
            return false;
        }
        int year;
        int month;
        int day;
        int hour;
        int minute;
        int second;
        int fraction;
        try {
            year = Integer.parseInt(tid.substring(0, 4));
            month = Integer.parseInt(tid.substring(4, 6));
            day = Integer.parseInt(tid.substring(6, 8));
            hour = Integer.parseInt(tid.substring(8, 10));
            minute = Integer.parseInt(tid.substring(10, 12));
            second = Integer.parseInt(tid.substring(12, 14));
            fraction = Integer.parseInt(tid.substring(14, 16));
        } catch (NumberFormatException e) {
            return false;
        }
        if (month < 1 || month > 12) {
            return false;
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
            return false;
        }
        if (fraction < 0 || fraction > 99) {
            return false;
        }
        try {
            return YearMonth.of(year, month).isValidDay(day);
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static boolean isInvalidCustomerNumber(String value) {
        if (value == null) {
            return true;
        }
        if (value.length() > 20) {
            return true;
        }
        if (!DIGITS_ONLY.matcher(value).matches()) {
            return true;
        }
        return value.length() != 10 && value.length() != 14;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

