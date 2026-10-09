package bg.energo.phoenix.cashterminal.service;

import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalPaymentStatus;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalStatusCode;
import bg.energo.phoenix.cashterminal.repository.CashTerminalPaymentRepository;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.service.receivable.payment.PaymentService;
import bg.energo.phoenix.service.receivable.paymentPackage.PaymentPackageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CashTerminalPaymentCreationTransactionalService {

    private final CashTerminalMapperService cashTerminalMapperService;
    private final PaymentService paymentService;
    private final PaymentPackageRepository paymentPackageRepository;
    private final PaymentPackageService paymentPackageService;
    private final CashTerminalPaymentRepository cashTerminalPaymentRepository;
    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                LocalDate.now()
        );
        if (Objects.isNull(paymentPackage)) {
            return null;
        }
        BigDecimal convertedAmount = bg.energo.phoenix.utils.EPBPaymentCalculationUtils.convertToUnitAmount(amount);
        CreatePaymentRequest request = cashTerminalMapperService.buildPaymentObjectCreateRequest(
                customer,
                collectionChannel,
                paymentPackage,
                convertedAmount,
                contractBillingGroupId,
                paymentDate
        );
        Long paymentId = paymentService.createFromOnlinePayment(request, null);
        entityManager.flush();
        return paymentId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCashTerminalPaymentConfirmationDetails(
            CashTerminalPaymentEntity paymentEntity,
            String requestDateString,
            Long totalAmount,
            Long paymentId
    ) {
        paymentEntity.setConfirmDate(bg.energo.phoenix.utils.EPBFunctionUtils.localDateTimeFromDateString(requestDateString));
        paymentEntity.setConfirmedAmount(bg.energo.phoenix.utils.EPBPaymentCalculationUtils.convertToUnitAmount(totalAmount));
        paymentEntity.setPaymentStatus(CashTerminalPaymentStatus.CONFIRMED);
        paymentEntity.setConfirmedTotalCoinAmount(totalAmount);
        paymentEntity.setPaymentId(paymentId);
        paymentEntity.setStatusCode(CashTerminalStatusCode.OK.getCode());
        cashTerminalPaymentRepository.saveAndFlush(paymentEntity);
    }

    private PaymentPackage createOrFetchPaymentPackage(
            Long collectionChannelId,
            Long accountingPeriodId,
            LocalDate paymentPackageDate
    ) {
        Optional<PaymentPackage> paymentPackage = paymentPackageRepository.findPaymentPackageByPaymentDateAndLockStatusInAndCollectionChannelId(
                paymentPackageDate,
                collectionChannelId
        );
        if (paymentPackage.isPresent()) {
            return paymentPackage.get();
        } else {
            return paymentPackageService.createFromOnlinePayment(
                    bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageType.ONLINE,
                    bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus.UNLOCKED,
                    collectionChannelId,
                    accountingPeriodId,
                    paymentPackageDate
            );
        }
    }
}

