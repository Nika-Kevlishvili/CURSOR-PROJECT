package bg.energo.phoenix.process;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.process.model.request.ProcessCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProcessEventHandler {

    private final List<BaseProcessHandler> processHandlers;

    /**
     * Handle {@link ProcessCreatedEvent} by dispatching it to the appropriate process handler.
     *
     * @param event {@link ProcessCreatedEvent} to be handled
     * @throws DomainEntityNotFoundException if no suitable process handler is found
     */
    public void handleEvent(ProcessCreatedEvent event) {
        log.debug("Received event : {}", event);
        var type = EventType.valueOf(event.getMetadata().getType());
        var processor = processHandlers.stream()
                .filter(baseProcessHandler -> baseProcessHandler.supports(type))
                .findAny()
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        String.format("Unable to find handler for event type : %s".formatted(type))
                ));
        processor.handleProcessCreatedEvent(event);
    }
}
