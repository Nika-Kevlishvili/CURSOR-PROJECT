package bg.energo.phoenix.model.response.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CostCenterAndIncomeAccountResponse {

    private Long detailId;

    private String costCenter;

    private String incomeAccount;

}
