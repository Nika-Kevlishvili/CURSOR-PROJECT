package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.Status;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSegmentResponse {
    private Long id;
    private CustomerDetailsResponse customerDetail;
    private SegmentResponse segment;
    private Status status;
}
