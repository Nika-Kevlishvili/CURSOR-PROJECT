package bg.energo.phoenix.process.service;

import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import bg.energo.phoenix.process.model.response.ProcessedRecordInfoResponse;
import org.springframework.stereotype.Service;

@Service
public class ProcessedRecordInfoMapper {

    public ProcessedRecordInfoResponse entityToResponse(ProcessedRecordInfo processedRecordInfo) {
        return ProcessedRecordInfoResponse.builder()
                .id(processedRecordInfo.getId())
                .recordId(processedRecordInfo.getRecordId())
                .recordIdentifier(processedRecordInfo.getRecordIdentifier())
                .recordIdentifierVersion(processedRecordInfo.getRecordIdentifierVersion())
                .success(processedRecordInfo.isSuccess())
                .errorMessage(processedRecordInfo.getErrorMessage())
                .build();
    }

}
