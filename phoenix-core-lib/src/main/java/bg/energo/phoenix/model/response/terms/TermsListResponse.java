package bg.energo.phoenix.model.response.terms;

import java.time.LocalDateTime;

public interface TermsListResponse {
    Long getId();
    String getName();
    Boolean getNoInterestOnOverdueDebts();
    String getAvailable();
    LocalDateTime getDateOfCreation();
    String getStatus();
}
