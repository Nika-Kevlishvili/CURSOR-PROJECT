package bg.energo.phoenix.model.response.template;

import org.springframework.core.io.ByteArrayResource;

public record TemplateFileContent(String name, ByteArrayResource resource) {
}
