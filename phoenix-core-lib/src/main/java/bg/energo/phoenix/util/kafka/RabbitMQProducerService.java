package bg.energo.phoenix.util.kafka;

import bg.energo.phoenix.process.model.request.ProcessCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev","test","local"})
public class RabbitMQProducerService {

    private final RabbitTemplate rabbitTemplate;
    @Value("${rabbit.mass-import.queue}")
    private String massImportQueues;


    public void publishProcessEvent(ProcessCreatedEvent processCreatedEvent) {
        rabbitTemplate.convertAndSend(massImportQueues, processCreatedEvent);
    }
}
