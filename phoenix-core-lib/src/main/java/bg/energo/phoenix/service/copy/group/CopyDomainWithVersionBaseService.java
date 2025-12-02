package bg.energo.phoenix.service.copy.group;

import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CopyDomainWithVersionBaseService {

    CopyDomainWithVersion getGroupType();

    Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request);

    List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId);

}
