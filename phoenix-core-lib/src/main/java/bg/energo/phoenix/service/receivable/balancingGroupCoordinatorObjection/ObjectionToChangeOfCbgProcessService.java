package bg.energo.phoenix.service.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgFile;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgPods;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgProcessResult;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ProcessMiddleResult;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgFileRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgPodsRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgProcessResultRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectionToChangeOfCbgProcessService {

    private final ObjectionToChangeOfCbgFileRepository objectionToChangeOfCbgFileRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final ObjectionToChangeOfCbgPodsRepository objectionToChangeOfCbgPodsRepository;
    private final ObjectionToChangeOfCbgProcessResultRepository objectionToChangeOfCbgProcessResultRepository;
    private final FileService fileService;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    public ProxyFileResponse uploadProxyFile(MultipartFile file) {
        if (!hasExcelFormat(file)) {
            throw new ClientException("You can upload only .xlsx format file;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "proxy_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ObjectionToChangeOfCbgFile proxyFile = ObjectionToChangeOfCbgFile.builder()
                .fileUrl(url)
                .name(fileName)
                .status(EntityStatus.ACTIVE)
                .build();
        var savedProxyFile = objectionToChangeOfCbgFileRepository.save(proxyFile);
        return new ProxyFileResponse(savedProxyFile);
    }

    public FileContent downloadProxyFile(Long id) {
        var proxyFile = objectionToChangeOfCbgFileRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(proxyFile.getFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }


    public Set<String> readFile(byte[] content) throws IOException {
        Set<String> cellValues = new HashSet<>();

        try (ByteArrayInputStream baIs = new ByteArrayInputStream(content);
             XSSFWorkbook workbook = new XSSFWorkbook(baIs)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
                XSSFRow row = sheet.getRow(rowNum);
                if (row != null) {
                    XSSFCell cell = row.getCell(0);
                    if (cell != null) {
                        String cellValue = switch (cell.getCellType()) {
                            case STRING -> cell.getStringCellValue();
                            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                            default -> "";
                        };

                        if (!cellValue.isEmpty()) {
                            cellValues.add(cellValue);
                        }
                    }
                }
            }
        }
        return cellValues;
    }

    public void savePodsForObjectionCreate(Set<String> podIdentifiers, Long changeOfCbgId) {
        List<ObjectionToChangeOfCbgPods> podsToSave = new ArrayList<>();

        for (String podIdentifier : podIdentifiers) {
            Optional<PointOfDelivery> pointOfDelivery = pointOfDeliveryRepository.findByIdentifierAndStatus(podIdentifier, PodStatus.ACTIVE);
            pointOfDelivery.ifPresent(ofDelivery ->
                    podsToSave.add(new ObjectionToChangeOfCbgPods(changeOfCbgId, ofDelivery.getId()))
            );
        }
        if (!podsToSave.isEmpty()) {
            objectionToChangeOfCbgPodsRepository.saveAll(podsToSave);
        }
    }


    public void savePodsForObjectionEdit(Set<String> podIdentifiers, Set<Long> podIds, Long changeOfCbgId) {
        List<Long> notWrittenPodIds = objectionToChangeOfCbgPodsRepository.findPodIds(podIdentifiers, podIds);
        List<ObjectionToChangeOfCbgPods> podsToSave = new ArrayList<>();

        for (Long id : notWrittenPodIds) {
            podsToSave.add(new ObjectionToChangeOfCbgPods(changeOfCbgId, id));
        }
        objectionToChangeOfCbgPodsRepository.saveAll(podsToSave);
    }

    @Transactional
    public void startProcess(Long changeOfCbgId) {
        List<ProcessMiddleResult> processMiddleResults = objectionToChangeOfCbgProcessResultRepository.calculate(changeOfCbgId);
        List<ObjectionToChangeOfCbgProcessResult> processResults = processMiddleResults.stream().map(ObjectionToChangeOfCbgProcessResult::new).toList();
        objectionToChangeOfCbgProcessResultRepository.saveAll(processResults);
    }

    private boolean hasExcelFormat(MultipartFile file) {
        return Objects.equals(file.getContentType(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

}
