package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellationFile;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.Data;

@Data
public class InvoiceCancellationFileResponse {

    private Long id;
    private String name;

    public InvoiceCancellationFileResponse(InvoiceCancellationFile invoiceCancellationFile) {
        this.id = invoiceCancellationFile.getId();
        String name = invoiceCancellationFile.getName();
        try {
            this.name = name.substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (IndexOutOfBoundsException e) {
            this.name = name;
        }
    }

}
