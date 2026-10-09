package bg.energo.phoenix.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CheckLiabilityDetails;
import bg.energo.phoenix.model.entity.EasyPayPaymentDetailsEntity;
import bg.energo.phoenix.model.entity.EasyPayPaymentEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.enums.receivable.OutgoingDocumentType;
import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.model.process.latePaymentFIne.ResultItemDTO;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.service.receivable.latePaymentFine.LatePaymentFineService;
import bg.energo.phoenix.util.epb.EPBListUtils;
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
import java.time.ZoneId;
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
public class PaymentOffsettingTransactionalService {

    @PersistenceContext
    private EntityManager entityManager;

    private final EasyPayInitPaymentService easyPayInitPaymentService;
    private final BillingGroupHelperService billingGroupHelperService;
    private final LatePaymentFineService latePaymentFineService;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CurrencyRepository currencyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Asynchronously starts the payment offsetting process, calculating and updating the receivable amounts
     * for the liabilities associated with the payment.
     * This method performs the following steps for payment offsetting:
     * Fetches the liabilities associated with the payment using the provided customer and billing group information.
     * Iterates through the payment details and matches them with the corresponding liabilities.
     * Performs liability offsetting by calling the relevant method for each matched liability.
     * Calculates the total receivable amount for the payment.
     * If the total receivable amount is greater than zero, a new receivable record is created.
     *
     * @param easyPayPaymentDetailsEntities The list of payment details entities associated with the payment.
     * @param easyPayPaymentEntity          The payment entity that contains the details of the payment.
     * @param paymentId                     The ID of the payment being processed.
     */
    @Transactional
    public void startPaymentOffsetting(
            List<EasyPayPaymentDetailsEntity> easyPayPaymentDetailsEntities,
            EasyPayPaymentEntity easyPayPaymentEntity,
            Long paymentId
    ) {
        log.info(
                "Starting payment and liabilities offsetting process for paymentId: {}",
                paymentId
        );
        ConcurrentHashMap<Long, BigDecimal> threadReceivableAmount = new ConcurrentHashMap<>();

        log.info(
                "Fetching current liabilities for customerId: {} and billingGroupNumber: {}",
                easyPayPaymentEntity.getCustomerId(),
                easyPayPaymentEntity.getBillingGroupNumber()
        );
        List<CheckLiabilityDetails> currentLiabilities = easyPayInitPaymentService.fetchLiabilities(
                easyPayPaymentEntity.getCollectionChannelId(),
                easyPayPaymentEntity.getCustomerId(),
                easyPayPaymentEntity.getBillingGroupNumber()
        );

        if (CollectionUtils.isNotEmpty(currentLiabilities)) {
            log.info(
                    "Fetched {} liabilities for customerId: {}",
                    currentLiabilities.size(),
                    easyPayPaymentEntity.getCustomerId()
            );
            Map<Long, CheckLiabilityDetails> currentLiabilitiesMap = EPBListUtils.transformToMap(
                    currentLiabilities,
                    CheckLiabilityDetails::getLiabilityId
            );

            for (EasyPayPaymentDetailsEntity easyPayPaymentDetailsEntity : easyPayPaymentDetailsEntities) {
                log.info(
                        "Processing checked liability: {} with total amount: {}",
                        easyPayPaymentDetailsEntity.getLiabilityId(),
                        easyPayPaymentDetailsEntity.getTotalAmount()
                );

                CheckLiabilityDetails checkLiabilityDetail = currentLiabilitiesMap.get(easyPayPaymentDetailsEntity.getLiabilityId());
                if (checkLiabilityDetail != null) {
                    log.info(
                            "Ongoing liability found for liabilityId: {}. Starting liability offsetting.",
                            easyPayPaymentDetailsEntity.getLiabilityId()
                    );
                    startLiabilityOffsetting(
                            paymentId,
                            easyPayPaymentEntity.getCurrencyId(),
                            easyPayPaymentEntity.getInitDate(),
                            checkLiabilityDetail,
                            easyPayPaymentDetailsEntity,
                            threadReceivableAmount
                    );
                } else {
                    log.info(
                            "No ongoing liability found for liabilityId: {}. Adding to receivable amount.",
                            easyPayPaymentDetailsEntity.getLiabilityId()
                    );
                    threadReceivableAmount.merge(
                            paymentId,
                            easyPayPaymentDetailsEntity.getTotalAmount(),
                            BigDecimal::add
                    );
                }
            }

            BigDecimal totalReceivableAmount = threadReceivableAmount
                    .values()
                    .stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info(
                    "Total receivable amount calculated: {}",
                    totalReceivableAmount
            );

            if (totalReceivableAmount.compareTo(BigDecimal.ZERO) > 0) {
                log.info(
                        "Creating and offsetting receivable for paymentId: {} with amount: {}",
                        paymentId,
                        totalReceivableAmount
                );

                createAndOffsetReceivable(
                        easyPayPaymentEntity.getCustomerId(),
                        easyPayPaymentEntity.getCurrencyId(),
                        paymentId,
                        totalReceivableAmount,
                        billingGroupHelperService.fetchContractBillingGroupIdForCustomer(
                                easyPayPaymentEntity.getBillingGroupNumber(),
                                easyPayPaymentDetailsEntities
                        ),
                        easyPayPaymentEntity.getInitDate().toLocalDate()
                );
            }

        } else {
            log.info(
                    "No ongoing liabilities found. Creating and offsetting receivable for paymentId: {} with confirmed amount: {}",
                    paymentId,
                    easyPayPaymentEntity.getConfirmedAmount()
            );

            createAndOffsetReceivable(
                    easyPayPaymentEntity.getCustomerId(),
                    easyPayPaymentEntity.getCurrencyId(),
                    paymentId,
                    easyPayPaymentEntity.getConfirmedAmount(),
                    billingGroupHelperService.fetchContractBillingGroupIdForCustomer(
                            easyPayPaymentEntity.getBillingGroupNumber(),
                            easyPayPaymentDetailsEntities
                    ),
                    easyPayPaymentEntity.getInitDate().toLocalDate()
            );
        }

    }

