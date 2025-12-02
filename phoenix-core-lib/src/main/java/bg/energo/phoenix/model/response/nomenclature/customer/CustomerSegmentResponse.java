package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSegmentResponse {
    private Long id;
    private SegmentResponse segment;
    private Status status;
}
