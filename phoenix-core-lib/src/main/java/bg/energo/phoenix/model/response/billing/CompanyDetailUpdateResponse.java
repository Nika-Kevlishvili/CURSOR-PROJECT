package bg.energo.phoenix.model.response.billing;

import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailUpdateResponse {
    private Long companyId;
    private Long versionId;


    public static CompanyDetailUpdateResponse toCompanyDetailUpdateResponse(CompanyDetails companyDetails) {
        return CompanyDetailUpdateResponse.builder()
                .companyId(companyDetails.getCompanyId())
                .versionId(companyDetails.getVersionId())
                .build();
    }
}