    /**
     * Initiates the liability offsetting process based on the comparison of the current liability amount
     * and the checked amount from the payment details. Depending on the comparison, it takes different
     * actions, such as offsetting the full liability amount, merging amounts into the concurrent map, or
     * initiating partial liability offsetting. If the liability is fully covered, it proceeds with late
     * payment liability offsetting.
     * This method operates within a new transactional context to ensure isolated liability offsetting
     * and related operations.
     *
     * @param paymentId               The ID of the payment being processed.
     * @param currencyId              The currency ID for the payment.
     * @param paymentOffsetDate       The date when the payment offset takes place.
     * @param liabilityCurrentDetails The current liability details, including the current amount.
     * @param liabilityCheckedDetails The checked liability details, including the amount to be offset.
     * @param concurrentHashMap       A map to track the accumulated offset amounts for each payment ID.
     *                                The method follows these steps based on the comparison between the current liability amount
     *                                and the checked liability amount:
     *                                If the amounts are equal, it attempts to offset the full liability amount. If the liability
     *                                is successfully covered, it triggers late payment liability offsetting.
     *                                If the current liability amount is greater than the checked amount (principal increased
     *                                since init), it applies confirm-time scenarios using received total vs current principal.
     *                                If the checked amount is greater than the current amount, it proceeds with partial liability
     *                                offsetting based on the current liability amount.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void startLiabilityOffsetting(
            Long paymentId,
            Long currencyId,
            LocalDateTime paymentOffsetDate,
            CheckLiabilityDetails liabilityCurrentDetails,
            EasyPayPaymentDetailsEntity liabilityCheckedDetails,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        // Compare the current amount of the liability with the checked amount to determine the next action
        int comparison = liabilityCurrentDetails.getCurrentAmount().compareTo(liabilityCheckedDetails.getAmount());

        if (comparison == 0) {
            log.info(
                    "Amounts are equal. Performing full liability offset for liabilityId: {}",
                    liabilityCheckedDetails.getLiabilityId()
            );
            // Case when amounts are equal
            Boolean liabilityCovered = offsetLiabilityFullAmount(
                    paymentId,
                    liabilityCheckedDetails.getLiabilityId(),
                    currencyId,
                    liabilityCheckedDetails.getAmount(),
                    paymentOffsetDate,
                    concurrentHashMap
            );

            if (liabilityCovered) {
                log.info(
                        "Full liability offset complete: {}",
                        liabilityCheckedDetails.getLiabilityId()
                );
                BigDecimal liveLfp = liabilityCurrentDetails.getLfpAmount();
                BigDecimal initLfp = liabilityCheckedDetails.getLfpAmount();
                boolean hasLfp = (liveLfp != null && liveLfp.compareTo(BigDecimal.ZERO) > 0)
                        || (initLfp != null && initLfp.compareTo(BigDecimal.ZERO) > 0);
                if (hasLfp) {
                    log.info(
                            "Full liability offset complete. LFP present (live={}, init={}), starting late payment create and offset for liabilityId: {}",
                            liveLfp,
                            initLfp,
                            liabilityCheckedDetails.getLiabilityId()
                    );
                    startLatePaymentLiabilityOffsetting(
                            paymentId,
                            currencyId,
                            paymentOffsetDate,
                            liabilityCurrentDetails,
                            liabilityCheckedDetails,
                            concurrentHashMap
                    );
                }
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
            // Principal decreased since init (e.g. another receivable covered part of the liability)
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
     * Remaining principal is covered by this payment (full clear of open balance). Per Solution 1,
     * LPF from the init snapshot must still be generated and applied — otherwise the interest
     * portion of the confirmed amount stays on the payment.
     */
    private void handleLiabilityDecreasedSinceInit(
            Long paymentId,
            Long currencyId,
            LocalDateTime paymentOffsetDate,
            CheckLiabilityDetails liabilityCurrentDetails,
            EasyPayPaymentDetailsEntity liabilityCheckedDetails,
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

        // Create LPF before covering remaining principal so interest calc still sees an open overdue liability
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

        if (!principalCovered) {
            return;
        }

        if (initLfp == null || initLfp.compareTo(BigDecimal.ZERO) <= 0) {
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
            log.warn(
                    "LPF liability was not created for liabilityId: {}. Parking init LFP amount {} on receivable.",
                    liabilityId,
                    initLfp
            );
            concurrentHashMap.merge(paymentId, initLfp, BigDecimal::add);
        }
    }

