package bg.energo.phoenix.virtualpos.service;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.service.receivable.payment.PaymentService;
import bg.energo.phoenix.service.receivable.paymentPackage.PaymentPackageService;
import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentEntity;
import bg.energo.phoenix.virtualpos.model.enums.VirtualPosPaymentStatus;
import bg.energo.phoenix.virtualpos.model.enums.VirtualPosStatusCode;
import bg.energo.phoenix.virtualpos.repository.VirtualPosPaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualPosPaymentCreationTransactionalService {

    private final VirtualPosMapperService virtualPosMapperService;
    private final PaymentService paymentService;
    private final PaymentPackageRepository paymentPackageRepository;
    private final PaymentPackageService paymentPackageService;
    private final VirtualPosPaymentRepository virtualPosPaymentRepository;
    @PersistenceContext
    protected EntityManager entityManager;

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
            return null;
        }
        BigDecimal convertedAmount = bg.energo.phoenix.utils.EPBPaymentCalculationUtils.convertToUnitAmount(amount);
        CreatePaymentRequest request = virtualPosMapperService.buildPaymentObjectCreateRequest(
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

    @Transactional
    public void saveVirtualPosPaymentConfirmationDetails(
            VirtualPosPaymentEntity paymentEntity,
            String requestDateString,
            Long totalAmount,
            Long paymentId
    ) {
        paymentEntity.setConfirmDate(bg.energo.phoenix.util.epb.BulgariaTimeUtils.bgToUtc(bg.energo.phoenix.utils.EPBFunctionUtils.localDateTimeFromDateString(requestDateString)));
        paymentEntity.setConfirmedAmount(bg.energo.phoenix.utils.EPBPaymentCalculationUtils.convertToUnitAmount(totalAmount));
        paymentEntity.setPaymentStatus(VirtualPosPaymentStatus.CONFIRMED);
        paymentEntity.setConfirmedTotalCoinAmount(totalAmount);
        paymentEntity.setPaymentId(paymentId);
        paymentEntity.setStatusCode(VirtualPosStatusCode.OK.getCode());
        virtualPosPaymentRepository.saveAndFlush(paymentEntity);
    }

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
                    bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageType.ONLINE,
                    bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus.UNLOCKED,
                    collectionChannelId,
                    accountingPeriodId,
                    paymentPackageDate
            );
            log.info("New PaymentPackage created with ID: {}", newPaymentPackage.getId());
            return newPaymentPackage;
        } catch (DataIntegrityViolationException ex) {
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
}
