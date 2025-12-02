package bg.energo.phoenix.service.archivation.edms;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class JobStatusService {

    private final AtomicBoolean isJobRunning = new AtomicBoolean(false);

    public boolean isPreviousJobCompleted() {
        return !isJobRunning.get();
    }

    public void markJobAsRunning() {
        isJobRunning.set(true);
    }

    public void markJobAsCompleted() {
        isJobRunning.set(false);
    }
}