    private void handleLiabilityIncreasedSinceInit(
            Long paymentId,
            Long currencyId,
            LocalDateTime paymentOffsetDate,
            CheckLiabilityDetails liabilityCurrentDetails,
            EasyPayPaymentDetailsEntity liabilityCheckedDetails,
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
            EasyPayPaymentDetailsEntity liabilityCheckedDetails,
            ConcurrentHashMap<Long, BigDecimal> concurrentHashMap
    ) {
        log.info(
                "Starting late payment liability offsetting process for payment ID: {}",
                paymentId
        );

        // Compare the current amount of the liability with the checked amount to determine the next action
        Long latePaymentLiabilityId = createLatePaymentFineAndLiability(
                liabilityCurrentDetails.getLiabilityId(),
                paymentOffsetDate.toLocalDate(),
                liabilityCheckedDetails.getLfpAmount()
        );

        BigDecimal liveLfp = liabilityCurrentDetails.getLfpAmount();
        BigDecimal initLfp = liabilityCheckedDetails.getLfpAmount();
        if (liveLfp == null) {
            liveLfp = BigDecimal.ZERO;
        }
        if (initLfp == null) {
            initLfp = BigDecimal.ZERO;
        }

        int latePaymentComparison = liveLfp.compareTo(initLfp);
        log.info(
                "Comparing amounts: current LFP amount = {}, checked LFP amount = {}",
                liveLfp,
                initLfp
        );

        if (latePaymentComparison == 0) {
            log.info(
                    "Amounts are equal. Proceeding to offset full liability amount."
            );
            // Case when amounts are equal
            if (latePaymentLiabilityId != null && latePaymentLiabilityId > 0) {
                log.info("Created late payment liability ID: {}", latePaymentLiabilityId);
                offsetLiabilityFullAmount(
                        paymentId,
                        latePaymentLiabilityId,
                        currencyId,
                        initLfp,
                        paymentOffsetDate,
                        concurrentHashMap
                );
            } else {
                log.info(
                        "Late payment liability ID is invalid or null. Adding amount to receivable"
                );
                concurrentHashMap.merge(
                        paymentId,
                        initLfp,
                        BigDecimal::add
                );
            }
        } else if (latePaymentComparison > 0) {
            log.info(
                    "Current liability amount is greater than the checked amount. Merging into receivable amount."
            );
            // Case when the current amount is greater than the checked amount
            concurrentHashMap.merge(
                    paymentId,
                    initLfp,
                    BigDecimal::add
            );
        } else {
            log.info(
                    "Checked liability amount is greater than current amount. Proceeding to offset partial liability amount."
            );
            // Case when the checked amount is greater than the current amount
            if (latePaymentLiabilityId != null && latePaymentLiabilityId > 0) {
                offsetLiabilityPartialAmount(
                        paymentId,
                        latePaymentLiabilityId,
                        currencyId,
                        liveLfp,
                        initLfp,
                        paymentOffsetDate,
                        concurrentHashMap
                );
            } else {
                concurrentHashMap.merge(paymentId, initLfp, BigDecimal::add);
            }
        }
    }

