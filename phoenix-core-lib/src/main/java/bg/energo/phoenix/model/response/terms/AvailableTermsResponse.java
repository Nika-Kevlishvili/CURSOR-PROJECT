package bg.energo.phoenix.model.response.terms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableTermsResponse {
    private Long id;
    private String name;
    private String fullName;
    private LocalDateTime dateOfCreation;

    public AvailableTermsResponse(Long id, String name, LocalDateTime dateOfCreation) {
        this.id = id;
        this.name = name;
        this.fullName = "%s (%s)".formatted(this.name, this.id.toString());
        this.dateOfCreation = dateOfCreation;
    }
}
