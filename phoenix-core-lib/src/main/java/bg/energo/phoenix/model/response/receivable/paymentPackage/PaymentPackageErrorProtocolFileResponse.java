package bg.energo.phoenix.model.response.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackageFiles;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.Data;

@Data
public class PaymentPackageErrorProtocolFileResponse {
    private Long id;
    private String fileName;

    public PaymentPackageErrorProtocolFileResponse(PaymentPackageFiles paymentPackageFiles) {
        this.id = paymentPackageFiles.getId();

        try {
            this.fileName = paymentPackageFiles.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = paymentPackageFiles.getName();
        }
    }
}
