package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

import org.springframework.core.io.ByteArrayResource;

public record DisconnectionOfPowerSupplyTemplateContent(String fileName, ByteArrayResource content) {

}
