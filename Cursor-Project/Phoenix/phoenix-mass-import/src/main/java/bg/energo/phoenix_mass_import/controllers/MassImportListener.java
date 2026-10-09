package bg.energo.phoenix_mass_import.controllers;

import bg.energo.phoenix.process.ProcessEventHandler;
import bg.energo.phoenix.process.model.request.ProcessCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MassImportListener {

    private final ProcessEventHandler processEventHandler;

    @RabbitListener(queues = "${rabbit.mass-import.queue}")
    public void listenToMassImport(ProcessCreatedEvent message) {
        log.info("MassImportListener: received process event!; {}", message.getMetadata().toString());
        processEventHandler.handleEvent(message);
    }
}
