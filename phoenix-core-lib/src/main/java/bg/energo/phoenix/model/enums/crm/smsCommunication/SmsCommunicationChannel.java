package bg.energo.phoenix.model.enums.crm.smsCommunication;

import lombok.Getter;

@Getter
public enum SmsCommunicationChannel {
    SMS(KindOfCommunicationSms.Individual),
    MASS_SMS(KindOfCommunicationSms.Mass);

    private final KindOfCommunicationSms kindOfCommunicationSms;

    private SmsCommunicationChannel(KindOfCommunicationSms kindOfCommunicationSms) {
        this.kindOfCommunicationSms = kindOfCommunicationSms;
    }
}
