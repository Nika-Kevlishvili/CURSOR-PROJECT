package bg.energo.phoenix.service.portal;

import bg.energo.common.portal.api.appTag.AppTag;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.PortalTag;
import bg.energo.phoenix.model.response.nomenclature.contract.PortalTagResponse;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalTagService {

    private final PortalTagRepository portalTagRepository;

    @Transactional
    public void saveTag(List<AppTag> portalTags,List<AppTag> bulgarianTags) {
        List<PortalTag> tagsToSave = new ArrayList<>();
        List<PortalTag> allByStatus = portalTagRepository.findAllByStatus(EntityStatus.ACTIVE);
        Map<String, AppTag> bulgarianTagMap = bulgarianTags.stream().collect(Collectors.toMap(x -> x.getId().toString(), j -> j));
        Map<String, PortalTag> collect = allByStatus.stream().collect(Collectors.toMap(PortalTag::getPortalId, j -> j));
        for (AppTag portalTag : portalTags) {
            String portalTagId = portalTag.getId().toString();
            PortalTag mainTag = collect.remove(portalTagId);
            AppTag bulgarianTag = bulgarianTagMap.get(portalTagId);
            if (mainTag == null) {
                tagsToSave.add(new PortalTag(portalTag,bulgarianTag));
            } else {
                mainTag.setName(portalTag.getName());
                mainTag.setDescription(portalTag.getDescription());
                mainTag.setNameBg(bulgarianTag.getName());
                mainTag.setDescriptionBg(bulgarianTag.getDescription());
                tagsToSave.add(mainTag);
            }
        }
        portalTagRepository.saveAll(tagsToSave);
        Collection<PortalTag> values = collect.values();
        values.forEach(portalTag -> portalTag.setStatus(EntityStatus.DELETED));
        portalTagRepository.saveAll(values);
    }



    public Page<PortalTagResponse> getTags(boolean withGroupPrefix, String prompt, Integer page, Integer size) {
        return portalTagRepository.findAllByPrefix(withGroupPrefix, prompt, EntityStatus.ACTIVE,PageRequest.of(page,size, Sort.by(Sort.Direction.ASC,"name")));
    }

    public Page<PortalTagManagerResponse> getTagsAndManagers(boolean withGroupPrefix, String prompt, Integer page, Integer size) {
        return portalTagRepository.findAllByPrefixAndManagers(withGroupPrefix, prompt, PageRequest.of(page,size, Sort.by(Sort.Direction.ASC,"performerType","name")));
    }
}