    /**
     * Offsets the full liability amount for a given payment, liability, and currency. This method attempts
     * to create an offsetting statement for the liability and payment using the provided details. If the
     * offsetting is successful (i.e., the offsetting statement is created), it returns `true`. Otherwise, it
     * merges the offset amount into the provided concurrent map for further processing and returns `false`.
     * This method operates within a transactional context (REQUIRED), meaning it participates in an
     * existing transaction if one exists.
     *
     * @param paymentId         The ID of the payment being processed.
     * @param liabilityId       The ID of the liability being offset.
     * @param currencyId        The currency ID for the payment.
     * @param offsetAmount      The amount to be offset.
     * @param paymentOffsetDate The date of the payment offset.
     * @param concurrentHashMap A map to track the accumulated offset amounts for each payment ID if the
     *                          offsetting is unsuccessful.
     * @return `true` if the full liability offset was successful (offsetting statement created),
     * `false` if the offsetting failed and the amount was merged into the concurrent map.
     */
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

    /**
     * Offsets a partial liability amount for a given payment, liability, and currency. This method attempts
     * to create an offsetting statement for the liability using the provided details. If the offsetting is
     * successful (i.e., the offsetting statement is created), it subtracts the offset amount from the checked
     * amount and merges the remaining amount into the provided concurrent map for further processing.
     * If the offsetting fails, the full checked amount is merged into the map.
     * This method operates within a transactional context (REQUIRED), meaning it participates in an
     * existing transaction if one exists.
     *
     * @param paymentId         The ID of the payment being processed.
     * @param liabilityId       The ID of the liability being offset.
     * @param currencyId        The currency ID for the payment.
     * @param offsetAmount      The amount to be offset from the liability.
     * @param checkAmount       The amount to check against for partial offsetting.
     * @param paymentOffsetDate The date of the payment offset.
     * @param concurrentHashMap A map to track the accumulated offset amounts for each payment ID if the
     *                          offsetting is unsuccessful.
     * @return {@code true} if the offsetting statement was created, {@code false} otherwise.
     * If the offsetting is successful:
     * The difference between the checked amount and the offset amount is merged into the concurrent map.
     * If the offsetting fails:
     * The full checked amount is merged into the concurrent map.
     */
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
     * Creates a late payment fine and associated liability for a given customer liability.
     * This method calculates the late payment fine and interest based on the provided liability ID and
     * initial date. If the late payment fine is calculated successfully, it is created along with the
     * liability. If any errors occur during the process, it logs error messages and returns {@code null}.
     * The process includes the following steps:
     * Retrieves the customer liability associated with the given liability ID.
     * Calculates the late payment fine and interest rate for the liability using an online payment calculation service.
     * If the calculated late payment amount is valid (greater than zero), it creates a late payment fine and liability record.
     * If any issues arise during the calculation or creation, the method returns {@code null}.
     *
     * @param liabilityId The ID of the liability for which the late payment fine and liability are to be created.
     * @param initDate    The initial date to use for calculating the late payment fine and interest.
     * @return The ID of the created late payment fine and liability if successful, or {@code null} if there was an issue.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Long createLatePaymentFineAndLiability(
            Long liabilityId,
            LocalDate initDate
    ) {
        return createLatePaymentFineAndLiability(liabilityId, initDate, null);
    }

