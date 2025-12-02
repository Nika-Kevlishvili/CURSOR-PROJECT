package bg.energo.phoenix.repository.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.response.receivable.paymentPackage.PaymentPackageErrorProtocolResponse;
import bg.energo.phoenix.model.response.receivable.paymentPackage.PaymentPackageListingMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface PaymentPackageRepository extends JpaRepository<PaymentPackage, Long> {

    Optional<PaymentPackage> findPaymentPackageByIdAndLockStatusIn(Long id, List<PaymentPackageLockStatus> status);

    Optional<PaymentPackage> findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn(Long id, Long collectionChannelId, LocalDate paymentDate, List<PaymentPackageLockStatus> status);

    boolean existsByCollectionChannelId(Long id);

    @Query(nativeQuery = true,
            value = """
                                select id,
                                collectionChannel,
                                status,
                                entityStatus,
                                paymentDate
                                from 
                                (select
                                    pp.id as id,
                                    (select cc.name from receivable.collection_channels cc where cc.id = pp.collection_channel_id) as collectionChannel,
                                    pp.payment_package_status as status,
                                    pp.status as entityStatus,
                                    pp.payment_date as paymentDate
                                from
                                    receivable.payment_packages pp
                                where
                                    ((:collectionChannelIds) is null or pp.collection_channel_id in :collectionChannelIds)
                                    and
                                    ((:status) is null or text(pp.payment_package_status) in (:status))
                                    and
                                    ((:entityStatus) is null or text(pp.status) in :entityStatus)
                                    and
                                    (date(:paymentDateFrom) is null or pp.payment_date >= date(:paymentDateFrom))
                                    and
                                    (date(:paymentDateTo) is null or pp.payment_date <= date(:paymentDateTo))
                                    and
                                    (:prompt is null or
                                        (
                                            :searchBy = 'ALL' and (
                                                lower(text(pp.id)) like :prompt
                                            )
                                        ) or (
                                            (
                                                :searchBy = 'NUMBER' and lower(text(pp.id)) like :prompt
                                            )
                                        )
                                    )) as tbl
                    """,
            countQuery = """
                                select
                                    count(tbl.id)
                                from
                                 (select
                                    pp.id as id,
                                    (select cc.name from receivable.collection_channels cc where cc.id = pp.collection_channel_id) as collectionChannel,
                                    pp.payment_package_status as status,
                                    pp.status as entityStatus,
                                    pp.payment_date as paymentDate
                                from
                                    receivable.payment_packages pp
                                where
                                    ((:collectionChannelIds) is null or pp.collection_channel_id in :collectionChannelIds)
                                    and
                                    ((:status) is null or text(pp.payment_package_status) in (:status))
                                    and
                                    ((:entityStatus) is null or text(pp.status) in :entityStatus)
                                    and
                                    (date(:paymentDateFrom) is null or pp.payment_date >= date(:paymentDateFrom))
                                    and
                                    (date(:paymentDateTo) is null or pp.payment_date <= date(:paymentDateTo))
                                    and
                                    (:prompt is null or
                                        (
                                            :searchBy = 'ALL' and (
                                                lower(text(pp.id)) like :prompt
                                            )
                                        ) or (
                                            (
                                                :searchBy = 'NUMBER' and lower(text(pp.id)) like :prompt
                                            )
                                        )
                                    )) as tbl
                    """
    )
    Page<PaymentPackageListingMiddleResponse> filter(
            @Param("collectionChannelIds") List<Long> collectionChannelIds,
            @Param("status") List<String> status,
            @Param("paymentDateFrom") LocalDate paymentDateFrom,
            @Param("paymentDateTo") LocalDate paymentDateTo,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("entityStatus") List<String> entityStatus,
            Pageable pageable
    );

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.receivable.paymentPackage.PaymentPackageErrorProtocolResponse(
                            p.id,
                            cast(p.initialAmount as string),
                            cast(p.currentAmount as string),
                            pro.customerReceivableId
                        )
                        from Payment p
                        join PaymentReceivableOffsetting pro on pro.customerPaymentId = p.id
                        where p.paymentPackageId = :paymentPackageId
                    """
    )
    List<PaymentPackageErrorProtocolResponse> findReceivablesAndLiabilities(Long paymentPackageId);

    @Query(value = """
            select pp from PaymentPackage pp
            where pp.paymentDate = :paymentDate
            and pp.collectionChannelId = :collectionChannelId
            and pp.status = 'ACTIVE'
            and pp.type = 'ONLINE'
            and pp.lockStatus = 'UNLOCKED'
            """
    )
    Optional<PaymentPackage> findPaymentPackageByPaymentDateAndLockStatusInAndCollectionChannelId(LocalDate paymentDate, Long collectionChannelId);

    @Query("""
    select pp
    from PaymentPackage pp
    where pp.lockStatus = 'UNLOCKED'
    and pp.type = 'ONLINE'
    and pp.createDate > :startDate and pp.createDate < :endDate

""")
    List<PaymentPackage> getTodayCreatedPackage(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
