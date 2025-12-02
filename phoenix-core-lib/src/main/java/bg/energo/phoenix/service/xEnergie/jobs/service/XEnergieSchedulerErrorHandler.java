package bg.energo.phoenix.service.xEnergie.jobs.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.ProcessedRecordInfo;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.service.excel.MassImportErrorReportExcelService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Lazy(value = false)
@RequiredArgsConstructor
@Profile({"dev", "test"})
@Service("XEnergieSchedulerErrorHandler")
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class XEnergieSchedulerErrorHandler {
    private final ProcessRepository processRepository;
    private final ProcessedRecordInfoRepository processedRecordInfoRepository;
    private final Object lock = new Object();
    private final MassImportErrorReportExcelService multiSheetExcelBaseService;

    @Value("${xEnergie.report-file-location}")
    private String reportDirPath;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleException(Process process,
                                String exceptionMessage) {
        synchronized (lock) {
            try {
                Optional<ProcessedRecordInfo> lastProcessRecord = processedRecordInfoRepository
                        .findFirstByProcessIdOrderByRecordIdDesc(process.getId());

                long nextProcessRecordId = lastProcessRecord.map(ProcessedRecordInfo::getRecordId).orElse(-1L) + 1;

                ProcessedRecordInfo processedRecordInfo = ProcessedRecordInfo
                        .builder()
                        .processId(process.getId())
                        .success(false)
                        .recordId(nextProcessRecordId)
                        .errorMessage(exceptionMessage)
                        .build();

                processedRecordInfoRepository.save(processedRecordInfo);
            } catch (Exception e) {
                log.error("Exception handled while trying to save process record info", e);
            }
        }
    }

    @Transactional
    public void finishProcess(Process process) {
        try {
            log.debug("Finishing process: {}", process);
            Process persistedProcess = processRepository
                    .findById(process.getId())
                    .orElseThrow(() -> new ClientException("Exception while finishing process", ErrorCode.APPLICATION_ERROR));

            persistedProcess.setProcessCompleteDate(LocalDateTime.now());
            persistedProcess.setStatus(ProcessStatus.COMPLETED);

            try {
                log.debug("Starting generation of excel report");

                log.debug("Creating temp file");
                Path tempFile = Files.createTempFile("", ".xlsx");

                log.debug("Generating excel report");
                multiSheetExcelBaseService.generateExcel(new FileOutputStream(tempFile.toFile()), String.valueOf(persistedProcess.getId()));
                log.debug("Excel report generated");

                log.debug("Creating report directory");
                Files.createDirectories(Path.of(reportDirPath));

                log.debug("Copying temp file to final file");
                File file = new File("%s%s%s".formatted(reportDirPath, "%s (%s)".formatted(persistedProcess.getName(), persistedProcess.getId()), ".xlsx"));

                Files.copy(tempFile, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.debug("Temp file copied to final file, {}", file.getAbsolutePath());
            } catch (Exception e) {
                log.error("Exception handled while trying to generate excel report", e);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to complete process", e);
        }
    }
}
