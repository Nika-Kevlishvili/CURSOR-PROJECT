package bg.energo.phoenix.model.response.copy.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopyDomainWithVersionMiddleResponse {

    private Long id;
    private Long versionId;
    private LocalDateTime startDate;

    public CopyDomainWithVersionMiddleResponse(Long id, Long versionId, LocalDate startDate) {
        this.id = id;
        this.versionId = versionId;
        this.startDate = startDate.atStartOfDay();
    }
    public CopyDomainWithVersionMiddleResponse(Long id, Long versionId) {
        this.id = id;
        this.versionId = versionId;
    }
}
