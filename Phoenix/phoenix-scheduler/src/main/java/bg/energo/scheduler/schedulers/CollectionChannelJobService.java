package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.PaymentFTPFiles;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.TypeOfFile;
import bg.energo.phoenix.process.massImport.PaymentMassImportService;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.PaymentFTPFilesRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.collectionChannel.CollectionChannelHelperService;
import bg.energo.phoenix.util.RRuleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@RequiredArgsConstructor
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
// Better implementation would be with Quartz Scheduler !

public class CollectionChannelJobService {
    private final CollectionChannelRepository collectionChannelRepository;
    private final CollectionChannelHelperService collectionChannelHelperService;
    private final FileService fileService;
    private final PaymentMassImportService paymentMassImportService;
    private final PaymentFTPFilesRepository paymentFTPFilesRepository;

    @Scheduled(cron = "${collection.channel.job.hourly.cron}")
    @ExecutionTimeLogger(value = "CollectionChannelJobService")
    public void executeHourly() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("starting hourly job ");
        List<CollectionChannel> collectionChannels = collectionChannelRepository.findOfflineCollectionChannelsWhichAreNotMarked(now).stream()
                .filter(cc-> {
                    if(cc.getDataReceivingSchedule()==null) {
                        log.debug("RRULE in database is null");
                        return false;
                    }
                    boolean rruleValid = validateRRULE(cc.getDataReceivingSchedule());
                    if(!rruleValid) {
                        log.debug("RRULE is not valid for collection channel: {}", cc.getId());
                    }
                    boolean folderNotNull = cc.getFolderForFileReceiving()!=null && !cc.getFolderForFileReceiving().isBlank();
                    if(!folderNotNull) {
                        log.debug("Folder for receiving collection channel is null! for collection channel : {}",cc.getId());
                    }
                    if (TypeOfFile.RPS_PAYMENT.equals(cc.getTypeOfFile())) {
                        log.debug("Skipping RPS payment collection channel from automatic import: {}", cc.getId());
                        return false;
                    }
                    return rruleValid && folderNotNull;
                })
                .toList();
        log.debug("Filtered collection channels {}",collectionChannels);
        List<CollectionChannel> markedCollectionChannels= new ArrayList<>();
        Map<CollectionChannel,List<MultipartFile>> collectionChannelsToExecute = new HashMap<>();
        for(CollectionChannel collectionChannel: collectionChannels) {
            List<MultipartFile> files = retrieveFilesFromFtpFolder(collectionChannel.getFolderForFileReceiving());
            log.debug("files retrieved from folder {}",files);
            if(files ==null || files.isEmpty()) {
                log.debug("files not present");
                markedCollectionChannels.add(collectionChannel);
            } else {
                collectionChannelsToExecute.put(collectionChannel,files);
            }
        }
        markForWaitingPeriod(markedCollectionChannels,now);
        collectionChannelHelperService.saveAll(markedCollectionChannels);

