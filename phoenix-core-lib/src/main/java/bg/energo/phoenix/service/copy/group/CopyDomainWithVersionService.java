package bg.energo.phoenix.service.copy.group;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CopyDomainWithVersionService {

    private final List<CopyDomainWithVersionBaseService> copyDomainWithVersionBaseServices;

    public Page<CopyDomainWithVersionBaseResponse> listCopies(CopyDomainWithVersion copyDomainWithVersion, CopyDomainWithVersionBaseRequest request) {
        return findCorrectService(copyDomainWithVersion).findGroups(request);
    }

    public List<CopyDomainWithVersionMiddleResponse> listVersions(CopyDomainWithVersion copyDomainWithVersion, Long id) {
        return findCorrectService(copyDomainWithVersion).findGroupVersions(id);
    }

    private CopyDomainWithVersionBaseService findCorrectService(CopyDomainWithVersion copyDomainWithVersion) {
        return copyDomainWithVersionBaseServices.
                stream().filter(x -> x.getGroupType().equals(copyDomainWithVersion))
                .findFirst()
                .orElseThrow(() -> new ClientException("Service do not exist with this type", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
    }

}
