package bg.energo.phoenix.process.service;

import bg.energo.phoenix.process.model.response.ProcessedRecordInfoResponse;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedRecordInfoService {

    private final ProcessedRecordInfoRepository processedRecordInfoRepository;
    private final ProcessedRecordInfoMapper processedRecordInfoMapper;

    public List<ProcessedRecordInfoResponse> getRecordsByProcessIdAndStatus(Long processId, Boolean success) {
        log.debug("Fetching process record infos by process ID {}", processId);
        return processedRecordInfoRepository
                .findAllByProcessIdAndSuccessOrderByRecordIdAsc(processId, success)
                .stream()
                .map(processedRecordInfoMapper::entityToResponse)
                .toList();
    }

    public List<ProcessedRecordInfoResponse> getFirst(Long processId, Boolean success) {
        log.debug("Fetching process record infos by process ID {}", processId);
        return processedRecordInfoRepository
                .findFirstByProcessIdAndSuccess(processId, success)
                .stream()
                .map(processedRecordInfoMapper::entityToResponse)
                .toList();
    }

}
