package bg.energo.phoenix.model.response.terms;

import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import lombok.Data;

@Data
public class TermsShortResponse {
    private Long id;
    private String name;

    public TermsShortResponse(Terms terms) {
        this.id = terms.getId();
        this.name = "%s (%s)".formatted(terms.getName(), terms.getId());
    }
}
