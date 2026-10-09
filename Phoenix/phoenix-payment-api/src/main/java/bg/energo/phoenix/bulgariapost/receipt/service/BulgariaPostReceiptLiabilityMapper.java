package bg.energo.phoenix.bulgariapost.receipt.service;

import bg.energo.phoenix.bulgariapost.liabilities.repository.BulgariaPostLiabilitiesProjection;
import bg.energo.phoenix.payment.receipt.model.PaymentReceiptLiabilityContext;

import java.util.List;

final class BulgariaPostReceiptLiabilityMapper {

    private BulgariaPostReceiptLiabilityMapper() {
    }

    static List<PaymentReceiptLiabilityContext> toContexts(List<BulgariaPostLiabilitiesProjection> projections) {
        if (projections == null) {
            return List.of();
        }
        return projections.stream().map(BulgariaPostReceiptLiabilityMapper::toContext).toList();
    }

    static PaymentReceiptLiabilityContext toContext(BulgariaPostLiabilitiesProjection projection) {
        return new PaymentReceiptLiabilityContext(
                projection.getLiabilityId(),
                projection.getLiabilityNumber(),
                projection.getOccurrenceDate(),
                projection.getDueDate(),
                projection.getCurrentAmount(),
                projection.getOutgoingDocumentFromExternalSystem(),
                projection.getInvoiceNumber(),
                projection.getOutgoingDocumentNumber(),
                projection.getBasisForIssuing(),
                projection.getMeterReadingPeriodFrom(),
                projection.getMeterReadingPeriodTo(),
                projection.getCustomerName(),
                projection.getCustomerAddress(),
                projection.getCustomerNumber(),
                projection.getBillingGroup(),
                projection.getCurrency(),
                projection.getInvoiceId(),
                projection.getOutgoingDocumentType(),
                projection.getLatePaymentFineId(),
                projection.getActionId(),
                projection.getClaimedPenaltyId(),
                projection.getReschedulingId(),
                projection.getDepositId()
        );
    }
}
