package bg.energo.phoenix.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EasyPayPaymentEntity;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.enums.EPayStatusCode;
import bg.energo.phoenix.model.enums.EasyPayPaymentStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageType;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.repository.EasyPayPaymentRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.service.receivable.payment.PaymentService;
import bg.energo.phoenix.service.receivable.paymentPackage.PaymentPackageService;
import bg.energo.phoenix.utils.EPBFunctionUtils;
import bg.energo.phoenix.utils.EPBPaymentCalculationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class PaymentCreationTransactionalService {

    private final EasyPayMapperService easyPayMapperService;
    private final PaymentService paymentService;
    private final PaymentPackageRepository paymentPackageRepository;
    private final PaymentPackageService paymentPackageService;
    private final EasyPayPaymentRepository easyPayPaymentRepository;
    @PersistenceContext
    protected EntityManager entityManager;

    public PaymentCreationTransactionalService(
            PaymentPackageRepository paymentPackageRepository,
            PaymentPackageService paymentPackageService,
            EasyPayMapperService easyPayMapperService,
            PaymentService paymentService,
            EasyPayPaymentRepository easyPayPaymentRepository
    ) {
        this.paymentPackageRepository = paymentPackageRepository;
        this.paymentPackageService = paymentPackageService;
        this.easyPayMapperService = easyPayMapperService;
        this.paymentService = paymentService;
        this.easyPayPaymentRepository = easyPayPaymentRepository;
    }

    /**
     * Creates an online payment for a customer.
     * This method performs the creation of an online payment by first determining whether
     * a valid payment package exists for the provided collection channel and accounting period.
     * It then converts the provided amount into a unit amount and prepares a request to create
     * a payment via the {@link PaymentService}. If the payment package is not found, the method
     * returns null.
     *
     * @param amount                 The amount to be paid in the smallest unit of currency (e.g., cents or pence).
     *                               This is used to calculate the payment's final amount.
     * @param customer               The {@link Customer} object representing the customer making the payment.
     * @param paymentDate            The date on which the payment is made. This date is used to record
     *                               the payment and associate it with the correct accounting period.
     * @param accountingPeriodId     The ID of the accounting period during which the payment is made.
     *                               This is used to fetch the relevant {@link PaymentPackage}.
     * @param collectionChannel      The {@link CollectionChannel} used for the payment, representing
     *                               the channel through which the payment will be collected (e.g., online, in-store).
     * @param contractBillingGroupId The ID of the billing group associated with the customer's contract.
     *                               This may affect payment terms and conditions.
     * @return The ID of the newly created payment if successful, or {@code null} if no valid
     * payment package is found or if there are issues during the payment creation process.
     * @throws IllegalArgumentException If any of the arguments are invalid or null.
     */
    @Transactional
    public Long createOnlinePayment(
            Long amount,
            Customer customer,
            LocalDate paymentDate,
            Long accountingPeriodId,
            CollectionChannel collectionChannel,
            Long contractBillingGroupId
    ) {
        PaymentPackage paymentPackage = createOrFetchPaymentPackage(
                collectionChannel.getId(),
                accountingPeriodId,
                paymentDate
        );
        if (Objects.isNull(paymentPackage)) {
            log.error(
                    "Failed to create or fetch payment package for customer: {}. Collection Channel: {}, Accounting Period: {}",
                    customer.getId(),
                    collectionChannel.getId(),
                    accountingPeriodId
            );
            return null;
        }
        log.info(
                "Successfully fetched or created payment package for customer: {} with package ID: {}",
                customer.getId(),
                paymentPackage.getId()
        );

        BigDecimal convertedAmount = EPBPaymentCalculationUtils.convertToUnitAmount(amount);
        CreatePaymentRequest request = easyPayMapperService.buildPaymentObjectCreateRequest(
                customer,
                collectionChannel,
                paymentPackage,
                convertedAmount,
                contractBillingGroupId,
                paymentDate
        );
        log.info(
                "Payment create request built for customer: {} with details: {}",
                customer.getId(),
                request
        );

        Long paymentId = paymentService.createFromOnlinePayment(request, null);
        entityManager.flush();

        if (paymentId != null) {
            log.info(
                    "Created online payment for customer: {} with payment ID: {}",
                    customer.getId(),
                    paymentId
            );
        }
        return paymentId;
    }


    /**
     * Creates or fetches an existing payment package for a given collection channel.
     * This method performs the following steps:
     * Builds a request to create a new payment package based on the provided collection channel ID.
     * Attempts to fetch an existing payment package from the repository using the provided payment date and collection channel ID.
     * If no existing payment package is found, it retrieves the current accounting period ID and creates a new payment package for the online payment.
     * If an existing payment package is found, it returns the found package.
     * The method ensures that a valid payment package is returned, either by fetching an existing one or creating a new one if necessary.
     *
     * @param collectionChannelId The ID of the collection channel for which the payment package is being created or fetched.
     * @return The existing or newly created payment package.
     * @throws DomainEntityNotFoundException If no accounting period is found when creating a new payment package.
     */
    private PaymentPackage createOrFetchPaymentPackage(
            Long collectionChannelId,
            Long accountingPeriodId,
            LocalDate paymentPackageDate
    ) {
        log.info(
                "Attempting to fetch PaymentPackage for collectionChannelId: {}, accountingPeriodId: {}, paymentPackageDate: {}",
                collectionChannelId,
                accountingPeriodId,
                paymentPackageDate
        );

        Optional<PaymentPackage> paymentPackage = paymentPackageRepository.findPaymentPackageByPaymentDateAndLockStatusInAndCollectionChannelId(
                paymentPackageDate,
                collectionChannelId
        );

        if (paymentPackage.isPresent()) {
            log.info(
                    "Found existing PaymentPackage with collectionChannelId: {}, paymentPackageDate: {}",
                    collectionChannelId,
                    paymentPackageDate
            );
            return paymentPackage.get();
        }

        log.info(
                "No existing PaymentPackage found. Creating a new PaymentPackage for collectionChannelId: {}, paymentPackageDate: {}",
                collectionChannelId,
                paymentPackageDate
        );

        try {
            PaymentPackage newPaymentPackage = paymentPackageService.createFromOnlinePayment(
                    PaymentPackageType.ONLINE,
                    PaymentPackageLockStatus.UNLOCKED,
                    collectionChannelId,
                    accountingPeriodId,
                    paymentPackageDate
            );

            log.info("New PaymentPackage created with ID: {}", newPaymentPackage.getId());
            return newPaymentPackage;

        } catch (DataIntegrityViolationException ex) {
            // Another concurrent request inserted the row first
            log.warn(
                    "Race condition detected. PaymentPackage already created for collectionChannelId: {}, paymentPackageDate: {}. Fetching existing one.",
                    collectionChannelId,
                    paymentPackageDate
            );

            return paymentPackageRepository
                    .findPaymentPackageByPaymentDateAndLockStatusInAndCollectionChannelId(
                            paymentPackageDate,
                            collectionChannelId
                    )
                    .orElse(null);
        }
    }


    /**
     * Saves the payment confirmation details into the {@link EasyPayPaymentEntity} and persists it in the database.
     * This method updates various attributes of the provided {@link EasyPayPaymentEntity} instance, including the confirmation date,
     * the confirmed amount, the payment status, and other relevant details, and then persists the entity using the repository.
     *
     * @param easyPayPaymentEntity The {@link EasyPayPaymentEntity} instance to be updated with confirmation details.
     * @param requestDateString    A string representation of the request date, which will be converted to a {@link LocalDateTime}.
     * @param totalAmount          The total amount of the payment, which will be converted to the appropriate unit amount.
     * @param paymentId            The unique ID of the payment.
     * @throws IllegalArgumentException if any required argument is {@code null} or invalid.
     */
    @Transactional
    public void saveEasyPayPaymentConfirmationDetails(
            EasyPayPaymentEntity easyPayPaymentEntity,
            String requestDateString,
            Long totalAmount,
            Long paymentId
    ) {
        easyPayPaymentEntity.setConfirmDate(EPBFunctionUtils.localDateTimeFromDateString(requestDateString));
        easyPayPaymentEntity.setConfirmedAmount(EPBPaymentCalculationUtils.convertToUnitAmount(totalAmount));
        easyPayPaymentEntity.setPaymentStatus(EasyPayPaymentStatus.CONFIRMED);
        easyPayPaymentEntity.setConfirmedTotalCoinAmount(totalAmount);
        easyPayPaymentEntity.setPaymentId(paymentId);
        easyPayPaymentEntity.setStatusCode(EPayStatusCode.OK.getCode());
        easyPayPaymentRepository.saveAndFlush(easyPayPaymentEntity);
    }

}
