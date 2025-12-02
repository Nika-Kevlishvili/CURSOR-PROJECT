package bg.energo.phoenix.model.response.termsGroup;

import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TermsGroupListingResponse {
    private Long id;
    private String name;
    private Boolean noInterestOnOverdueDebts;
    private TermGroupStatus status;
    private LocalDateTime dateOfCreation;
}
