package bg.energo.phoenix.model.response.receivable.customerAssessment;

import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerAssessmentParametersResponse {

    private ShortResponse parameterCriteria;

    private String parameterValue;

    private Assessment parameterAssessment;

    private Assessment parameterFinalAssessment;

    private Boolean isValue;

    private BigDecimal valueFrom;

    private BigDecimal valueTo;

    private String customerType;

}
