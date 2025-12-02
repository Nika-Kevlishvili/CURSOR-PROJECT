package bg.energo.phoenix.model.request.product.term.termsGroup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TermsGroupEditRequest {

    @Size(min = 1, max = 1024, message = "name-length should be 1/1024 characters;")
    private String name;

    @NotNull(message = "termsId-must not be null;")
    private Long termsId;

    @NotNull(message = "detailsVersion-detailsVersion version is required;")
    private Long detailsVersion;

    @NotNull(message = "updateExistingVersion-updateExistingVersion is required;")
    private Boolean updateExistingVersion;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime versionStartDate;

}
