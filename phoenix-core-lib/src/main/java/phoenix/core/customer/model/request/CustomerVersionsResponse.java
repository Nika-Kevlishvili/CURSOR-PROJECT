package phoenix.core.customer.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerVersionsResponse {
    private Long id;
    private LocalDateTime createDate;
}