    /**
     * Creates LPF + liability for online payment. When recalculation returns zero/empty but
     * {@code fallbackAmount} from init is present, uses the init snapshot so interest confirmed
     * by the payer is not left stranded on the payment.
     * <p>Not annotated with {@code @Transactional}: only called via self-invocation from
     * transactional callers (or the 2-arg proxy entry), so a local annotation would be a no-op.
     */
    public Long createLatePaymentFineAndLiability(
            Long liabilityId,
            LocalDate initDate,
            BigDecimal fallbackAmount
    ) {
        List<String> errorMessages = new ArrayList<>();

        CustomerLiability customerLiability = latePaymentFineService.getCustomerLiability(liabilityId);
        if (Objects.isNull(customerLiability)) {
            log.warn("Ongoing customer liability not found for liabilityId: {}", liabilityId);
            return null;
        }

        String json = customerLiabilityRepository.calculateLatePaymentAndInterestRateForOnlinePayment(liabilityId, initDate);
        if (StringUtils.isBlank(json)) {
            log.warn("No JSON response received for late payment calculation for liabilityId: {}", liabilityId);
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
                log.info("Late payment amount calculated: {} for liabilityId: {}", latePaymentAmount, liabilityId);
                return latePaymentFineService.createLatePaymentFineAndLiabilityFromOnlinePayment(
                        customerLiability,
                        latePaymentAmount,
                        initDate,
                        errorMessages,
                        resultRows
                );
            }

        } catch (JsonProcessingException e) {
            log.error("Error parsing late payment data for liabilityId: {}: {}", liabilityId, e.getMessage());
            return null;
        }

