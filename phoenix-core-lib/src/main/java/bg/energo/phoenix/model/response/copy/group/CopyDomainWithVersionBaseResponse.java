package bg.energo.phoenix.model.response.copy.group;

import lombok.Data;

@Data
public class CopyDomainWithVersionBaseResponse {

    private String displayName;
    private Long id;

    public CopyDomainWithVersionBaseResponse(Long id, String displayName) {
        this.displayName = displayName;
        this.id = id;
    }
}
