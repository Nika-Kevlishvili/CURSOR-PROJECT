package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BasePeriodOfYearDto implements Comparable<BasePeriodOfYearDto> {
    private String startDate;

    private String endDate;

    @Override
    public int compareTo(BasePeriodOfYearDto basePeriodOfYearDto) {
        return Integer.compare(dateToNumber(startDate), dateToNumber(basePeriodOfYearDto.startDate));
    }

    public static int dateToNumber(String date) {
        String result = date.substring(0, 2) + date.substring(3);
        if (result.charAt(0) == '0') result = result.substring(1);
        return Integer.parseInt(result);
    }
}
