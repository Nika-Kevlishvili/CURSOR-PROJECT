package bg.energo.phoenix.systech.receipt.service;

import bg.energo.phoenix.systech.liabilities.repository.SystechLiabilitiesProjection;
import bg.energo.phoenix.payment.receipt.model.PaymentReceiptLiabilityContext;

import java.util.List;

final class SystechReceiptLiabilityMapper {

    private SystechReceiptLiabilityMapper() {
    }

    static List<PaymentReceiptLiabilityContext> toContexts(List<SystechLiabilitiesProjection> projections) {
        if (projections == null) {
            return List.of();
        }
        return projections.stream().map(SystechReceiptLiabilityMapper::toContext).toList();
    }

    static PaymentReceiptLiabilityContext toContext(SystechLiabilitiesProjection projection) {
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
