package bg.energo.phoenix.model.response.billing.billingRun;

import org.springframework.core.io.ByteArrayResource;

public record ManualInvoiceTemplateContent(String fileName, ByteArrayResource content) {
}
