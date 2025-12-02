package bg.energo.phoenix.model.request.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <h1>CustomerVersionsResponse</h1>
 * {@link #id} customer details version id
 * {@link #createDate} creation date of a customer version
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerVersionsResponse {
    private Long id;
    private LocalDateTime createDate;
}
