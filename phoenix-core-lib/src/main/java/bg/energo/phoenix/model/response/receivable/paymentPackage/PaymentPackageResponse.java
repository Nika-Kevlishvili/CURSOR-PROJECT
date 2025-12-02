package bg.energo.phoenix.model.response.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelShortResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentPackageResponse {

    private Long id;

    private PaymentPackageLockStatus lockStatus;

    private CollectionChannelShortResponse collectionChannel;

    private AccountingPeriodShortResponse accountingPeriod;

    private LocalDate paymentDate;

    private EntityStatus entityStatus;

    private List<PaymentPackageStatusChangeHistoryShortResponse> statusChangeHistory;

    private PaymentPackageErrorProtocolFileResponse errorProtocolFileId;

}
