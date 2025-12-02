package bg.energo.phoenix.service.template;

import bg.energo.phoenix.model.enums.template.QesSortBy;
import bg.energo.phoenix.model.enums.template.QesStatus;
import bg.energo.phoenix.model.request.template.QesDocumentFilterRequest;
import bg.energo.phoenix.model.response.template.QesDocumentResponse;
import bg.energo.phoenix.model.response.template.QesDocumentResponseImpl;
import bg.energo.phoenix.repository.template.QesDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QesDocumentService {
    private final QesDocumentRepository qesDocumentRepository;


    public Page<QesDocumentResponse> filter(QesDocumentFilterRequest request) {
        return qesDocumentRepository
                .filter(request.getStatus() == null ? List.of(QesStatus.ACTIVE.toString(), QesStatus.FINISHED.toString(), QesStatus.IN_PROGRESS.toString()) : request.getStatus().stream().map(Enum::toString).toList(),
                        request.getSigningStatuses() == null ? new ArrayList<>() : request.getSigningStatuses().stream().map(Enum::toString).toList(),
                        request.getPurposes() == null ? new ArrayList<>() : request.getPurposes().stream().map(Enum::toString).toList(),
                        request.getProductIds()==null? new ArrayList<>(): request.getProductIds(),
                        request.getServiceIds()==null?new ArrayList<>():request.getServiceIds(),
                        request.getPodIds()==null? new ArrayList<>():request.getPodIds(),
                        request.getSegments()==null?new ArrayList<>():request.getSegments(),
                        request.getSaleChannels()==null?new ArrayList<>():request.getSaleChannels(),
                        CollectionUtils.isNotEmpty(request.getSaleChannels()),
                        request.getCreatedBy()==null? new ArrayList<>():request.getCreatedBy(),
                        request.getUpdatedFrom(),
                        request.getUpdatedTo(),
                        request.getSearchFilter() == null ? null : request.getSearchFilter().toString(),
                        request.getPrompt(),
                        LocalDateTime.now().minusDays(30),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(request.getDirection(), request.getSortBy() == null ? QesSortBy.ID.getSortBy() : request.getSortBy().getSortBy()))).map(QesDocumentResponseImpl::new);
    }
}
