package bg.energo.phoenix.service.product.termination.terminations;

import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.model.response.terminations.TerminationForNotificationResponse;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerminationNoticeEmailJobService {

    private final EmailCommunicationService emailCommunicationService;

    private final TerminationRepository terminationRepository;

    private final EmailMailboxesRepository emailMailboxesRepository;


    @Value("${nomenclature.contract-termination.communication-topic.id}")
    private Long contractTerminationTopicId;

    @Transactional
    public List<String> sendTerminationEmails() {
        List<TerminationForNotificationResponse> terminationsToNotify = terminationRepository.retrieveForTerminationNotification();

        return terminationsToNotify.stream()
                .map(this::createEmailModel)
                .toList();
    }

    private EmailMailboxes getDefaultMailbox() {
        return emailMailboxesRepository.findByDefaultSelectionTrue().orElse(null);
    }

    protected String createEmailModel(TerminationForNotificationResponse response) {
        return "Action id: %s, Termination id: %s, Email Communication id: %s"
                .formatted(response.getActionId(),
                        response.getTerminationId(),
                        emailCommunicationService.createEmailFromDocument(
                                DocumentEmailCommunicationCreateRequest
                                        .builder()
                                        .emailBoxId(getDefaultMailbox().getId())
                                        .emailBody(generateEmailBody(response))
                                        .emailSubject(response.getEmailSubject())
                                        .emailTemplateId(response.getTemplateId())
                                        .communicationTopicId(contractTerminationTopicId)
                                        .customerCommunicationId(response.getCommunicationId())
                                        .customerDetailId(response.getCustomerDetailId())
                                        .customerEmailAddress(response.getEmails())
                                        .build(), false));
    }


    private String generateEmailBody(TerminationForNotificationResponse termination) {
        // todo: Implement email body generation logic from template
        return "<p>Contract Termination Notice Body</p>";
    }

}
