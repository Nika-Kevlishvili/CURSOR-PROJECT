package bg.energo.phoenix.service.copy.domain;

import bg.energo.phoenix.model.enums.copy.domain.CopyDomain;
import bg.energo.phoenix.model.request.copy.domain.CopyDomainBaseRequest;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import org.springframework.data.domain.Page;

public interface CopyDomainBaseService {

    CopyDomain getDomain();

    Page<CopyDomainListResponse> filterCopyDomain(CopyDomainBaseRequest request);


}
