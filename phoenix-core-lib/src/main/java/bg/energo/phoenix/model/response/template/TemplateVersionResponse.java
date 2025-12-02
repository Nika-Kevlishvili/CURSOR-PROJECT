package bg.energo.phoenix.model.response.template;

import java.time.LocalDate;

public record TemplateVersionResponse(Long id,
                                      Integer versionId,
                                      LocalDate startDate) {
}
