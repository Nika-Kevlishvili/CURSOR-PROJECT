package bg.energo.phoenix.service.receivable.payment;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.entity.receivable.AutomaticOffsettingService;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivableTransactions;
import bg.energo.phoenix.model.entity.receivable.OffsettingService;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOCustomerReceivables;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ManualLiabilityOffsetting;
import bg.energo.phoenix.model.entity.receivable.payment.Payment;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingType;
import bg.energo.phoenix.model.enums.receivable.payment.CanDeletePaymentStatus;
import bg.energo.phoenix.model.enums.receivable.payment.OutgoingDocumentType;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunSearchFields;
import bg.energo.phoenix.model.request.receivable.payment.*;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse;
import bg.energo.phoenix.model.response.contract.biling.ContractBillingGroupShortResponse;
import bg.energo.phoenix.model.response.contract.productContract.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelShortResponse;
import bg.energo.phoenix.model.response.receivable.deposit.DepositShortResponse;
import bg.energo.phoenix.model.response.receivable.latePaymentFine.LatePaymentFineShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.BlockingReasonShortResponse;
import bg.energo.phoenix.model.response.receivable.payment.PaymentListResponse;
import bg.energo.phoenix.model.response.receivable.payment.PaymentResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.receivable.CustomerReceivableTransactionsRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityPaidByPaymentRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting.MLOCustomerReceivablesRepository;
import bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingRepository;
import bg.energo.phoenix.repository.receivable.payment.PaymentReceivableOffsettingRepository;
import bg.energo.phoenix.repository.receivable.payment.PaymentRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.service.notifications.service.NotificationModel;
import bg.energo.phoenix.service.notifications.service.NotificationService;
import bg.energo.phoenix.service.receivable.ObjectOffsettingService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.service.receivable.customerReceivables.ReceivableDirectOffsettingService;
import bg.energo.phoenix.service.receivable.latePaymentFine.LatePaymentFineService;
import bg.energo.phoenix.service.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionEnum.RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING;
import static bg.energo.phoenix.util.epb.EPBListUtils.convertEnumListIntoStringListIfNotNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    public static final String RECEIVABLE_PREFIX = "Receivable-";
    private static final String PREFIX = "Payment";
    private final CurrencyRepository currencyRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final LegalFormRepository legalFormRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CollectionChannelRepository collectionChannelRepository;
    private final CustomerLiabilityPaidByPaymentRepository customerLiabilityPaidByPaymentRepository;
    private final PaymentPackageRepository paymentPackageRepository;
    private final BlockingReasonRepository blockingReasonRepository;
    private final DepositRepository depositRepository;
    private final PenaltyRepository penaltyRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final PaymentReceivableOffsettingRepository paymentReceivableOffsettingRepository;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final PermissionService permissionService;
    private final AutomaticOffsettingService automaticOffsettingService;
    private final CustomerReceivableService customerReceivableService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final NotificationService notificationService;
    private final CustomerReceivableTransactionsRepository customerReceivableTransactionsRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final LatePaymentFineService latePaymentFineService;
    private final MLOCustomerReceivablesRepository mloCustomerReceivablesRepository;
    private final ManualLiabilityOffsettingRepository manualLiabilityOffsettingRepository;
    private final ManualLiabilityOffsettingService manualLiabilityOffsettingService;
    private final OffsettingService offsettingService;
    private final ReceivableDirectOffsettingService receivableDirectOffsettingService;
    private final PaymentOffsettingService paymentOffsettingService;
    private final ObjectOffsettingService objectOffsettingService;
    @PersistenceContext
    private EntityManager em;

    private static CreatePaymentRequest buildPaymentRequestForCancellationProcess(
            Payment payment,
            Long customerId,
            BigDecimal amount,
            Boolean blockedForOffsetting,
            LocalDate blockedForOffsettingFromDate,
            LocalDate blockedForOffsettingToDate,
            Long accountPeriodId,
            Long reasonId
    ) {

        boolean isBlockedForOffsetting = blockedForOffsetting != null && blockedForOffsetting;

        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        createPaymentRequest.setInitialAmount(amount);
        createPaymentRequest.setPaymentDate(LocalDate.now());
        createPaymentRequest.setCurrencyId(payment.getCurrencyId());
        createPaymentRequest.setCollectionChannelId(payment.getCollectionChannelId());
        createPaymentRequest.setPaymentPackageId(payment.getPaymentPackageId());
        createPaymentRequest.setAccountPeriodId(accountPeriodId);
        createPaymentRequest.setPaymentPurpose(payment.getPaymentPurpose());
        createPaymentRequest.setPaymentInfo(payment.getPaymentInfo());
        createPaymentRequest.setBlockedForOffsetting(isBlockedForOffsetting);
        createPaymentRequest.setCustomerId(customerId);
        createPaymentRequest.setContractBillingGroupId(payment.getContractBillingGroupId());
        createPaymentRequest.setOutgoingDocumentType(payment.getOutgoingDocumentType());
        createPaymentRequest.setInvoiceId(payment.getInvoiceId());
        createPaymentRequest.setLatePaymentFineId(payment.getLatePaymentFineId());
        createPaymentRequest.setCustomerDepositId(payment.getCustomerDepositId());
        createPaymentRequest.setPenaltyId(payment.getPenaltyId());
        createPaymentRequest.setFromPaymentCancel(true);

        if (isBlockedForOffsetting) {
            createPaymentRequest.setBlockedForOffsettingBlockingReasonId(reasonId);
            createPaymentRequest.setBlockedForOffsettingFromDate(blockedForOffsettingFromDate);
            createPaymentRequest.setBlockedForOffsettingToDate(blockedForOffsettingToDate);
        }

        return createPaymentRequest;
    }

    @Transactional
    public Long create(CreatePaymentRequest createPaymentRequest, Set<String> permissions) {
        List<String> exceptionMessages = new ArrayList<>();

        Payment payment = new Payment();
        String sequenceValue = paymentRepository.getNextSequenceValue();
        payment.setId(Long.valueOf(sequenceValue));
        payment.setPaymentNumber(PREFIX.concat(sequenceValue));

        Optional<AccountingPeriods> accountingPeriod = validateAndSetAccountingPeriod(createPaymentRequest, exceptionMessages, payment);
        validateAndSetPaymentDate(createPaymentRequest, accountingPeriod, payment, exceptionMessages);
        validateAndSetBlockedForOffsetting(createPaymentRequest, payment, exceptionMessages, permissions);
        Optional<Customer> customer = validateAndSetCustomer(createPaymentRequest, exceptionMessages, payment);
        Optional<ContractBillingGroup> contractBillingGroup = validateAndSetContractBillingGroup(createPaymentRequest, exceptionMessages, payment);
        validateAndSetOutgoingDocumentType(createPaymentRequest, customer, contractBillingGroup, exceptionMessages, payment);

        payment.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(createPaymentRequest.getInitialAmount()));
        payment.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(createPaymentRequest.getInitialAmount()));

        validateAndSetCurrency(createPaymentRequest, exceptionMessages, payment);
        validateAndSetCollectionChannel(createPaymentRequest, payment, exceptionMessages);
        validateAndSetPaymentPackage(createPaymentRequest, exceptionMessages, payment);

        if (createPaymentRequest.getPaymentPurpose() != null) {
            payment.setPaymentPurpose(createPaymentRequest.getPaymentPurpose().trim());
        }

        if (createPaymentRequest.getPaymentInfo() != null) {
            payment.setPaymentInfo(createPaymentRequest.getPaymentInfo().trim());
        }

        payment.setStatus(EntityStatus.ACTIVE);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        payment = paymentRepository.saveAndFlush(payment);
        Long paymentId = payment.getId();
        BigDecimal paymentCurrentAmount = null;

        if (!Objects.requireNonNullElse(createPaymentRequest.getFromPaymentCancel(), false)) {
            paymentCurrentAmount = executePaymentOffsetting(paymentId);

//            if (Objects.equals(paymentCurrentAmount, BigDecimal.ZERO)) {
//                payment.setFullOffsetDate(LocalDate.now());
//                paymentRepository.save(payment);
//            }
        }

        if (Objects.nonNull(paymentCurrentAmount) && paymentCurrentAmount.compareTo(BigDecimal.ZERO) > 0) {
            Long receivableFromPayment = createReceivableFromPayment(payment, paymentCurrentAmount, true);
            if (receivableFromPayment != null) {
                receivableDirectOffsettingService.directOffsetting(
                        DirectOffsettingSourceType.PAYMENT,
                        paymentId,
                        receivableFromPayment,
                        permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                        permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                        OperationContext.APO.name()
                );

//                payment.setCurrentAmount(BigDecimal.ZERO);
//                payment.setFullOffsetDate(LocalDate.now());
//                paymentRepository.save(payment);
            }

            if (payment.getPaymentPackageId() != null) {
//                notificationEventPublisher.publishNotification(new NotificationEvent(paymentId, NotificationType.COLLECTION_CHANNEL_ERROR, collectionChannelRepository, NotificationState.ERROR));
                List<Long> notificationTargets = collectionChannelRepository.getNotificationTargets(payment.getId(), null);
                Long paymentPackageId = payment.getPaymentPackageId();
                notificationService.sendNotifications(notificationTargets.stream().map(x -> new NotificationModel(x, paymentPackageId, NotificationType.PAYMENT_PACKAGE_ERROR)).toList());
            }
        }
        return paymentId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Long createFromOnlinePayment(CreatePaymentRequest createPaymentRequest, Set<String> permissions) {
        List<String> exceptionMessages = new ArrayList<>();

        Payment payment = new Payment();
        String sequenceValue = paymentRepository.getNextSequenceValue();
        payment.setId(Long.valueOf(sequenceValue));
        payment.setPaymentNumber(PREFIX.concat(sequenceValue));

        Optional<AccountingPeriods> accountingPeriod = validateAndSetAccountingPeriod(createPaymentRequest, exceptionMessages, payment);
        validateAndSetPaymentDateForOnlinePayment(createPaymentRequest, accountingPeriod, payment, exceptionMessages);
        validateAndSetBlockedForOffsetting(createPaymentRequest, payment, exceptionMessages, permissions);
        Optional<Customer> customer = validateAndSetCustomer(createPaymentRequest, exceptionMessages, payment);
        Optional<ContractBillingGroup> contractBillingGroup = validateAndSetContractBillingGroup(createPaymentRequest, exceptionMessages, payment);
        validateAndSetOutgoingDocumentType(createPaymentRequest, customer, contractBillingGroup, exceptionMessages, payment);

        payment.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(createPaymentRequest.getInitialAmount()));
        payment.setCurrentAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(createPaymentRequest.getInitialAmount()));

        validateAndSetCurrency(createPaymentRequest, exceptionMessages, payment);
        validateAndSetCollectionChannel(createPaymentRequest, payment, exceptionMessages);
        validateAndSetPaymentPackageFromOnlinePayment(createPaymentRequest, exceptionMessages, payment);

        if (createPaymentRequest.getPaymentPurpose() != null) {
            payment.setPaymentPurpose(createPaymentRequest.getPaymentPurpose().trim());
        }

        if (createPaymentRequest.getPaymentInfo() != null) {
            payment.setPaymentInfo(createPaymentRequest.getPaymentInfo().trim());
        }

        payment.setStatus(EntityStatus.ACTIVE);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        payment.setSystemUserId("system.admin");
        payment = paymentRepository.saveAndFlush(payment);
        return payment.getId();
    }

    private void validateAndSetPaymentPackage(CreatePaymentRequest createPaymentRequest, List<String> exceptionMessages, Payment payment) {
        Optional<PaymentPackage> paymentPackage = paymentPackageRepository.findPaymentPackageByIdAndLockStatusIn(createPaymentRequest.getPaymentPackageId(), List.of(PaymentPackageLockStatus.UNLOCKED));
        if (paymentPackage.isEmpty()) {
            exceptionMessages.add(String.format("paymentPackageId-Payment package with given id [%s] not found;", createPaymentRequest.getPaymentPackageId()));
            return;
        }

        if (Objects.requireNonNullElse(createPaymentRequest.getFromPaymentCancel(), false)) {
            payment.setPaymentPackageId(createPaymentRequest.getPaymentPackageId());
            return;
        }

        paymentPackage = paymentPackageRepository.findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn(createPaymentRequest.getPaymentPackageId(), createPaymentRequest.getCollectionChannelId(), createPaymentRequest.getPaymentDate(), List.of(PaymentPackageLockStatus.UNLOCKED));
        if (paymentPackage.isEmpty()) {
            exceptionMessages.add(String.format("paymentPackageId-Provided payment package with id [%s] is not applicable for selected payment date and collection channel;", createPaymentRequest.getPaymentPackageId()));
        } else {
            payment.setPaymentPackageId(createPaymentRequest.getPaymentPackageId());
        }
    }

    private void validateAndSetPaymentPackageFromOnlinePayment(
            CreatePaymentRequest createPaymentRequest,
            List<String> exceptionMessages,
            Payment payment
    ) {
        Optional<PaymentPackage> paymentPackage = paymentPackageRepository.findPaymentPackageByIdAndLockStatusIn(
                createPaymentRequest.getPaymentPackageId(),
                List.of(PaymentPackageLockStatus.UNLOCKED)
        );

        if (paymentPackage.isEmpty()) {
            exceptionMessages.add(String.format("paymentPackageId-Payment package with given id [%s] not found;", createPaymentRequest.getPaymentPackageId()));
            return;
        }

        if (Objects.requireNonNullElse(createPaymentRequest.getFromPaymentCancel(), false)) {
            payment.setPaymentPackageId(createPaymentRequest.getPaymentPackageId());
            return;
        }

        paymentPackage = paymentPackageRepository.findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn(
                createPaymentRequest.getPaymentPackageId(),
                createPaymentRequest.getCollectionChannelId(),
                LocalDate.now(),
                List.of(PaymentPackageLockStatus.UNLOCKED)
        );

        if (paymentPackage.isEmpty()) {
            exceptionMessages.add(String.format("paymentPackageId-Provided payment package with id [%s] is not applicable for selected payment date and collection channel;", createPaymentRequest.getPaymentPackageId()));
        } else {
            payment.setPaymentPackageId(createPaymentRequest.getPaymentPackageId());
        }
    }

    public Long update(Long id, UpdatePaymentRequest updatePaymentRequest) {
        Payment payment = paymentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Payment does not exists with given id [%s];".formatted(id)));

        if (payment.getStatus().equals(EntityStatus.REVERSED)) {
            throw new ClientException("It is not possible to edit payment that has been involved in a Reverse process", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        Boolean canUpdateBlockingOnly = checkValidationsForUpdatingObjects(id, payment);

        List<String> exceptionMessages = new ArrayList<>();

        Optional<AccountingPeriods> accountingPeriod = validateAndSetAccountPeriodForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetPaymentDateForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages, accountingPeriod);
        validateAndSetInitialAmountForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetCurrencyForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetCollectionChannelForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetPaymentPackageForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetPaymentPurposeForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetPaymentInfoForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        Optional<Customer> customer = validateAndSetCustomerForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        Optional<ContractBillingGroup> contractBillingGroup = validateAndSetContractBillingGroupForUpdate(updatePaymentRequest, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetOutgoingDocumentTypeForUpdate(updatePaymentRequest, customer, contractBillingGroup, payment, canUpdateBlockingOnly, exceptionMessages);
        validateAndSetBlockedForOffsettingForUpdate(updatePaymentRequest, payment, exceptionMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        paymentRepository.save(payment);

        return payment.getId();
    }

    private void handleBlockedForOffsettingAdditionalInfoForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, List<String> exceptionMessages) {
        if (updatePaymentRequest.getBlockedForOffsettingAdditionalInfo() != null) {
            payment.setBlockedForOffsettingAdditionalInfo(updatePaymentRequest.getBlockedForOffsettingAdditionalInfo());
        }
    }

    private void handleBlockedForOffsettingFromDateForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, List<String> exceptionMessages) {
        if (updatePaymentRequest.getBlockedForOffsettingFromDate() == null) {
            exceptionMessages.add("blockedForOffsettingFromDate-BlockedForOffsettingFromDate should not be null;");
            return;
        }

        if (updatePaymentRequest.getBlockedForOffsettingToDate() != null &&
                updatePaymentRequest.getBlockedForOffsettingFromDate().isAfter(updatePaymentRequest.getBlockedForOffsettingToDate())) {
            exceptionMessages.add("blockedForOffsettingFromDate-BlockedForOffsettingFromDate is not <= BlockedForOffsettingToDate;");
            return;
        }

        payment.setBlockedForOffsettingFromDate(updatePaymentRequest.getBlockedForOffsettingFromDate());
    }

    private void validateAndSetInitialAmountForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (!Objects.equals(payment.getInitialAmount(), updatePaymentRequest.getInitialAmount())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("initialAmount-Can not update initial amount because you can update blocking only;");
            } else {
                payment.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(updatePaymentRequest.getInitialAmount()));
            }
        }
    }

    private void validateAndSetPaymentPackageForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (!Objects.equals(payment.getPaymentPackageId(), updatePaymentRequest.getPaymentPackageId())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("paymentPackageId-Can not update payment package because you can update blocking only;");
            } else {
                Optional<PaymentPackage> paymentPackage = paymentPackageRepository.findPaymentPackageByIdAndLockStatusIn(updatePaymentRequest.getPaymentPackageId(), List.of(PaymentPackageLockStatus.UNLOCKED));
                if (paymentPackage.isEmpty()) {
                    exceptionMessages.add(String.format("paymentPackageId-Payment package with given id [%s] not found;", updatePaymentRequest.getPaymentPackageId()));
                    return;
                }

                paymentPackage = paymentPackageRepository.findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn(updatePaymentRequest.getPaymentPackageId(), updatePaymentRequest.getCollectionChannelId(), updatePaymentRequest.getPaymentDate(), List.of(PaymentPackageLockStatus.UNLOCKED));
                if (paymentPackage.isEmpty()) {
                    exceptionMessages.add(String.format("paymentPackageId-Provided payment package with id [%s] is not applicable for selected payment date and collection channel;", updatePaymentRequest.getPaymentPackageId()));
                } else {
                    payment.setPaymentPackageId(updatePaymentRequest.getPaymentPackageId());
                }
            }
        }
    }

    private void validateAndSetPaymentPurposeForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (!Objects.equals(payment.getPaymentPurpose(), updatePaymentRequest.getPaymentPurpose())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("paymentPurpose-Can not update payment purpose because you can update blocking only;");
            } else {
                payment.setPaymentPurpose(updatePaymentRequest.getPaymentPurpose().trim());
            }
        }
    }

    private void validateAndSetPaymentInfoForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (!Objects.equals(payment.getPaymentInfo(), updatePaymentRequest.getPaymentInfo())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("paymentInfo-Can not update payment info because you can update blocking only;");
            } else {
                payment.setPaymentInfo(updatePaymentRequest.getPaymentInfo().trim());
            }
        }
    }

    private void validateAndSetPaymentDateForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages, Optional<AccountingPeriods> accountingPeriod) {
        if (!Objects.equals(payment.getPaymentDate(), updatePaymentRequest.getPaymentDate())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("paymentDate-Can not update payment date because you can update blocking only;");
            }
            if (accountingPeriod.isPresent() && (!accountingPeriod.get().getStartDate().isAfter(updatePaymentRequest.getPaymentDate().atStartOfDay()) &&
                    !updatePaymentRequest.getPaymentDate().atStartOfDay().isAfter(accountingPeriod.get().getEndDate()))) {
                payment.setPaymentDate(updatePaymentRequest.getPaymentDate());
            } else {
                exceptionMessages.add(String.format("paymentDate-PaymentDate is before accounting period with id [%s];", updatePaymentRequest.getAccountPeriodId()));
            }
        }
    }

    private Optional<AccountingPeriods> validateAndSetAccountPeriodForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (!Objects.equals(payment.getAccountPeriodId(), updatePaymentRequest.getAccountPeriodId())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("accountPeriodId-Can not update account period because you can update blocking only;");
            } else {
                Optional<AccountingPeriods> accountingPeriod = accountingPeriodsRepository.findByIdAndStatus(updatePaymentRequest.getAccountPeriodId(), AccountingPeriodStatus.OPEN);
                if (accountingPeriod.isEmpty()) {
                    exceptionMessages.add(String.format("accountingPeriodId-AccountingPeriod with given id [%s] not found;", updatePaymentRequest.getAccountPeriodId()));
                } else {
                    payment.setAccountPeriodId(updatePaymentRequest.getAccountPeriodId());
                    return accountingPeriod;
                }
            }
        }
        return Optional.empty();
    }

    private void validateAndSetCollectionChannelForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (!Objects.equals(payment.getCollectionChannelId(), updatePaymentRequest.getCollectionChannelId())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("collectionChannelId-Can not update collection channel because you can update blocking only;");
            } else {
                Optional<CollectionChannel> collectionChannelOptional = collectionChannelRepository.findCollectionChannelByIdAndStatusIn(updatePaymentRequest.getCollectionChannelId(), List.of(EntityStatus.ACTIVE, EntityStatus.DELETED));
                if (collectionChannelOptional.isEmpty()) {
                    exceptionMessages.add(String.format("collectionChannelId-Collection channel not found with id [%s];", payment.getCollectionChannelId()));
                    return;
                }
                if (Objects.equals(payment.getCurrencyId(), collectionChannelOptional.get().getCurrencyId())) {
                    payment.setCollectionChannelId(updatePaymentRequest.getCollectionChannelId());
                } else {
                    exceptionMessages.add(String.format("collectionChannelId-Currency mismatch Collection Channel ID [%s] is set to currency [%s], which differs from the provided payment currency [%s];", collectionChannelOptional.get().getId(), collectionChannelOptional.get().getCurrencyId(), updatePaymentRequest.getCurrencyId()));
                }
            }
        } else {
            Optional<CollectionChannel> collectionChannelOptional = collectionChannelRepository.findCollectionChannelByIdAndStatusIn(payment.getCollectionChannelId(), List.of(EntityStatus.ACTIVE, EntityStatus.DELETED));
            if (collectionChannelOptional.isEmpty()) {
                exceptionMessages.add(String.format("collectionChannelId-Collection channel not found with id [%s];", payment.getCollectionChannelId()));
                return;
            }

            if (!Objects.equals(payment.getCurrencyId(), collectionChannelOptional.get().getCurrencyId())) {
                exceptionMessages.add(String.format("collectionChannelId-Currency mismatch Collection Channel ID [%s] is set to currency [%s], which differs from the provided payment currency [%s];", payment.getCollectionChannelId(), collectionChannelOptional.get().getCurrencyId(), payment.getCurrencyId()));
            }
        }
    }

    private void validateAndSetCurrencyForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (!Objects.equals(payment.getCurrencyId(), updatePaymentRequest.getCurrencyId())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("currencyId-Can not update currency because you can update blocking only;");
            } else {
                Optional<Currency> currency = currencyRepository.findByIdAndStatus(updatePaymentRequest.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE));
                if (currency.isEmpty()) {
                    exceptionMessages.add(String.format("currencyId-Currency with given ID [%s] not found;", updatePaymentRequest.getCurrencyId()));
                } else {
                    payment.setCurrencyId(updatePaymentRequest.getCurrencyId());
                }
            }
        }
    }

    private boolean checkValidationsForUpdatingObjects(Long id, Payment payment) {
        boolean canUpdateBlockingOnly = false;
        AccountingPeriods accountingPeriod = accountingPeriodsRepository
                .findById(payment.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period does not exists with given id [%s];".formatted(payment.getAccountPeriodId())));

        if (accountingPeriod.getStatus() == AccountingPeriodStatus.CLOSED) {
            canUpdateBlockingOnly = true;
        }

        PaymentPackage paymentPackage = paymentPackageRepository
                .findById(payment.getPaymentPackageId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Payment package does not exists with given id [%s];".formatted(payment.getPaymentPackageId())));

        if (paymentPackage.getLockStatus() == PaymentPackageLockStatus.LOCKED) {
            canUpdateBlockingOnly = true;
        }

        boolean liabilityExists = customerLiabilityPaidByPaymentRepository.existsByCustomerPaymentId(id);
        if (liabilityExists) {
            canUpdateBlockingOnly = true;
        }

        if (paymentRepository.isOnlinePayment(payment.getId())) {
            canUpdateBlockingOnly = true;
        }

        return canUpdateBlockingOnly;
    }

    private Optional<ContractBillingGroup> validateAndSetContractBillingGroupForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        Optional<ContractBillingGroup> contractBillingGroup = Optional.empty();
        if (!Objects.equals(payment.getContractBillingGroupId(), updatePaymentRequest.getContractBillingGroupId())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("contractBillingGroupId-Can't update contract billing group because you can update blocking only;");
                return contractBillingGroup;
            }

            if (updatePaymentRequest.getContractBillingGroupId() != null) {
                contractBillingGroup = contractBillingGroupRepository.findByIdAndStatusIn(updatePaymentRequest.getContractBillingGroupId(), List.of(EntityStatus.ACTIVE));
                if (contractBillingGroup.isEmpty()) {
                    exceptionMessages.add(String.format("contractBillingGroupId-ContractBillingGroup with given id [%s] not found;", updatePaymentRequest.getContractBillingGroupId()));
                } else {
                    payment.setContractBillingGroupId(updatePaymentRequest.getContractBillingGroupId());
                }
            } else {
                payment.setContractBillingGroupId(null);
            }
        }
        return contractBillingGroup;
    }

    private Optional<Customer> validateAndSetCustomerForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        Optional<Customer> customer = customerRepository.findById(payment.getCustomerId());
        if (!Objects.equals(payment.getCustomerId(), updatePaymentRequest.getCustomerId())) {
            if (canUpdateBlockingOnly) {
                exceptionMessages.add("customerId-Can not update customer because you can update blocking only;");
                return customer;
            }
            customer = customerRepository.findByIdAndStatuses(updatePaymentRequest.getCustomerId(), List.of(CustomerStatus.ACTIVE));
            if (customer.isEmpty()) {
                exceptionMessages.add(String.format("customerId-Customer with given id [%s] not found;", updatePaymentRequest.getCustomerId()));
            } else {
                payment.setCustomerId(updatePaymentRequest.getCustomerId());
            }
        }
        return customer;
    }

    public Long delete(Long id, boolean deleteAnyway) {
        Payment payment = paymentRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Payment does not exists with given id [%s];".formatted(id)));

        if (payment.getStatus().equals(EntityStatus.REVERSED)) {
            throw new ClientException("It is not possible to delete payment that has been involved in a Reverse process", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        List<String> exceptionMessages = new ArrayList<>();

        Optional<PaymentPackage> paymentPackage = paymentPackageRepository.findPaymentPackageByIdAndLockStatusIn(payment.getPaymentPackageId(), List.of(PaymentPackageLockStatus.LOCKED, PaymentPackageLockStatus.UNLOCKED));
        if (paymentPackage.isEmpty()) {
            exceptionMessages.add("Payment package does not exists with given id[%s];".formatted(payment.getPaymentPackageId()));
        } else {
            if (paymentPackage.get().getLockStatus().equals(PaymentPackageLockStatus.LOCKED)) {
                exceptionMessages.add("Payment package is locked, can not delete payment !;");
            }
        }

        Optional<AccountingPeriods> accountingPeriod = accountingPeriodsRepository.findByIdAndStatusIn(payment.getAccountPeriodId(), List.of(AccountingPeriodStatus.OPEN, AccountingPeriodStatus.CLOSED));
        if (accountingPeriod.isEmpty()) {
            exceptionMessages.add("Accounting period does not exists with given id[%s];".formatted(payment.getAccountPeriodId()));
        } else {
            if (accountingPeriod.get().getStatus().equals(AccountingPeriodStatus.CLOSED)) {
                exceptionMessages.add("Accounting period is closed, can not delete payment !;");
            }
        }

        String status = paymentRepository.getPaymentDeleteStatus(id);
        CanDeletePaymentStatus canDeletePaymentStatus = Enum.valueOf(CanDeletePaymentStatus.class, status);
        if (!deleteAnyway) {
            switch (canDeletePaymentStatus) {
                case CANNOT_DELETE:
                    exceptionMessages.add("Can not delete payment because it is connected to receivable;");
                case CAN_DELETE_WITH_MESSAGE:
                    exceptionMessages.add("%s-Payment is already used for covering liabilities, are you sure you want to delete?".formatted(EPBFinalFields.INFORMATIONAL_ERROR_MESSAGE_INDICATOR));
            }
        } else {
            if (canDeletePaymentStatus == CanDeletePaymentStatus.CANNOT_DELETE) {
                exceptionMessages.add("Can not delete payment because it is connected to receivable;");
            } else {
                //TODO: Paid amount should be restored in the connected Customer Liability object
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        payment.setStatus(EntityStatus.DELETED);
        paymentRepository.save(payment);

        return payment.getId();
    }

    public PaymentResponse view(Long id) {
        Payment payment = paymentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Payment does not exists with given id [%s];".formatted(id)));

        EntityStatus status = payment.getStatus();
        if (status == EntityStatus.ACTIVE && !hasPermission(PermissionEnum.RECEIVABLE_PAYMENT_VIEW)) {
            throw new DomainEntityNotFoundException("You don't have permission to view active payment;");
        } else if (status == EntityStatus.DELETED && !hasPermission(PermissionEnum.RECEIVABLE_PAYMENT_VIEW_DELETE)) {
            throw new DomainEntityNotFoundException("You don't have permission to view deleted payment;");
        }

        Currency currency = currencyRepository
                .findById(payment.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Currency does not exists with given id [%s];".formatted(payment.getCurrencyId())));

        CollectionChannel collectionChannel = collectionChannelRepository
                .findById(payment.getCollectionChannelId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Collection channel does not exists with given id [%s];".formatted(payment.getCollectionChannelId())));

        AccountingPeriods accountingPeriod = accountingPeriodsRepository
                .findById(payment.getAccountPeriodId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period does not exists with given id [%s];".formatted(payment.getAccountPeriodId())));

        Customer customer = customerRepository
                .findById(payment.getCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer does not exists with given id [%s];".formatted(payment.getCustomerId())));

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(customer.getLastCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details does not exists with given id [%s];".formatted(customer.getLastCustomerDetailId())));

        LegalForm legalForm = customer.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER) ? null : legalFormRepository
                .findById(customerDetails.getLegalFormId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice does not exists with given id [%s];".formatted(customerDetails.getLegalFormId())));

        Invoice invoice = null;
        if (payment.getInvoiceId() != null) {
            invoice = invoiceRepository
                    .findById(payment.getInvoiceId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Invoice does not exists with given id [%s];".formatted(payment.getInvoiceId())));
        }

        Penalty penalty = null;
        if (payment.getPenaltyId() != null) {
            penalty = penaltyRepository
                    .findById(payment.getPenaltyId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Penalty does not exists with given id [%s];".formatted(payment.getPenaltyId())));
        }

        Deposit deposit = null;
        if (payment.getCustomerDepositId() != null) {
            deposit = depositRepository
                    .findById(payment.getCustomerDepositId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Deposit does not exists with given id [%s];".formatted(payment.getCustomerDepositId())));
        }

        LatePaymentFine latePaymentFine = null;
        if (payment.getLatePaymentFineId() != null) {
            latePaymentFine = latePaymentFineRepository
                    .findById(payment.getLatePaymentFineId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Late payment fine does not exists with given id [%s];".formatted(payment.getLatePaymentFineId())));
        }

        ContractBillingGroup contractBillingGroup = null;
        if (payment.getContractBillingGroupId() != null) {
            contractBillingGroup = contractBillingGroupRepository
                    .findById(payment.getContractBillingGroupId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Contract billing group does not exists with given id [%s];".formatted(payment.getContractBillingGroupId())));
        }

        BlockingReason blockingReason = null;
        if (payment.getBlockedForOffsettingBlockingReasonId() != null) {
            blockingReason = blockingReasonRepository
                    .findById(payment.getBlockedForOffsettingBlockingReasonId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Blocking reason does not exists with given id [%s];".formatted(payment.getBlockedForOffsettingBlockingReasonId())));
        }

//        List<CustomerOffsettingResponse> customerOffsettingResponseList = new ArrayList<>();
//        List<PaymentReceivableOffsetting> paymentReceivableOffsettings = paymentRepository.getPaymentReceivableOffsetting(id);
//        List<CustomerLiabilityPaidByPayment> paymentLiabilityOffsettings = paymentRepository.getPaymentLiabilityOffsetting(id);
//        paymentReceivableOffsettings.forEach(it -> {
//            Currency ccy = currencyRepository.findById(it.getCurrencyId()).orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with id : " + it.getCurrencyId()));
//            customerOffsettingResponseList.add(CustomerOffsettingResponse.from(it, ccy, OffsettingObject.RECEIVABLE));
//        });
//        paymentLiabilityOffsettings.forEach(it -> {
//            Currency ccy = currencyRepository.findById(it.getCurrencyId()).orElseThrow(() -> new DomainEntityNotFoundException("Currency not found with id : " + it.getCurrencyId()));
//            customerOffsettingResponseList.add(CustomerOffsettingResponse.from(it, ccy, OffsettingObject.LIABILITY));
//        });

        boolean isOnlinePayment = paymentRepository.isOnlinePayment(payment.getId());

        return PaymentResponse.builder()
                .withId(payment.getId())
                .withPaymentNumber(payment.getPaymentNumber())
                .withPaymentDate(payment.getPaymentDate().atStartOfDay())
                .withFullOffsetDate(payment.getFullOffsetDate() == null ? null : payment.getFullOffsetDate().atStartOfDay())
                .withInitialAmount(payment.getInitialAmount())
                .withCurrentAmount(payment.getCurrentAmount())
                .withCurrencyId(
                        new CurrencyShortResponse(currency)
                )
                .withCollectionChannelId(
                        new CollectionChannelShortResponse(collectionChannel)
                )
                .withPaymentPackageId(payment.getPaymentPackageId())
                .withAccountPeriodId(
                        new AccountingPeriodsResponse(accountingPeriod)
                )
                .withPaymentPurpose(payment.getPaymentPurpose())
                .withPaymentInfo(payment.getPaymentInfo())
                .withBlockedForOffsetting(Objects.requireNonNullElse(payment.getBlockedForOffsetting(), false))
                .withBlockedForOffsettingFromDate(payment.getBlockedForOffsettingFromDate() == null ? null : payment.getBlockedForOffsettingFromDate().atStartOfDay())
                .withBlockedForOffsettingToDate(payment.getBlockedForOffsettingToDate() == null ? null : payment.getBlockedForOffsettingToDate().atStartOfDay())
                .withBlockedForOffsettingBlockingReasonId(
                        blockingReason == null ? null : new BlockingReasonShortResponse(blockingReason.getId(), blockingReason.getName())
                )
                .withBlockedForOffsettingAdditionalInfo(payment.getBlockedForOffsettingAdditionalInfo())
                .withCustomerId(
                        new CustomerShortResponse(
                                customer.getId(),
                                customer.getIdentifier(),
                                customer.getCustomerType(),
                                customerDetails.getBusinessActivity(),
                                Optional.ofNullable(legalForm).map(LegalForm::getName).orElse(null),
                                customerDetails.getName(),
                                customerDetails.getMiddleName(),
                                customerDetails.getLastName()
                        )
                )
                .withContractBillingGroupId(
                        contractBillingGroup == null ? null : new ContractBillingGroupShortResponse(contractBillingGroup)
                )
                .withOutgoingDocumentType(payment.getOutgoingDocumentType())
                .withInvoiceId(
                        invoice == null ? null : new InvoiceShortResponse(invoice.getId(), invoice.getInvoiceNumber())
                )
                .withLatePaymentFineId(
                        latePaymentFine == null ? null : new LatePaymentFineShortResponse(latePaymentFine)
                )
                .withCustomerDepositId(
                        deposit == null ? null : new DepositShortResponse(deposit)
                )
                .withPenaltyId(
                        penalty == null ? null : new PenaltyShortResponse(penalty)
                )
                .withOffsettingResponseList(getPaymentOffsettingResponseList(id))
                .withStatus(payment.getStatus())
                .withIsOnlinePayment(isOnlinePayment)
                .build();
    }

    /**
     * Retrieves a list of CustomerOffsettingResponse objects associated with the specified payment ID.
     *
     * @param paymentId the unique identifier of the payment for which the offsetting responses are to be retrieved
     * @return a list of CustomerOffsettingResponse objects representing the offsettings for the specified payment
     */
    private List<CustomerOffsettingResponse> getPaymentOffsettingResponseList(Long paymentId) {
        return objectOffsettingService
                .fetchObjectOffsettings(ObjectOffsettingType.PAYMENT, paymentId)
                .stream()
                .map(CustomerOffsettingResponse::from)
                .collect(Collectors.toList());
    }

    public Page<PaymentListResponse> list(PaymentListingRequest paymentListingRequest) {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        if (hasPermission(PermissionEnum.RECEIVABLE_PAYMENT_VIEW_DELETE)) {
            entityStatuses.add(EntityStatus.DELETED);
        }

        if (hasPermission(PermissionEnum.RECEIVABLE_PAYMENT_VIEW)) {
            entityStatuses.add(EntityStatus.ACTIVE);
            entityStatuses.add(EntityStatus.REVERSED);
        }

        return paymentRepository
                .filter(
                        getSearchByEnum(paymentListingRequest.getSearchFields()),
                        paymentListingRequest.getPrompt(),
                        convertEnumListIntoStringListIfNotNull(entityStatuses),
                        paymentListingRequest.getInitialAmountFrom(),
                        paymentListingRequest.getInitialAmountTo(),
                        paymentListingRequest.getCurrentAmountFrom(),
                        paymentListingRequest.getCurrentAmountTo(),
                        ListUtils.emptyIfNull(paymentListingRequest.getCollectionChannelIds()),
                        paymentListingRequest.getBlockedForOffsetting(),
                        paymentListingRequest.getPaymentDateFrom(),
                        paymentListingRequest.getPaymentDateTo(),
                        PageRequest.of(
                                paymentListingRequest.getPage(),
                                paymentListingRequest.getSize(),
                                Sort.by(
                                        new Sort.Order(
                                                paymentListingRequest.getDirection(),
                                                getSorByEnum(paymentListingRequest.getColumns())
                                        )
                                )
                        )
                )
                .map(PaymentListResponse::new);
    }

    private String getSearchByEnum(PaymentSearchFields paymentSearchFields) {
        return paymentSearchFields != null ? paymentSearchFields.getValue() : BillingRunSearchFields.ALL.getValue();
    }

    private String getSorByEnum(PaymentListColumns paymentListColumns) {
        return paymentListColumns != null ? paymentListColumns.getValue() : PaymentListColumns.PAYMENT_NUMBER.getValue();
    }

    private void validateAndSetCollectionChannel(CreatePaymentRequest createPaymentRequest, Payment payment, List<String> exceptionMessages) {
        Optional<CollectionChannel> collectionChannel = collectionChannelRepository.findCollectionChannelByIdAndStatusIn(createPaymentRequest.getCollectionChannelId(), List.of(EntityStatus.ACTIVE));
        if (collectionChannel.isEmpty()) {
            exceptionMessages.add(String.format("collectionChannelId-Collection Channel with given id [%s] not found;", createPaymentRequest.getCollectionChannelId()));
        } else {
            if (Objects.equals(payment.getCurrencyId(), collectionChannel.get().getCurrencyId())) {
                payment.setCollectionChannelId(createPaymentRequest.getCollectionChannelId());
            } else {
                exceptionMessages.add(String.format("collectionChannelId-Currency mismatch Collection Channel ID [%s] is set to currency [%s], which differs from the provided payment currency [%s];", createPaymentRequest.getCollectionChannelId(), collectionChannel.get().getCurrencyId(), createPaymentRequest.getCurrencyId()));
            }
        }
    }

    @Transactional
    public Long cancel(PaymentCancelRequest request) {
        Payment payment = paymentRepository
                .findById(request.getPaymentId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Payment with id %s not found;".formatted(request.getPaymentId())));

        if (payment.getStatus() == EntityStatus.REVERSED) {
            throw new ClientException("Payment with id %s is already reversed;".formatted(payment.getId()), ErrorCode.OPERATION_NOT_ALLOWED);
        }

        paymentPackageRepository
                .findPaymentPackageByIdAndLockStatusIn(payment.getPaymentPackageId(), List.of(PaymentPackageLockStatus.UNLOCKED))
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Payment package not found with with id %s and lock status in %s;".formatted(payment.getPaymentPackageId(), PaymentPackageLockStatus.UNLOCKED))
                );

        List<CustomerReceivableTransactions> activePaymentToLiabilityTransactions = customerReceivableTransactionsRepository.findPaymentConnectedToLiabilityViaAutomaticOffsetting(request.getPaymentId());
        activePaymentToLiabilityTransactions.forEach(transaction -> {
                    Optional<CustomerLiability> optionalCustomerLiability = customerLiabilityRepository.findById(transaction.getDestinationObjectId());
                    optionalCustomerLiability.ifPresent(liability -> {
                        if (liability.getChildLatePaymentFineId() != null && latePaymentFineRepository.existsByIdAndReversed(liability.getChildLatePaymentFineId(), false)) {
                            latePaymentFineService.reverse(liability.getChildLatePaymentFineId(), false);
                        }
                    });
                    offsettingService.reverseOffsetting(transaction.getId(), LocalDateTime.now());
                }
        );

        List<CustomerReceivable> customerReceivableCreatedByPayment = customerReceivableRepository.findCustomerReceivableCreatedByPayment(payment.getPaymentNumber());

        customerReceivableCreatedByPayment.forEach(customerReceivable -> {
                    List<MLOCustomerReceivables> mloCustomerReceivables = mloCustomerReceivablesRepository.findAllByCustomerReceivablesId(
                            customerReceivable.getId()
                    );

                    mloCustomerReceivables.forEach(mloCustomerReceivable -> {
                                List<ManualLiabilityOffsetting> mlo = manualLiabilityOffsettingRepository.findManualLiabilityOffsettingsByIdAndReversedIsFalse(
                                        mloCustomerReceivable.getManualLiabilityOffsettingId()
                                );

                                mlo.forEach(mloLiabilityOffsetting -> manualLiabilityOffsettingService.reversal(mloLiabilityOffsetting.getId()));
                            }
                    );

                    List<CustomerReceivableTransactions> nonReversedReceivableTransactions = customerReceivableTransactionsRepository.findNonReversedReceivableTransactions(customerReceivable.getId(), payment.getId());
                    nonReversedReceivableTransactions.forEach(nonReversedReceivableTransaction -> {
                                offsettingService.reverseOffsetting(
                                        nonReversedReceivableTransaction.getId(),
                                        LocalDateTime.now()
                                );
                            }
                    );
                }
        );

        em.refresh(payment);
        Long receivableFromPayment = directReceivableOffsetting(payment, payment.getCurrentAmount(), request);

        CreatePaymentRequest negativePaymentRequest = buildPaymentRequestForCancellationProcess(
                payment,
                payment.getCustomerId(),
                payment.getInitialAmount().negate(),
                false,
                request.getBlockedForOffsettingFromDate(),
                request.getBlockedForOffsettingToDate(),
                payment.getAccountPeriodId(),
                request.getReasonId()
        );
        Long negativePaymentId = create(negativePaymentRequest, null);

        paymentOffsettingService.directNegativePaymentOffsetting(
                DirectOffsettingSourceType.RECEIVABLE,
                receivableFromPayment,
                negativePaymentId,
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                OperationContext.PCN
        );

        if (request.getCustomerId() != null) {
            Payment negativePayment = paymentRepository
                    .findById(negativePaymentId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Payment with id %s not found;".formatted(negativePaymentId)));

            CacheObject cacheObject = accountingPeriodsRepository
                    .findAccountingPeriodsByDate(LocalDateTime.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period for current month not found!;"));

            CreatePaymentRequest paymentRequest = buildPaymentRequestForCancellationProcess(
                    negativePayment,
                    request.getCustomerId(),
                    payment.getInitialAmount(),
                    request.getBlockedForOffsetting(),
                    request.getBlockedForOffsettingFromDate(),
                    request.getBlockedForOffsettingToDate(),
                    cacheObject.getId(),
                    request.getReasonId()
            );
            Long newPaymentId = create(paymentRequest, null);

            Payment newPayment = paymentRepository
                    .findById(newPaymentId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Payment with id %s not found;".formatted(newPaymentId)));

            em.refresh(payment);
            em.refresh(negativePayment);
            payment.setStatus(EntityStatus.REVERSED);
            negativePayment.setStatus(EntityStatus.REVERSED);

            if (request.getBlockedForOffsetting() != null && request.getBlockedForOffsetting()) {
                createReceivableFromPayment(newPayment, newPayment.getCurrentAmount(), false);
                directReceivableOffsetting(newPayment, newPayment.getCurrentAmount(), request);
            } else {
                BigDecimal amount = executePaymentOffsetting(newPaymentId);

                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    directReceivableOffsetting(newPayment, amount, request);
                }
            }

            em.refresh(newPayment);

            return newPaymentId;
        }

        return null;
    }

    private void validateAndSetOutgoingDocumentType(
            CreatePaymentRequest createPaymentRequest,
            Optional<Customer> customer,
            Optional<ContractBillingGroup> contractBillingGroup,
            List<String> exceptionMessages,
            Payment payment
    ) {
        if (customer.isPresent() && createPaymentRequest.getOutgoingDocumentType() != null && !Boolean.TRUE.equals(createPaymentRequest.getFromPaymentCancel())) {
            switch (createPaymentRequest.getOutgoingDocumentType()) {
                case INVOICE -> {
                    Optional<Invoice> invoice = Optional.empty();
                    List<CustomerDetailsShortResponse> customerDetails = customerDetailsRepository.findCustomerDetailsForCustomer(customer.get().getId());
                    if (createPaymentRequest.getContractBillingGroupId() != null && contractBillingGroup.isPresent()) {
                        for (CustomerDetailsShortResponse it : customerDetails) {
                            Optional<Invoice> invoiceOptional = invoiceRepository.findByIdAndCustomerDetailIdAndContractBillingGroupIdAndInvoiceStatusIn(
                                    createPaymentRequest.getInvoiceId(),
                                    it.getDetailId(),
                                    contractBillingGroup.get().getId(),
                                    List.of(InvoiceStatus.REAL)
                            );
                            if (invoiceOptional.isPresent()) {
                                invoice = invoiceOptional;
                            }
                        }
                    } else {
                        if (!customer.get().getIdentifier().equals("000000000")) {
                            for (CustomerDetailsShortResponse it : customerDetails) {
                                Optional<Invoice> invoiceOptional = invoiceRepository.findByIdAndCustomerDetailIdAndInvoiceStatusIn(
                                        createPaymentRequest.getInvoiceId(),
                                        it.getDetailId(),
                                        List.of(InvoiceStatus.REAL)
                                );
                                if (invoiceOptional.isPresent()) {
                                    invoice = invoiceOptional;
                                }
                            }
                        } else {
                            invoice = invoiceRepository.findByIdAndInvoiceStatusIn(createPaymentRequest.getInvoiceId(), List.of(InvoiceStatus.REAL));
                        }
                    }

                    if (invoice.isEmpty()) {
                        exceptionMessages.add(String.format("invoiceId-Invoice with given id [%s] not found;", createPaymentRequest.getInvoiceId()));
                    } else {
                        payment.setInvoiceId(createPaymentRequest.getInvoiceId());
                        payment.setOutgoingDocumentType(OutgoingDocumentType.INVOICE);
                    }
                }
                case LATE_PAYMENT_FINE -> {
                    Optional<LatePaymentFine> latePaymentFine = latePaymentFineRepository.findByIdAndCustomerId(createPaymentRequest.getLatePaymentFineId(), customer.get().getId());
                    if (latePaymentFine.isEmpty()) {
                        exceptionMessages.add(String.format("latePaymentFineId-Late payment fine with given id [%s] not found;", createPaymentRequest.getLatePaymentFineId()));
                    } else {
                        payment.setLatePaymentFineId(createPaymentRequest.getLatePaymentFineId());
                        payment.setOutgoingDocumentType(OutgoingDocumentType.LATE_PAYMENT_FINE);
                    }
                }
                case DEPOSIT -> {
                    Optional<Deposit> deposit = depositRepository.findByIdAndStatusIn(createPaymentRequest.getCustomerDepositId(), List.of(EntityStatus.ACTIVE));
                    if (deposit.isEmpty()) {
                        exceptionMessages.add(String.format("customerDepositId-Deposit with given id [%s] not found;", createPaymentRequest.getCustomerDepositId()));
                    } else {
                        payment.setCustomerDepositId(createPaymentRequest.getCustomerDepositId());
                        payment.setOutgoingDocumentType(OutgoingDocumentType.DEPOSIT);
                    }
                }
                case PENALTY -> {
                    Optional<Penalty> penalty = penaltyRepository.findByIdAndStatus(createPaymentRequest.getPenaltyId(), EntityStatus.ACTIVE);
                    if (penalty.isEmpty()) {
                        exceptionMessages.add(String.format("penaltyId-Penalty with given id [%s] not found;", createPaymentRequest.getPenaltyId()));
                    } else {
                        payment.setPenaltyId(createPaymentRequest.getPenaltyId());
                        payment.setOutgoingDocumentType(OutgoingDocumentType.PENALTY);
                    }
                }
                default -> exceptionMessages.add("outgoingDocument-OutgoingDocument should not be null;");
            }
        }
    }

    private void validateAndSetOutgoingDocumentTypeForUpdate(UpdatePaymentRequest updatePaymentRequest, Optional<Customer> customer, Optional<ContractBillingGroup> contractBillingGroup, Payment payment, Boolean canUpdateBlockingOnly, List<String> exceptionMessages) {
        if (customer.isPresent()) {
            if (updatePaymentRequest.getOutgoingDocumentType() != null) {
                switch (updatePaymentRequest.getOutgoingDocumentType()) {
                    case INVOICE -> {
                        if (!Objects.equals(payment.getInvoiceId(), updatePaymentRequest.getInvoiceId())) {
                            if (canUpdateBlockingOnly) {
                                exceptionMessages.add("invoiceId-Can not update invoice because you can update blocking only;");
                                break;
                            }

                            Optional<Invoice> invoice = Optional.empty();
                            List<CustomerDetailsShortResponse> customerDetails = customerDetailsRepository.findCustomerDetailsForCustomer(customer.get().getId());
                            if (updatePaymentRequest.getContractBillingGroupId() != null && contractBillingGroup.isPresent()) {
                                for (CustomerDetailsShortResponse it : customerDetails) {
                                    Optional<Invoice> invoiceOptional = invoiceRepository.findByIdAndCustomerDetailIdAndContractBillingGroupIdAndInvoiceStatusIn(updatePaymentRequest.getInvoiceId(), it.getDetailId(), contractBillingGroup.get().getId(), List.of(InvoiceStatus.REAL));
                                    if (invoiceOptional.isPresent()) {
                                        invoice = invoiceOptional;
                                    }
                                }
                            } else {
                                for (CustomerDetailsShortResponse it : customerDetails) {
                                    Optional<Invoice> invoiceOptional = invoiceRepository.findByIdAndCustomerDetailIdAndInvoiceStatusIn(updatePaymentRequest.getInvoiceId(), it.getDetailId(), List.of(InvoiceStatus.REAL));
                                    if (invoiceOptional.isPresent()) {
                                        invoice = invoiceOptional;
                                    }
                                }
                            }

                            if (invoice.isEmpty()) {
                                exceptionMessages.add(String.format("invoiceId-Invoice with given id [%s] not found;", updatePaymentRequest.getInvoiceId()));
                            } else {
                                payment.setOutgoingDocumentType(OutgoingDocumentType.INVOICE);
                                payment.setInvoiceId(updatePaymentRequest.getInvoiceId());
                            }

                            payment.setLatePaymentFineId(null);
                            payment.setCustomerDepositId(null);
                            payment.setPenaltyId(null);
                        }
                    }
                    case LATE_PAYMENT_FINE -> {
                        if (updatePaymentRequest.getLatePaymentFineId() != null) {
                            if (!Objects.equals(payment.getLatePaymentFineId(), updatePaymentRequest.getLatePaymentFineId())) {
                                if (canUpdateBlockingOnly) {
                                    exceptionMessages.add("latePaymentFineId-Can not update late payment fine because you can update blocking only;");
                                    break;
                                }

                                Optional<LatePaymentFine> latePaymentFine = latePaymentFineRepository.findByIdAndCustomerId(updatePaymentRequest.getLatePaymentFineId(), customer.get().getId());
                                if (latePaymentFine.isEmpty()) {
                                    exceptionMessages.add(String.format("latePaymentFineId-Late payment fine with given id [%s] not found;", updatePaymentRequest.getLatePaymentFineId()));
                                } else {
                                    payment.setLatePaymentFineId(updatePaymentRequest.getLatePaymentFineId());
                                    payment.setOutgoingDocumentType(OutgoingDocumentType.LATE_PAYMENT_FINE);
                                    payment.setInvoiceId(null);
                                    payment.setCustomerDepositId(null);
                                    payment.setPenaltyId(null);
                                }
                            }
                        } else {
                            exceptionMessages.add("latePaymentFineId-Late payment fine can not be null;");
                        }
                    }
                    case DEPOSIT -> {
                        if (updatePaymentRequest.getCustomerDepositId() != null) {
                            if (!Objects.equals(payment.getCustomerDepositId(), updatePaymentRequest.getCustomerDepositId())) {
                                if (canUpdateBlockingOnly) {
                                    exceptionMessages.add("customerDepositId-Can not update deposit because you can update blocking only;");
                                    break;
                                }

                                Optional<Deposit> deposit = depositRepository.findByIdAndStatusIn(updatePaymentRequest.getCustomerDepositId(), List.of(EntityStatus.ACTIVE));
                                if (deposit.isEmpty()) {
                                    exceptionMessages.add(String.format("customerDepositId-Deposit with given id [%s] not found;", updatePaymentRequest.getCustomerDepositId()));
                                } else {
                                    payment.setCustomerDepositId(updatePaymentRequest.getCustomerDepositId());
                                    payment.setOutgoingDocumentType(OutgoingDocumentType.DEPOSIT);
                                    payment.setInvoiceId(null);
                                    payment.setLatePaymentFineId(null);
                                    payment.setPenaltyId(null);
                                }
                            }
                        } else {
                            exceptionMessages.add("customerDepositId-Customer deposit can not be null;");
                        }
                    }
                    case PENALTY -> {
                        if (updatePaymentRequest.getPenaltyId() != null) {
                            if (!Objects.equals(payment.getPenaltyId(), updatePaymentRequest.getPenaltyId())) {
                                if (canUpdateBlockingOnly) {
                                    exceptionMessages.add("penaltyId-Can not update penalty because you can update blocking only;");
                                    break;
                                }

                                Optional<Penalty> penalty = penaltyRepository.findByIdAndStatus(updatePaymentRequest.getPenaltyId(), EntityStatus.ACTIVE);
                                if (penalty.isEmpty()) {
                                    exceptionMessages.add(String.format("penaltyId-Penalty with given id [%s] not found;", updatePaymentRequest.getPenaltyId()));
                                } else {
                                    payment.setPenaltyId(updatePaymentRequest.getPenaltyId());
                                    payment.setOutgoingDocumentType(OutgoingDocumentType.PENALTY);
                                    payment.setInvoiceId(null);
                                    payment.setLatePaymentFineId(null);
                                    payment.setCustomerDepositId(null);
                                }
                            }
                        } else {
                            exceptionMessages.add("penaltyId-Penalty can not be null;");
                        }
                    }
                    default -> exceptionMessages.add("outgoingDocument-OutgoingDocument should not be null;");
                }
            } else {
                if (payment.getOutgoingDocumentType() != null) {
                    if (canUpdateBlockingOnly) {
                        exceptionMessages.add("outgoingDocumentType-Can not update outgoing document type because you can update blocking only;");
                        return;
                    }

                    payment.setOutgoingDocumentType(null);
                    payment.setInvoiceId(null);
                    payment.setLatePaymentFineId(null);
                    payment.setCustomerDepositId(null);
                    payment.setCustomerDepositId(null);
                }
            }
        }
    }

    private Optional<ContractBillingGroup> validateAndSetContractBillingGroup(CreatePaymentRequest createPaymentRequest, List<String> exceptionMessages, Payment payment) {
        Optional<ContractBillingGroup> contractBillingGroup = Optional.empty();
        if (createPaymentRequest.getContractBillingGroupId() != null) {
            contractBillingGroup = contractBillingGroupRepository.findByIdAndStatusIn(createPaymentRequest.getContractBillingGroupId(), List.of(EntityStatus.ACTIVE));
            if (contractBillingGroup.isEmpty()) {
                exceptionMessages.add(String.format("contractBillingGroupId-ContractBillingGroup with given id [%s] not found;", createPaymentRequest.getContractBillingGroupId()));
            } else {
                payment.setContractBillingGroupId(createPaymentRequest.getContractBillingGroupId());
            }
        }
        return contractBillingGroup;
    }

    private Optional<Customer> validateAndSetCustomer(CreatePaymentRequest createPaymentRequest, List<String> exceptionMessages, Payment payment) {
        Optional<Customer> customer = customerRepository.findByIdAndStatuses(createPaymentRequest.getCustomerId(), List.of(CustomerStatus.ACTIVE));
        if (customer.isEmpty()) {
            exceptionMessages.add(String.format("customerId-Customer with given id [%s] not found;", createPaymentRequest.getCustomerId()));
        } else {
            payment.setCustomerId(createPaymentRequest.getCustomerId());
        }
        return customer;
    }

    private void validateAndSetBlockedForOffsetting(CreatePaymentRequest createPaymentRequest, Payment payment, List<String> exceptionMessages, Set<String> permissions) {
        if (createPaymentRequest.isBlockedForOffsetting()) {
            handleBlockedForOffsettingParameters(createPaymentRequest, payment, exceptionMessages, permissions);
        } else {
            payment.setBlockedForOffsetting(false);
        }
    }

    private void validateAndSetBlockedForOffsettingForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, List<String> exceptionMessages) {
        if (hasBlockedForOffsettingChanged(updatePaymentRequest, payment)) {
            boolean currentBlockedStatus = Boolean.TRUE.equals(payment.getBlockedForOffsetting());

            if (updatePaymentRequest.isBlockedForOffsetting() != currentBlockedStatus) {
                if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
                    if (updatePaymentRequest.isBlockedForOffsetting()) {
                        payment.setBlockedForOffsetting(true);
                        handleBlockedForOffsettingParametersForUpdate(updatePaymentRequest, payment, exceptionMessages);
                    } else {
                        payment.setBlockedForOffsetting(false);
                        payment.setBlockedForOffsettingFromDate(null);
                        payment.setBlockedForOffsettingToDate(null);
                        payment.setBlockedForOffsettingBlockingReasonId(null);
                        payment.setBlockedForOffsettingAdditionalInfo(null);
                    }
                } else {
                    exceptionMessages.add("You don't have permission for block offsetting!;");
                }
            } else if (updatePaymentRequest.isBlockedForOffsetting()) {
                if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
                    handleBlockedForOffsettingParametersForUpdate(updatePaymentRequest, payment, exceptionMessages);
                } else {
                    exceptionMessages.add("You don't have permission for block offsetting!;");
                }
            }
        }
    }

    private void handleBlockedForOffsetting(Payment payment, List<String> exceptionMessages, Set<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
                payment.setBlockedForOffsetting(true);
            } else {
                exceptionMessages.add("blockedForOffsetting-You don't have permission for block offsetting;");
            }
        } else {
            if (!permissions.contains(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.getId())) {
                throw new ClientException("You don't have appropriate permission: %s;".formatted(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.name()), ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

    }

    private void handleBlockedForOffsettingForUpdate(Payment payment, List<String> exceptionMessages) {
        if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
            payment.setBlockedForOffsetting(true);
        } else {
            exceptionMessages.add("blockedForOffsetting-You don't have permission for block offsetting, to update blocked for offsetting;");
        }
    }

    private void handleBlockedForOffsettingBlockingReasonIdForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, List<String> exceptionMessages) {
        if (updatePaymentRequest.getBlockedForOffsettingBlockingReasonId() == null) {
            exceptionMessages.add("blockedForOffsettingBlockingReasonId-BlockedForOffsettingBlockingReasonId should not be null;");
            return;
        }

        boolean reasonExists = blockingReasonRepository.existsByIdAndStatusIn(
                updatePaymentRequest.getBlockedForOffsettingBlockingReasonId(),
                List.of(NomenclatureItemStatus.ACTIVE));

        if (!reasonExists) {
            exceptionMessages.add("blockedForOffsettingBlockingReasonId-Reason id does not exists with given id [%s];".formatted(
                    updatePaymentRequest.getBlockedForOffsettingBlockingReasonId()));
            return;
        }

        payment.setBlockedForOffsettingBlockingReasonId(updatePaymentRequest.getBlockedForOffsettingBlockingReasonId());
    }

    private void handleBlockedForOffsettingToDateForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, List<String> exceptionMessages) {
        if (updatePaymentRequest.getBlockedForOffsettingToDate() == null) {
            return;
        }

        if (updatePaymentRequest.getBlockedForOffsettingFromDate() != null &&
                updatePaymentRequest.getBlockedForOffsettingToDate().isBefore(updatePaymentRequest.getBlockedForOffsettingFromDate())) {
            exceptionMessages.add("blockedForOffsettingToDate-BlockedForOffsettingToDate is before BlockedForOffsettingFromDate;");
            return;
        }

        payment.setBlockedForOffsettingToDate(updatePaymentRequest.getBlockedForOffsettingToDate());
    }

    private void handleBlockedForOffsettingParameters(CreatePaymentRequest createPaymentRequest, Payment payment, List<String> exceptionMessages, Set<String> permissions) {
        handleBlockedForOffsetting(payment, exceptionMessages, permissions);
        handleBlockedForOffsettingFromDate(createPaymentRequest, payment, exceptionMessages, permissions);
        handleBlockedForOffsettingBlockingReasonId(createPaymentRequest, payment, exceptionMessages, permissions);
        handleBlockedForOffsettingAdditionalInfo(createPaymentRequest, payment, exceptionMessages, permissions);
    }

    private void handleBlockedForOffsettingParametersForUpdate(UpdatePaymentRequest updatePaymentRequest, Payment payment, List<String> exceptionMessages) {
        handleBlockedForOffsettingForUpdate(payment, exceptionMessages);
        handleBlockedForOffsettingFromDateForUpdate(updatePaymentRequest, payment, exceptionMessages);
        handleBlockedForOffsettingToDateForUpdate(updatePaymentRequest, payment, exceptionMessages);
        handleBlockedForOffsettingBlockingReasonIdForUpdate(updatePaymentRequest, payment, exceptionMessages);
        handleBlockedForOffsettingAdditionalInfoForUpdate(updatePaymentRequest, payment, exceptionMessages);
    }

    private void handleBlockedForOffsettingFromDate(CreatePaymentRequest createPaymentRequest, Payment payment, List<String> exceptionMessages, Set<String> permissions) {
        if (createPaymentRequest.getBlockedForOffsettingFromDate() == null) {
            exceptionMessages.add("blockedForOffsettingFromDate-BlockedForOffsettingFromDate should not be null;");
        } else {
            if (CollectionUtils.isEmpty(permissions)) {
                if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
                    if (createPaymentRequest.getBlockedForOffsettingToDate() == null) {
                        payment.setBlockedForOffsettingFromDate(createPaymentRequest.getBlockedForOffsettingFromDate());
                    } else {
                        if (createPaymentRequest.getBlockedForOffsettingFromDate().isBefore(createPaymentRequest.getBlockedForOffsettingToDate()) || createPaymentRequest.getBlockedForOffsettingFromDate().equals(createPaymentRequest.getBlockedForOffsettingToDate())) {
                            payment.setBlockedForOffsettingFromDate(createPaymentRequest.getBlockedForOffsettingFromDate());
                            handleBlockedForOffsettingToDate(createPaymentRequest, payment, exceptionMessages, permissions);
                        } else {
                            exceptionMessages.add("blockedForOffsettingFromDate-BlockedForOffsettingFromDate is not <= BlockedForOffsettingToDate;");
                        }
                    }
                } else {
                    exceptionMessages.add("blockedForOffsettingFromDate-You don't have permission for block offsetting;");
                }
            } else {
                if (permissions.contains(PermissionEnum.RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.getId())) {
                    if (createPaymentRequest.getBlockedForOffsettingToDate() == null) {
                        payment.setBlockedForOffsettingFromDate(createPaymentRequest.getBlockedForOffsettingFromDate());
                    } else {
                        if (createPaymentRequest.getBlockedForOffsettingFromDate().isBefore(createPaymentRequest.getBlockedForOffsettingToDate()) || createPaymentRequest.getBlockedForOffsettingFromDate().equals(createPaymentRequest.getBlockedForOffsettingToDate())) {
                            payment.setBlockedForOffsettingFromDate(createPaymentRequest.getBlockedForOffsettingFromDate());
                            handleBlockedForOffsettingToDate(createPaymentRequest, payment, exceptionMessages, permissions);
                        } else {
                            exceptionMessages.add("blockedForOffsettingFromDate-BlockedForOffsettingFromDate is not <= BlockedForOffsettingToDate;");
                        }
                    }
                } else {
                    throw new ClientException("You don't have appropriate permission: %s;".formatted(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.name()), ErrorCode.OPERATION_NOT_ALLOWED);
                }
            }

        }
    }

    private void handleBlockedForOffsettingToDate(CreatePaymentRequest createPaymentRequest, Payment payment, List<String> exceptionMessages, Set<String> permissions) {
        if (createPaymentRequest.getBlockedForOffsettingToDate() != null && createPaymentRequest.getBlockedForOffsettingToDate().isBefore(createPaymentRequest.getBlockedForOffsettingFromDate())) {
            exceptionMessages.add("blockedForOffsettingToDate-BlockedForOffsettingToDate is before BlockedForOffsettingFromDate;");
            return;
        }

        if (createPaymentRequest.getBlockedForOffsettingToDate() != null) {
            if (CollectionUtils.isEmpty(permissions)) {
                if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
                    payment.setBlockedForOffsettingToDate(createPaymentRequest.getBlockedForOffsettingToDate());
                } else {
                    exceptionMessages.add("blockedForOffsettingToDate-You don't have permission for block offsetting;");
                }
            } else {
                if (permissions.contains(PermissionEnum.RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.getId())) {
                    payment.setBlockedForOffsettingToDate(createPaymentRequest.getBlockedForOffsettingToDate());
                } else {
                    throw new ClientException("You don't have appropriate permission: %s;".formatted(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.name()), ErrorCode.OPERATION_NOT_ALLOWED);
                }
            }

        }
    }

    private void handleBlockedForOffsettingBlockingReasonId(CreatePaymentRequest createPaymentRequest, Payment payment, List<String> exceptionMessages, Set<String> permissions) {
        if (createPaymentRequest.getBlockedForOffsettingBlockingReasonId() == null) {
            exceptionMessages.add("blockedForOffsettingBlockingReasonId-BlockedForOffsettingBlockingReasonId should not be null;");
        } else {
            boolean reasonExists = blockingReasonRepository.existsByIdAndStatusIn(createPaymentRequest.getBlockedForOffsettingBlockingReasonId(), List.of(NomenclatureItemStatus.ACTIVE));
            if (reasonExists) {
                if (CollectionUtils.isEmpty(permissions)) {
                    if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
                        payment.setBlockedForOffsettingBlockingReasonId(createPaymentRequest.getBlockedForOffsettingBlockingReasonId());
                    } else {
                        exceptionMessages.add("blockedForOffsettingBlockingReasonId-You don't have permission for block offsetting;");
                    }
                } else {
                    if (permissions.contains(PermissionEnum.RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.getId())) {
                        payment.setBlockedForOffsettingBlockingReasonId(createPaymentRequest.getBlockedForOffsettingBlockingReasonId());
                    } else {
                        throw new ClientException("You don't have appropriate permission: %s;".formatted(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.name()), ErrorCode.OPERATION_NOT_ALLOWED);
                    }
                }
            } else {
                exceptionMessages.add("blockedForOffsettingBlockingReasonId-Reason id does not exists with given id [%s];".formatted(createPaymentRequest.getBlockedForOffsettingBlockingReasonId()));
            }
        }
    }

    private void handleBlockedForOffsettingAdditionalInfo(CreatePaymentRequest createPaymentRequest, Payment payment, List<String> exceptionMessages, Set<String> permissions) {
        String additionalInfo = createPaymentRequest.getBlockedForOffsettingAdditionalInfo();
        if (additionalInfo != null) {
            if (CollectionUtils.isEmpty(permissions)) {
                if (hasPermission(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING)) {
                    payment.setBlockedForOffsettingAdditionalInfo(additionalInfo);
                } else {
                    exceptionMessages.add("bBlockedForOffsettingAdditionalInfo-You don't have permission for block offsetting;");
                }
            } else {
                if (permissions.contains(PermissionEnum.RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.getId())) {
                    payment.setBlockedForOffsettingAdditionalInfo(additionalInfo);
                } else {
                    throw new ClientException("You don't have appropriate permission: %s;".formatted(RECEIVABLE_PAYMENT_BLOCK_FOR_LIABILITY_OFFSETTING.name()), ErrorCode.OPERATION_NOT_ALLOWED);
                }
            }
        }
    }

    private void validateAndSetCurrency(CreatePaymentRequest createPaymentRequest, List<String> exceptionMessages, Payment payment) {
        Optional<Currency> currency = currencyRepository.findByIdAndStatus(createPaymentRequest.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (currency.isEmpty()) {
            exceptionMessages.add(String.format("currencyId-Currency with given ID [%s] not found;", createPaymentRequest.getCurrencyId()));
        } else {
            payment.setCurrencyId(createPaymentRequest.getCurrencyId());
        }
    }

    private Optional<AccountingPeriods> validateAndSetAccountingPeriod(CreatePaymentRequest createPaymentRequest, List<String> exceptionMessages, Payment payment) {
        Optional<AccountingPeriods> accountingPeriod = accountingPeriodsRepository.findByIdAndStatus(createPaymentRequest.getAccountPeriodId(), AccountingPeriodStatus.OPEN);
        if (accountingPeriod.isEmpty()) {
            exceptionMessages.add(String.format("accountingPeriodId-AccountingPeriod with given id [%s] not found;", createPaymentRequest.getAccountPeriodId()));
        } else {
            payment.setAccountPeriodId(createPaymentRequest.getAccountPeriodId());
        }
        return accountingPeriod;
    }

    private void validateAndSetPaymentDate(
            CreatePaymentRequest createPaymentRequest,
            Optional<AccountingPeriods> accountingPeriod,
            Payment payment,
            List<String> exceptionMessages
    ) {
        // Step 1: Check if the accountingPeriod is present
        if (accountingPeriod.isPresent()) {
            AccountingPeriods period = accountingPeriod.get();

            // Step 2: Check if the payment date is within the accounting period's start and end date
            if ((Boolean.TRUE.equals(createPaymentRequest.getFromPaymentCancel())) ||
                    (!period.getStartDate().isAfter(createPaymentRequest.getPaymentDate().atStartOfDay()) &&
                            !createPaymentRequest.getPaymentDate().atStartOfDay().isAfter(period.getEndDate()))
            ) {
                payment.setPaymentDate(createPaymentRequest.getPaymentDate());
            } else {
                exceptionMessages.add(
                        String.format("paymentDate-PaymentDate is before accounting period with id [%s];", createPaymentRequest.getAccountPeriodId())
                );
            }
        } else {
            // Step 5: If accountingPeriod is not present, we do nothing (or can handle it as needed)
            exceptionMessages.add("paymentDate-PaymentDate Accounting period not found.");
        }
    }

    private void validateAndSetPaymentDateForOnlinePayment(CreatePaymentRequest createPaymentRequest, Optional<AccountingPeriods> accountingPeriod, Payment payment, List<String> exceptionMessages) {
        if (accountingPeriod.isPresent()) {
            LocalDateTime paymentDateTime = createPaymentRequest.getPaymentDate().atStartOfDay();
            LocalDateTime startDateTime = accountingPeriod.get().getStartDate();
            LocalDateTime endDateTime = accountingPeriod.get().getEndDate();

            boolean isAfterOrEqualStartDate = !startDateTime.isAfter(paymentDateTime);
            boolean isBeforeEndDate = paymentDateTime.isBefore(endDateTime);

            if (isAfterOrEqualStartDate && isBeforeEndDate) {
                payment.setPaymentDate(createPaymentRequest.getPaymentDate());
            } else {
                exceptionMessages.add(String.format("paymentDate-PaymentDate is outside the accounting period with id [%s];", createPaymentRequest.getAccountPeriodId()));
            }
        } else {
            exceptionMessages.add(String.format("Accounting period with id [%s] is not present;", createPaymentRequest.getAccountPeriodId()));
        }
    }

    private boolean hasPermission(PermissionEnum permission) {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.RECEIVABLE_PAYMENT, List.of(permission));
    }

    private BigDecimal executePaymentOffsetting(Long paymentId) {
        return automaticOffsettingService.offsetOfPayments(
                paymentId,
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
        );
    }

    /**
     * Creates a receivable record from the given payment and remaining payment amount after offsetting.
     *
     * @param payment                             the payment object containing the details of the payment.
     * @param paymentCurrentAmountAfterOffsetting the remaining amount of the payment after offsetting has been applied.
     * @return the identifier of the newly created receivable record as a Long.
     */
    private Long createReceivableFromPayment(Payment payment, BigDecimal paymentCurrentAmountAfterOffsetting, boolean needAutomaticOffsetting) {
        return customerReceivableService.createFromPayment(payment, paymentCurrentAmountAfterOffsetting, needAutomaticOffsetting);
    }

    private Long directReceivableOffsetting(Payment payment, BigDecimal amount, PaymentCancelRequest request) {
        Long receivableFromPayment = createReceivableFromPayment(payment, amount, false);
        CustomerReceivable receivable = customerReceivableRepository.findById(receivableFromPayment)
                .orElseThrow(() -> new DomainEntityNotFoundException("Receivable with id %s not found;".formatted(receivableFromPayment)));

        if (Boolean.TRUE.equals(request.getBlockedForOffsetting())) {
            receivable.setBlockedForPayment(true);
            receivable.setBlockedForPaymentFromDate(request.getBlockedForOffsettingFromDate());
            receivable.setBlockedForPaymentToDate(request.getBlockedForOffsettingToDate());
            receivable.setBlockedForPaymentBlockingReasonId(request.getReasonId());
        }

        customerReceivableRepository.saveAndFlush(receivable);
        receivableDirectOffsettingService.directOffsetting(
                DirectOffsettingSourceType.PAYMENT,
                payment.getId(),
                receivableFromPayment,
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                OperationContext.APO.name()
        );

        em.refresh(receivable);
        return receivableFromPayment;
    }

    private boolean hasBlockedForOffsettingChanged(UpdatePaymentRequest request, Payment payment) {
        boolean currentBlockedStatus = Boolean.TRUE.equals(payment.getBlockedForOffsetting());

        if (request.isBlockedForOffsetting() != currentBlockedStatus) {
            return true;
        }

        if (request.isBlockedForOffsetting()) {
            if (request.getBlockedForOffsettingFromDate() != null &&
                    !Objects.equals(request.getBlockedForOffsettingFromDate(), payment.getBlockedForOffsettingFromDate())) {
                return true;
            }

            if (request.getBlockedForOffsettingToDate() != null &&
                    !Objects.equals(request.getBlockedForOffsettingToDate(), payment.getBlockedForOffsettingToDate())) {
                return true;
            }

            if (request.getBlockedForOffsettingBlockingReasonId() != null &&
                    !Objects.equals(request.getBlockedForOffsettingBlockingReasonId(), payment.getBlockedForOffsettingBlockingReasonId())) {
                return true;
            }

            return request.getBlockedForOffsettingAdditionalInfo() != null &&
                    !Objects.equals(request.getBlockedForOffsettingAdditionalInfo(), payment.getBlockedForOffsettingAdditionalInfo());
        }

        return false;
    }
}
