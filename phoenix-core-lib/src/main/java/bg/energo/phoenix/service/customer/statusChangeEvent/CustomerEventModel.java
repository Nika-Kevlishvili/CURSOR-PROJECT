package bg.energo.phoenix.service.customer.statusChangeEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class CustomerEventModel {
    private List<Long> customerDetailIds;

    public CustomerEventModel(List<Long> customerDetailIds){
        this.customerDetailIds = customerDetailIds;
    }
}
