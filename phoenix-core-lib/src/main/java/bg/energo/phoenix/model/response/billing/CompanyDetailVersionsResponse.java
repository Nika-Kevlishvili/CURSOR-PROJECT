package bg.energo.phoenix.model.response.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailVersionsResponse {
    private Long versionId;
    private LocalDate createDate;
}
