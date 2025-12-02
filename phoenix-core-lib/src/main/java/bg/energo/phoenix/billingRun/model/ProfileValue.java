package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileValue {
    //Profile Value
    private BigDecimal v;
    //Price from formula
    private List<ProfilePrice> p;
}
