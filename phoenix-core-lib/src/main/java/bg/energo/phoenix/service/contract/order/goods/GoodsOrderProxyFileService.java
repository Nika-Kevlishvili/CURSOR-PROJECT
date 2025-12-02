package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxyFiles;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderProxyFilesRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsOrderProxyFileService {

    private final GoodsOrderProxyFilesRepository goodsOrderProxyFilesRepository;
    private final FileService fileService;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    public ProxyFileResponse uploadProxyFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String fileType = file.getContentType();
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "proxy_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        GoodsOrderProxyFiles proxyFile = GoodsOrderProxyFiles.builder()
                .fileUrl(url)
                .name(fileName)
                .status(EntityStatus.ACTIVE)
                .build();
        GoodsOrderProxyFiles savedProxyFile = goodsOrderProxyFilesRepository.save(proxyFile);
        return new ProxyFileResponse(savedProxyFile);
    }

    public FileContent downloadProxyFile(Long id) {
        var proxyFile = goodsOrderProxyFilesRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Proxy file with ID %s not found;".formatted(id)));
        var content = fileService.downloadFile(proxyFile.getFileUrl());
        return new FileContent(proxyFile.getName(), content.getByteArray());
    }
}
