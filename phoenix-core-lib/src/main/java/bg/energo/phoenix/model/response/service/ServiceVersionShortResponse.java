package bg.energo.phoenix.model.response.service;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class ServiceVersionShortResponse {
    Long id; // this is the ID of the version
    String name;

    public ServiceVersionShortResponse(Long id,
                                       String name,
                                       Long version,
                                       String customerIdentifier) {
        this.id = id;
        if (StringUtils.isNotEmpty(customerIdentifier)) {
            this.name = "%s".formatted(name);
        } else {
            this.name = "%s (Version %s)".formatted(name, version);
        }
    }
}
