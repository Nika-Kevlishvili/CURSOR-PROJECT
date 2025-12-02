package bg.energo.phoenix.service.copy.domain;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.enums.copy.domain.CopyDomain;
import bg.energo.phoenix.model.request.copy.domain.CopyDomainBaseRequest;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CopyDomainService {

    private final List<CopyDomainBaseService> copyDomainServiceList;

    public Page<CopyDomainListResponse> getCopyDomainList(CopyDomain copyDomain, CopyDomainBaseRequest request) {
        return findCopyDomainService(copyDomain).filterCopyDomain(request);
    }

    private CopyDomainBaseService findCopyDomainService(CopyDomain copyDomain) {
        Optional<CopyDomainBaseService> copyBaseServiceOptional =
                copyDomainServiceList.stream()
                        .filter(copyDomainBaseService -> copyDomainBaseService.getDomain().equals(copyDomain))
                        .findFirst();

        if (copyBaseServiceOptional.isEmpty()) {
            log.error("Service does not exist for copy object type : %s".formatted(copyDomain));
            throw new ClientException(ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }

        return copyBaseServiceOptional.get();

    }

}