        for(CollectionChannel collectionChannel: collectionChannelsToExecute.keySet()) {
            log.debug("processing collection channel with id {}",collectionChannel.getId());
            processFiles(collectionChannel, collectionChannelsToExecute.get(collectionChannel));
        }
    }

    private boolean checkIfAlreadyProcessed(String fileName) {
        return paymentFTPFilesRepository.existsByName(fileName);
    }

    public void processFiles(CollectionChannel collectionChannel,List<MultipartFile> files) {
        if (TypeOfFile.RPS_PAYMENT.equals(collectionChannel.getTypeOfFile())) {
            log.debug("Skipping automatic processing of RPS payment files for collection channel {}", collectionChannel.getId());
            return;
        }
        files.forEach(file-> {
            try {
                if(!checkIfAlreadyProcessed(file.getName())) {
                    try {
                        log.debug("calling mass import process");
                        paymentMassImportService.uploadMassImportFile(file, LocalDate.now().minusDays(1), collectionChannel.getId(),true);
                    }catch (Exception e) {
                        log.debug("Exception caught during file validation on file {}", file.getName());
                        log.debug("Error message {}", e.getMessage());
                        return;
                    }
                    PaymentFTPFiles paymentFTPFile = createPaymentFTPFile(file.getName());
                    collectionChannelHelperService.save(paymentFTPFile);
                    log.debug("Saved file with name {}",file.getName());
                } else {
                    log.debug("File with such name is already processed! skipping");
                }
            }catch (Exception e) {
                log.debug("Exception  during mass import file upload,skipping");
                log.debug("Exception message {}",e.getMessage());
            }
        });
    }

    public PaymentFTPFiles createPaymentFTPFile(String name) {
        PaymentFTPFiles paymentFTPFiles = new PaymentFTPFiles();
        paymentFTPFiles.setName(name);
        return paymentFTPFiles;
    }

    public void markForWaitingPeriod(List<CollectionChannel> collectionChannels,LocalDateTime now) {
        for(CollectionChannel collectionChannel: collectionChannels) {
            Integer waitingPeriodToleranceHours = collectionChannel.getWaitingPeriodToleranceInHours();
            if(waitingPeriodToleranceHours != null){
                collectionChannel.setWaitingPeriodTime(now.plusHours(waitingPeriodToleranceHours));
            } else return;
        }
    }


    @Scheduled(cron = "${collection.channel.job.minute.cron}")
    @ExecutionTimeLogger(value = "CollectionChannelJobService")
    public void executeForWaitingPeriods() {
        log.debug("starting  job for marked collection channels");

        LocalDateTime now = LocalDateTime.now();
        List<CollectionChannel> markedOfflineCollectionChannels = collectionChannelRepository.findMarkedOfflineCollectionChannels(now)
                .stream()
                .filter(cc-> {
                    boolean folderNotNull = cc.getFolderForFileReceiving() != null && !cc.getFolderForFileReceiving().isBlank();
                    if(folderNotNull) {
                        log.debug("Folder for receiving collection channel is null! for collection channel : {}", cc.getId());
                    }
                    return folderNotNull && !TypeOfFile.RPS_PAYMENT.equals(cc.getTypeOfFile());
                }).toList();

        log.debug("List of marked collection channels {}",markedOfflineCollectionChannels);

        Map<CollectionChannel, List<MultipartFile>> collectionChannelsToExecute = new HashMap<>();

        for(CollectionChannel collectionChannel : markedOfflineCollectionChannels) {
            List<MultipartFile> files = retrieveFilesFromFtpFolder(collectionChannel.getFolderForFileReceiving());
            if(files==null || files.isEmpty()) {
                log.debug("No files found for collection channel: {}", collectionChannel.getId());
            } else {
                collectionChannel.setWaitingPeriodTime(null);
                collectionChannelsToExecute.put(collectionChannel,files);
            }
        }
        collectionChannelHelperService.saveAll(collectionChannelsToExecute.keySet());
        for (CollectionChannel collectionChannel : collectionChannelsToExecute.keySet()) {
            log.debug("processing collection channel with id {}",collectionChannel);
            processFiles(collectionChannel, collectionChannelsToExecute.get(collectionChannel));
        }
    }


    private boolean validateRRULE(String RRULE) {
        RecurrenceRule recurrenceRule = RRuleUtil.validRecurrenceRule(RRULE);
        if (recurrenceRule == null) {
            return false;
        }
        return RRuleUtil.periodMatchesRRule(recurrenceRule, LocalDate.now());
    }


    private List<MultipartFile> retrieveFilesFromFtpFolder(String ftpFolderPath) {
        return fileService.fetchFilesFromFolder(ftpFolderPath)
                .stream().filter(file-> {
                    boolean isText = file.getContentType()!=null && file.getContentType().equals("text/plain");
                    if(isText) {
                        log.debug("File doesn't have text extension , skipping");
                    }
                    return isText;
                }).toList();
    }

}
