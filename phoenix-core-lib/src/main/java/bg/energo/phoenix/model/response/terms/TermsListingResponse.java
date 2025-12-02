package bg.energo.phoenix.model.response.terms;

import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TermsListingResponse {

    private Long id;
    private String name;
    private Boolean noInterestOnOverdueDebts;
    private Boolean available;
    private TermStatus status;
    private LocalDateTime dateOfCreation;


}
