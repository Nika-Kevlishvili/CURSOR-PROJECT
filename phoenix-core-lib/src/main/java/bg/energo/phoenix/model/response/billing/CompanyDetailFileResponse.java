package bg.energo.phoenix.model.response.billing;

import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyLogos;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailFileResponse {
    private Long id;
    private String fileName;

    public CompanyDetailFileResponse(CompanyLogos companyLogos) {
        this.id = companyLogos.getId();
        try {
            this.fileName = companyLogos.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = companyLogos.getName();
        }
    }
}
