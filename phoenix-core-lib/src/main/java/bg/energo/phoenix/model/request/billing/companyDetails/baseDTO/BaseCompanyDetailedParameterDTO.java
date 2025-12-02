package bg.energo.phoenix.model.request.billing.companyDetails.baseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class BaseCompanyDetailedParameterDTO {
    private String parameter;
    private String parameterTranslated;

}