        log.error("No late payment fine created for liabilityId: {}", liabilityId);
        return null;
    }

    /**
     * Creates a new receivable for a given customer and associates it with a specified accounting period,
     * currency, and billing group. The method handles the creation of a {@link CustomerReceivable} entity,
     * including setting the necessary fields like the receivable number, due date, and initial amount.
     * It also handles saving the entity and performs an offsetting operation if the receivable is successfully created.
     *
     * <p>This method is annotated with {@link Transactional} to ensure that it runs in a separate transaction
     * and is executed with the {@link Propagation#REQUIRES_NEW} propagation behavior, meaning that it
     * starts a new transaction even if one already exists.</p>
     *
     * @param customerId     The ID of the customer for whom the receivable is being created.
     * @param currencyId     The ID of the currency to be used for the receivable.
     * @param paymentId      The ID of the payment associated with this receivable.
     * @param amount         The amount of money for the receivable.
     * @param billingGroupId The ID of the billing group that the receivable will belong to.
     * @param occurrenceDate The date when the receivable occurs (e.g., due date).
     * @see CustomerReceivable
     * @see OutgoingDocumentType
     * @see DefaultAssignmentType
     * @see Pair
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createAndOffsetReceivable(
            Long customerId,
            Long currencyId,
            Long paymentId,
            BigDecimal amount,
            Long billingGroupId,
            LocalDate occurrenceDate
    ) {
        LocalDateTime date = LocalDateTime.now(ZoneId.of("Europe/Sofia"));
        Long accountingPeriodId = billingGroupHelperService.fetchAccountPeriodId(date);

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

    /**
     * Performs the direct liability offsetting operation by calling a stored procedure in the database.
     * This method uses the stored procedure {@code receivable.direct_liability_offsetting} to perform the
     * liability offsetting operation. The operation involves offsetting an amount against a liability and
     * returns a transaction ID and a message indicating success or failure.
     * Steps performed by this method:
     * Registers input and output parameters for the stored procedure.
     * Executes the stored procedure to perform the liability offsetting.
     * Logs an error if the operation fails or if the result message indicates an error.
     * Returns a {@link Pair} containing the transaction ID and the result message.
     *
     * @param paymentId         The ID of the payment for which the liability offsetting is being performed.
     * @param liabilityId       The ID of the liability to be offset.
     * @param offsettingAmount  The amount to be offset against the liability.
     * @param currencyId        The currency ID associated with the payment.
     * @param paymentOffsetDate The date of the payment offset.
     * @return A {@link Pair} containing the transaction ID and the message indicating the result of the operation.
     * The transaction ID will be {@code -1} if an error occurred, and the message will indicate the error if any.
     */
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
            query.setParameter(10, null); //pass null to let DB procedure handle date setting
            query.setParameter(11, OperationContext.OPO.name());

            log.info(
                    "Setting query parameters: [" +
                            "PAYMENT Type: {}, " +
                            "Payment ID: {}, " +
                            "Liability ID: {}, " +
                            "System User: {}, " +
                            "System User: {}, " +
                            "Offsetting Amount: {}, " +
                            "Currency ID: {}, " +
                            "Payment Offset Date: {}, " +
                            "Context: {}]",
                    DirectOffsettingSourceType.PAYMENT.name(),
                    paymentId,
                    liabilityId,
                    "system",
                    "system",
                    offsettingAmount,
                    currencyId,
                    paymentOffsetDate,
                    OperationContext.OPO.name()
            );

            query.execute();

            String outputMessage = (String) query.getOutputParameterValue(7);
            if (StringUtils.isNotBlank(outputMessage)) {
                message.set(outputMessage);
            }

            if ("OK".equals(message.get())) {
                transactionId.set((Long) query.getOutputParameterValue(6));
            } else {
                log.error("Error happened in payment offsetting message is [%s]".formatted(message));
            }

        } catch (Exception e) {
            log.error("An error occurred during liability offsetting: ", e);
        }

        return Pair.of(transactionId.get(), message.get());
    }

    /**
     * Performs a direct offsetting operation for a receivable using a stored procedure. This method calls the
     * stored procedure "receivable.direct_receivable_offsetting" in the database, passing relevant parameters
     * such as payment ID, receivable ID, currency ID, and offsetting amount. The result includes a transaction ID
     * and a message indicating success or failure.
     *
     * <p>The method is wrapped in a transaction with {@link Transactional} annotation, which uses the default
     * {@link Propagation#REQUIRED} propagation, meaning it will participate in an existing transaction or create one
     * if none exists.</p>
     *
     * @param paymentId        The ID of the payment associated with the receivable being offset.
     * @param receivableId     The ID of the receivable being offset.
     * @param currencyId       The ID of the currency used for the offsetting.
     * @param offsettingAmount The amount of money used for offsetting the receivable.
     * @return A {@link Pair} containing the transaction ID and a message:
     * - The transaction ID (long) is generated by the stored procedure and represents the offsetting transaction.
     * - The message (string) contains a result message such as "OK" if the offsetting was successful, or an error message.
     * @throws Exception If there is an error while executing the stored procedure or handling the transaction.
     * @see StoredProcedureQuery
     * @see Pair
     */
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

            log.info(
                    "Setting query parameters: [" +
                            "PAYMENT Type: {}, " +
                            "Payment ID: {}, " +
                            "Receivable ID: {}, " +
                            "System User: {}, " +
                            "System User: {}, " +
                            "Offsetting Amount: {}, " +
                            "Currency ID: {}, " +
                            "Receivable Offset Date: {}, " +
                            "Context: {}]",
                    DirectOffsettingSourceType.PAYMENT.name(),
                    paymentId,
                    receivableId,
                    "system",
                    "system",
                    offsettingAmount,
                    currencyId,
                    now,
                    OperationContext.OPO.name()
            );

            query.execute();

            String outputMessage = (String) query.getOutputParameterValue(7);
            if (StringUtils.isNotBlank(outputMessage)) {
                message.set(outputMessage);
            }

            if ("OK".equals(message.get())) {
                transactionId.set((Long) query.getOutputParameterValue(6));
            } else {
                log.error("Error happened in receivable offsetting message is [%s]".formatted(message));
            }

        } catch (Exception e) {
            log.error("An error occurred during receivable offsetting: ", e);
        }

        return Pair.of(transactionId.get(), message.get());
    }

}
