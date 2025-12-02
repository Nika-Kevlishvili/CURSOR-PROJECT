package bg.energo.phoenix.model.response.billing.file;

import bg.energo.phoenix.model.entity.contract.billing.BillingInvoicesFile;
import bg.energo.phoenix.util.epb.EPBFinalFields;

public record BillingFileResponse(Long id, String name) {

    public BillingFileResponse(BillingInvoicesFile billingInvoicesFile) {
        this(
                billingInvoicesFile.getId(),
                billingInvoicesFile.getName().length() > EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME ?
                        billingInvoicesFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME) :
                        billingInvoicesFile.getName()
        );
    }

}
