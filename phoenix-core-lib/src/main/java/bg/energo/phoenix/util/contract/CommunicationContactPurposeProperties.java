package bg.energo.phoenix.util.contract;

import lombok.Getter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CommunicationContactPurposeProperties {

    private final Long billingCommunicationId;
    private final Long contractCommunicationId;

    public CommunicationContactPurposeProperties(Environment environment) {
        this.billingCommunicationId = environment.getProperty("communication.billing.contact.purpose", Long.class);
        this.contractCommunicationId = environment.getProperty("communication.contract.contact.purpose", Long.class);
    }

}
