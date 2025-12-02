package bg.energo.phoenix.model.response.billing.invoice;

import org.springframework.core.io.ByteArrayResource;

public record InvoiceCancellationFileContent(String fileName, ByteArrayResource content) {
}

