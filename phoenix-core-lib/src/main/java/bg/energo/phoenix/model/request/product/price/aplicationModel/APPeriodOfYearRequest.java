package bg.energo.phoenix.model.request.product.price.aplicationModel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class APPeriodOfYearRequest implements Comparable<APPeriodOfYearRequest> {
    private Long id;
    @NotBlank(message = "applicationModelRequest.volumesByScaleRequest.periodsOfYear.startDate-Start Date is required;")
    private String startDate;

    @NotBlank(message = "applicationModelRequest.volumesByScaleRequest.periodsOfYear.endDate-End Date is required;")
    private String endDate;

    @Override
    public int compareTo(APPeriodOfYearRequest periodOfYear) {
        return Integer.compare(dateToNumber(startDate), dateToNumber(periodOfYear.startDate));
    }

    public static int dateToNumber(String date){
        String result = date.substring(0,2) + date.substring(3);
        if(result.charAt(0) == '0') result = result.substring(1);
        return Integer.parseInt(result);
    }
}
