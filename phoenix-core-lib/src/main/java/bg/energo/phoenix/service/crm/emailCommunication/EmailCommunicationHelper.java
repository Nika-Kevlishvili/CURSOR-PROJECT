package bg.energo.phoenix.service.crm.emailCommunication;

import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailCommunicationHelper {

    private final EmailCommunicationRepository emailCommunicationRepository;

    /**
     * Saves the EmailCommunication entity to the database and flushes changes immediately.
     * This method operates within its own transactional context with propagation behavior set to REQUIRES_NEW,
     * ensuring that changes are persisted independently of any surrounding transactions.
     *
     * @param emailCommunication The EmailCommunication entity to save and flush.
     * @return The saved EmailCommunication entity with updated state.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailCommunication saveAndFlush(EmailCommunication emailCommunication) {
        return emailCommunicationRepository.saveAndFlush(emailCommunication);
    }
}
