package bg.energo.phoenix.virtualpos.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.enums.receivable.OutgoingDocumentType;
import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.model.process.latePaymentFIne.ResultItemDTO;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.service.receivable.latePaymentFine.LatePaymentFineService;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentDetailsEntity;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService.RECEIVABLE_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualPosPaymentOffsettingTransactionalService {

    private final VirtualPosInitPaymentService virtualPosInitPaymentService;
    private final VirtualPosBillingGroupHelperService virtualPosBillingGroupHelperService;
    private final LatePaymentFineService latePaymentFineService;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CurrencyRepository currencyRepository;
    private final ObjectMapper objectMapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startPaymentOffsetting(
            List<VirtualPosPaymentDetailsEntity> paymentDetails,
            VirtualPosPaymentEntity paymentEntity,
            Long paymentId
    ) {
        log.info("Starting payment and liabilities offsetting process for paymentId: {}", paymentId);
        ConcurrentHashMap<Long, BigDecimal> threadReceivableAmount = new ConcurrentHashMap<>();

        List<CheckLiabilityDetails> currentLiabilities = virtualPosInitPaymentService.fetchLiabilities(
                paymentEntity.getCollectionChannelId(),
                paymentEntity.getCustomerId(),
                paymentEntity.getBillingGroupNumber()
        );

        if (CollectionUtils.isNotEmpty(currentLiabilities)) {
            Map<Long, CheckLiabilityDetails> currentLiabilitiesMap = EPBListUtils.transformToMap(
                    currentLiabilities,
                    CheckLiabilityDetails::getLiabilityId
            );

            for (VirtualPosPaymentDetailsEntity detail : paymentDetails) {
                CheckLiabilityDetails checkLiabilityDetail = currentLiabilitiesMap.get(detail.getLiabilityId());
                if (checkLiabilityDetail != null) {
                    startLiabilityOffsetting(
                            paymentId,
                            paymentEntity.getCurrencyId(),
                            paymentEntity.getInitDate(),
                            checkLiabilityDetail,
                            detail,
                            threadReceivableAmount
                    );
                } else {
                    threadReceivableAmount.merge(
                            paymentId,
                            detail.getTotalAmount(),
                            BigDecimal::add
                    );
                }
            }

            BigDecimal totalReceivableAmount = threadReceivableAmount
                    .values()
                    .stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalReceivableAmount.compareTo(BigDecimal.ZERO) > 0) {
                createAndOffsetReceivable(
                        paymentEntity.getCustomerId(),
                        paymentEntity.getCurrencyId(),
                        paymentId,
                        totalReceivableAmount,
                        virtualPosBillingGroupHelperService.fetchContractBillingGroupIdForCustomer(
                                paymentEntity.getBillingGroupNumber(),
                                paymentDetails
                        ),
                        paymentEntity.getInitDate().toLocalDate()
                );
            }

        } else {
            createAndOffsetReceivable(
                    paymentEntity.getCustomerId(),
                    paymentEntity.getCurrencyId(),
                    paymentId,
                    paymentEntity.getConfirmedAmount(),
                    virtualPosBillingGroupHelperService.fetchContractBillingGroupIdForCustomer(
                            paymentEntity.getBillingGroupNumber(),
                            paymentDetails
                    ),
                    paymentEntity.getInitDate().toLocalDate()
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void startLiabilityOffsetting(
            Long paymentId,
            Long currencyId,
            LocalDateTime paymentOffsetDate,
            CheckLiabilityDetails liabilityCurrentDetails,
            VirtualPosPaymentDetailsEntity liabilityCheckedDetails,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        int comparison = liabilityCurrentDetails.getCurrentAmount().compareTo(liabilityCheckedDetails.getAmount());

        if (comparison == 0) {
            Boolean liabilityCovered = offsetLiabilityFullAmount(
                    paymentId,
                    liabilityCheckedDetails.getLiabilityId(),
                    currencyId,
                    liabilityCheckedDetails.getAmount(),
                    paymentOffsetDate,
                    concurrentHashMap
            );

            if (liabilityCovered && (
                    (liabilityCurrentDetails.getLfpAmount() != null && liabilityCurrentDetails.getLfpAmount().compareTo(BigDecimal.ZERO) > 0)
                            || (liabilityCheckedDetails.getLfpAmount() != null && liabilityCheckedDetails.getLfpAmount().compareTo(BigDecimal.ZERO) > 0)
            )) {
                startLatePaymentLiabilityOffsetting(
                        paymentId,
                        currencyId,
                        paymentOffsetDate,
                        liabilityCurrentDetails,
                        liabilityCheckedDetails,
                        concurrentHashMap
                );
            }
        } else if (comparison > 0) {
            handleLiabilityIncreasedSinceInit(
                    paymentId,
                    currencyId,
                    paymentOffsetDate,
                    liabilityCurrentDetails,
                    liabilityCheckedDetails,
                    concurrentHashMap
            );
        } else {
            handleLiabilityDecreasedSinceInit(
                    paymentId,
                    currencyId,
                    paymentOffsetDate,
                    liabilityCurrentDetails,
                    liabilityCheckedDetails,
                    concurrentHashMap
            );
        }
    }

    /**
     * Confirm-time handling when principal decreased since init.
     * Remaining principal is covered by this payment. Per Solution 1, LPF from the init snapshot
     * must still be generated and applied — otherwise the interest portion stays on the payment.
     */
    private void handleLiabilityDecreasedSinceInit(
            Long paymentId,
            Long currencyId,
            LocalDateTime paymentOffsetDate,
            CheckLiabilityDetails liabilityCurrentDetails,
            VirtualPosPaymentDetailsEntity liabilityCheckedDetails,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        BigDecimal initLfp = liabilityCheckedDetails.getLfpAmount();
        Long liabilityId = liabilityCheckedDetails.getLiabilityId();

        log.info(
                "Principal decreased since init for liabilityId: {}. current={}, initPrincipal={}, initLfp={}",
                liabilityId,
                liabilityCurrentDetails.getCurrentAmount(),
                liabilityCheckedDetails.getAmount(),
                initLfp
        );

        Long latePaymentLiabilityId = null;
        if (initLfp != null && initLfp.compareTo(BigDecimal.ZERO) > 0) {
            latePaymentLiabilityId = createLatePaymentFineAndLiability(
                    liabilityId,
                    paymentOffsetDate.toLocalDate(),
                    initLfp
            );
        }

        boolean principalCovered = offsetLiabilityPartialAmount(
                paymentId,
                liabilityId,
                currencyId,
                liabilityCurrentDetails.getCurrentAmount(),
                liabilityCheckedDetails.getAmount(),
                paymentOffsetDate,
                concurrentHashMap
        );

        if (!principalCovered || initLfp == null || initLfp.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (latePaymentLiabilityId != null && latePaymentLiabilityId > 0) {
            offsetLiabilityFullAmount(
                    paymentId,
                    latePaymentLiabilityId,
                    currencyId,
                    initLfp,
                    paymentOffsetDate,
                    concurrentHashMap
            );
        } else {
            concurrentHashMap.merge(paymentId, initLfp, BigDecimal::add);
        }
    }

    /**
     * Confirm-time handling when principal increased since init (e.g. prior partial payment was reversed).
     * Uses received total ({@code totalAmount}) vs current principal:
     * <ul>
     *   <li>received &lt; current → partial cover only; do not generate LPF</li>
     *   <li>received ≥ current → fully cover principal; generate LPF; apply remaining to LPF (may stay open)</li>
     * </ul>
     */
    private void handleLiabilityIncreasedSinceInit(
            Long paymentId,
            Long currencyId,
            LocalDateTime paymentOffsetDate,
            CheckLiabilityDetails liabilityCurrentDetails,
            VirtualPosPaymentDetailsEntity liabilityCheckedDetails,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        BigDecimal currentPrincipal = liabilityCurrentDetails.getCurrentAmount();
        BigDecimal received = liabilityCheckedDetails.getTotalAmount();
        Long liabilityId = liabilityCheckedDetails.getLiabilityId();

        log.info("Principal increased since init for liabilityId: {}. current={}, initPrincipal={}, received={}",
                liabilityId, currentPrincipal, liabilityCheckedDetails.getAmount(), received);

        if (received.compareTo(currentPrincipal) < 0) {
            offsetLiabilityPartialAmount(
                    paymentId,
                    liabilityId,
                    currencyId,
                    received,
                    received,
                    paymentOffsetDate,
                    concurrentHashMap
            );
            return;
        }

        if (!Boolean.TRUE.equals(
                offsetLiabilityFullAmount(
                        paymentId,
                        liabilityId,
                        currencyId,
                        currentPrincipal,
                        paymentOffsetDate,
                        concurrentHashMap
                )
        )) {
            return;
        }

        BigDecimal initLfp = liabilityCheckedDetails.getLfpAmount();
        if (initLfp == null || initLfp.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal remainingAfterPrincipal = received.subtract(currentPrincipal);
        Long latePaymentLiabilityId = createLatePaymentFineAndLiability(
                liabilityCurrentDetails.getLiabilityId(),
                paymentOffsetDate.toLocalDate(),
                initLfp
        );
        if (latePaymentLiabilityId == null || latePaymentLiabilityId <= 0) {
            if (remainingAfterPrincipal.compareTo(BigDecimal.ZERO) > 0) {
                concurrentHashMap.merge(paymentId, remainingAfterPrincipal, BigDecimal::add);
            }
            return;
        }

        BigDecimal lfpOffsetAmount = remainingAfterPrincipal.min(initLfp);
        if (lfpOffsetAmount.compareTo(BigDecimal.ZERO) > 0) {
            offsetLiabilityFullAmount(
                    paymentId,
                    latePaymentLiabilityId,
                    currencyId,
                    lfpOffsetAmount,
                    paymentOffsetDate,
                    concurrentHashMap
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void startLatePaymentLiabilityOffsetting(
            Long paymentId,
            Long currencyId,
            LocalDateTime paymentOffsetDate,
            CheckLiabilityDetails liabilityCurrentDetails,
            VirtualPosPaymentDetailsEntity liabilityCheckedDetails,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        BigDecimal liveLfp = liabilityCurrentDetails.getLfpAmount();
        BigDecimal initLfp = liabilityCheckedDetails.getLfpAmount();
        if (liveLfp == null) {
            liveLfp = BigDecimal.ZERO;
        }
        if (initLfp == null) {
            initLfp = BigDecimal.ZERO;
        }

        Long latePaymentLiabilityId = createLatePaymentFineAndLiability(
                liabilityCurrentDetails.getLiabilityId(),
                paymentOffsetDate.toLocalDate(),
                initLfp
        );

        int latePaymentComparison = liveLfp.compareTo(initLfp);

        if (latePaymentComparison == 0) {
            if (latePaymentLiabilityId != null && latePaymentLiabilityId > 0) {
                offsetLiabilityFullAmount(
                        paymentId,
                        latePaymentLiabilityId,
                        currencyId,
                        initLfp,
                        paymentOffsetDate,
                        concurrentHashMap
                );
            } else {
                concurrentHashMap.merge(
                        paymentId,
                        initLfp,
                        BigDecimal::add
                );
            }
        } else if (latePaymentComparison > 0) {
            concurrentHashMap.merge(
                    paymentId,
                    initLfp,
                    BigDecimal::add
            );
        } else {
            offsetLiabilityPartialAmount(
                    paymentId,
                    latePaymentLiabilityId,
                    currencyId,
                    liveLfp,
                    initLfp,
                    paymentOffsetDate,
                    concurrentHashMap
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Boolean offsetLiabilityFullAmount(
            Long paymentId,
            Long liabilityId,
            Long currencyId,
            BigDecimal offsetAmount,
            LocalDateTime paymentOffsetDate,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        Pair<Long, String> offsetingPair = liabilityDirectOffsettingStatementJpa(
                paymentId,
                liabilityId,
                offsetAmount,
                currencyId,
                paymentOffsetDate
        );

        if (offsetingPair.getKey() == null || offsetingPair.getKey() <= 0) {
            concurrentHashMap.merge(
                    paymentId,
                    offsetAmount,
                    BigDecimal::add
            );
            return false;
        } else {
            return true;
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean offsetLiabilityPartialAmount(
            Long paymentId,
            Long liabilityId,
            Long currencyId,
            BigDecimal offsetAmount,
            BigDecimal checkAmount,
            LocalDateTime paymentOffsetDate,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        Pair<Long, String> offsetingPair = liabilityDirectOffsettingStatementJpa(
                paymentId,
                liabilityId,
                offsetAmount,
                currencyId,
                paymentOffsetDate
        );
        if (offsetingPair.getKey() != null && offsetingPair.getKey() > 0) {
            log.info(
                    "Direct offsetting successful for paymentId: {}, liabilityId: {}, offsetAmount: {}. Updating receivable with remaining amount.",
                    paymentId,
                    liabilityId,
                    offsetAmount
            );
            concurrentHashMap.merge(
                    paymentId,
                    checkAmount.subtract(offsetAmount),
                    BigDecimal::add
            );
            return true;
        } else {
            log.warn(
                    "Direct offsetting failed for paymentId: {}, liabilityId: {}, offsetAmount: {}. Adding full checkAmount to receivable.",
                    paymentId,
                    liabilityId,
                    offsetAmount
            );
            concurrentHashMap.merge(
                    paymentId,
                    checkAmount,
                    BigDecimal::add
            );
            return false;
        }
    }

    /**
     * Creates LPF + liability for online payment. When recalculation returns zero/empty but
     * {@code fallbackAmount} from init is present, uses the init snapshot so interest confirmed
     * by the payer is not left stranded on the payment.
     * <p>Not annotated with {@code @Transactional}: only called via self-invocation from
     * transactional callers, so a local annotation would be a no-op.
     */
    public Long createLatePaymentFineAndLiability(
            Long liabilityId,
            LocalDate initDate,
            BigDecimal fallbackAmount
    ) {
        List<String> errorMessages = new ArrayList<>();

        var customerLiability = customerLiabilityRepository.findByIdAndStatus(liabilityId, EntityStatus.ACTIVE).orElse(null);
        if (Objects.isNull(customerLiability)) {
            return null;
        }

        String json = customerLiabilityRepository.calculateLatePaymentAndInterestRateForOnlinePayment(liabilityId, initDate);
        if (StringUtils.isBlank(json)) {
            if (fallbackAmount != null && fallbackAmount.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("Using init LFP fallback amount {} for liabilityId {}", fallbackAmount, liabilityId);
                return latePaymentFineService.createLatePaymentFineAndLiabilityFromOnlinePayment(
                        customerLiability,
                        fallbackAmount,
                        initDate,
                        errorMessages,
                        List.of()
                );
            }
            return null;
        }

        try {
            InterestCalculationResponseDTO interestCalc = objectMapper.readValue(json, InterestCalculationResponseDTO.class);
            BigDecimal latePaymentAmount = interestCalc.getCalculatedInterest();
            List<ResultItemDTO> resultRows = interestCalc.getResults();

            if ((latePaymentAmount == null || latePaymentAmount.compareTo(BigDecimal.ZERO) <= 0)
                    && fallbackAmount != null && fallbackAmount.compareTo(BigDecimal.ZERO) > 0) {
                log.warn(
                        "LPF recalc returned {} for liabilityId {}. Using init fallback amount {}",
                        latePaymentAmount,
                        liabilityId,
                        fallbackAmount
                );
                latePaymentAmount = fallbackAmount;
                if (resultRows == null) {
                    resultRows = List.of();
                }
            }

            if (latePaymentAmount != null && latePaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
                return latePaymentFineService.createLatePaymentFineAndLiabilityFromOnlinePayment(
                        customerLiability,
                        latePaymentAmount,
                        initDate,
                        errorMessages,
                        resultRows
                );
            }

        } catch (JsonProcessingException e) {
            return null;
        }

        return null;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void createAndOffsetReceivable(
            Long customerId,
            Long currencyId,
            Long paymentId,
            BigDecimal amount,
            Long billingGroupId,
            LocalDate occurrenceDate
    ) {
        Long accountingPeriodId = virtualPosBillingGroupHelperService.fetchAccountPeriodId();

        Currency currency = currencyRepository
                .findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("currencyId-[%d] currency with such ID not found!".formatted(currencyId))
                );

        CustomerReceivable customerReceivable = new CustomerReceivable();
        customerReceivable.setReceivableNumber("TEMP");
        customerReceivable.setAccountPeriodId(accountingPeriodId);
        customerReceivable.setInitialAmount(amount);
        customerReceivable.setInitialAmountInOtherCurrency(
                currency.getAltCurrencyExchangeRate() == null ? null : amount.multiply(currency.getAltCurrencyExchangeRate())
        );
        customerReceivable.setCurrentAmount(BigDecimal.ZERO);
        customerReceivable.setCurrentAmountInOtherCurrency(BigDecimal.ZERO);
        customerReceivable.setCurrencyId(currencyId);
        customerReceivable.setCustomerId(customerId);
        customerReceivable.setDueDate(occurrenceDate);
        customerReceivable.setOccurrenceDate(occurrenceDate);
        customerReceivable.setIncomeAccountNumber(
                incomeAccountNameRepository.findNumberByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_RECEIVABLES.name())
        );
        customerReceivable.setBillingGroupId(billingGroupId);
        customerReceivable.setCreationType(CreationType.AUTOMATIC);
        customerReceivable.setStatus(EntityStatus.ACTIVE);
        customerReceivable.setOutgoingDocumentType(OutgoingDocumentType.PAYMENT);
        customerReceivable = customerReceivableRepository.saveAndFlush(customerReceivable);
        customerReceivable.setReceivableNumber(RECEIVABLE_PREFIX + customerReceivable.getId());
        customerReceivableRepository.saveAndFlush(customerReceivable);

        if (customerReceivable.getId() != null) {
            Pair<Long, String> receivableOffsettingKeyPair = receivableDirectOffsettingStatementJpa(
                    paymentId,
                    customerReceivable.getId(),
                    currencyId,
                    amount
            );
            log.info(
                    "Receivable offsetting result key = {}, value = {}",
                    receivableOffsettingKeyPair.getKey(),
                    receivableOffsettingKeyPair.getValue()
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Pair<Long, String> liabilityDirectOffsettingStatementJpa(
            Long paymentId,
            Long liabilityId,
            BigDecimal offsettingAmount,
            Long currencyId,
            LocalDateTime paymentOffsetDate
    ) {
        AtomicReference<Long> transactionId = new AtomicReference<>(-1L);
        AtomicReference<String> message = new AtomicReference<>("Error occurred");

        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("receivable.direct_liability_offsetting");

            query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, Long.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(8, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(9, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(10, LocalDateTime.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(11, String.class, ParameterMode.IN);

            query.setParameter(1, DirectOffsettingSourceType.PAYMENT.name());
            query.setParameter(2, paymentId);
            query.setParameter(3, liabilityId);
            query.setParameter(4, "system");
            query.setParameter(5, "system");
            query.setParameter(8, offsettingAmount);
            query.setParameter(9, currencyId);
            query.setParameter(10, paymentOffsetDate);
            query.setParameter(11, OperationContext.OPO.name());

            query.execute();

            String outputMessage = (String) query.getOutputParameterValue(7);
            if (StringUtils.isNotBlank(outputMessage)) {
                message.set(outputMessage);
            }

            if ("OK".equals(message.get())) {
                transactionId.set((Long) query.getOutputParameterValue(6));
            }

        } catch (Exception e) {
            log.error("An error occurred during liability offsetting: ", e);
        }

        return Pair.of(transactionId.get(), message.get());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Pair<Long, String> receivableDirectOffsettingStatementJpa(
            Long paymentId,
            Long receivableId,
            Long currencyId,
            BigDecimal offsettingAmount
    ) {
        AtomicReference<Long> transactionId = new AtomicReference<>(-1L);
        AtomicReference<String> message = new AtomicReference<>("Error occurred");

        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("receivable.direct_receivable_offsetting");

            query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(6, Long.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(7, String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter(8, BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(9, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(10, LocalDateTime.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(11, String.class, ParameterMode.IN);

            LocalDateTime now = LocalDateTime.now();

            query.setParameter(1, DirectOffsettingSourceType.PAYMENT.name());
            query.setParameter(2, paymentId);
            query.setParameter(3, receivableId);
            query.setParameter(4, "system");
            query.setParameter(5, "system");
            query.setParameter(8, offsettingAmount);
            query.setParameter(9, currencyId);
            query.setParameter(10, now);
            query.setParameter(11, OperationContext.OPO.name());

            query.execute();

            String outputMessage = (String) query.getOutputParameterValue(7);
            if (StringUtils.isNotBlank(outputMessage)) {
                message.set(outputMessage);
            }

            if ("OK".equals(message.get())) {
                transactionId.set((Long) query.getOutputParameterValue(6));
            }

        } catch (Exception e) {
            log.error("An error occurred during receivable offsetting: ", e);
        }

        return Pair.of(transactionId.get(), message.get());
    }
}


