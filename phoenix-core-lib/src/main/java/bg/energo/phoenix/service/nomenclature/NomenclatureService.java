package bg.energo.phoenix.service.nomenclature;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureEnumResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NomenclatureService {

    private final List<NomenclatureBaseService> nomenclatureBaseServiceList;

    public Page<NomenclatureResponse> filterNomenclature(String nomenclature, NomenclatureItemsBaseFilterRequest request) {
        return findNomenclatureBaseService(nomenclature).filterNomenclature(request);
    }

    public void delete(String nomenclature, Long id) {
        findNomenclatureBaseService(nomenclature).delete(id);
    }

    public void sortAlphabetically(String nomenclature) {
        findNomenclatureBaseService(nomenclature).sortAlphabetically();
    }

    public void changeOrder(String nomenclature, NomenclatureItemsSortOrderRequest request) {
        findNomenclatureBaseService(nomenclature).changeOrder(request);
    }

    public boolean existsByIdAndStatusIn(String nomenclature, Long id, List<NomenclatureItemStatus> statuses) {
        return findNomenclatureBaseService(nomenclature).existsByIdAndStatusIn(id, statuses);
    }

    public List<ActivityNomenclatureResponse> findByIdIn(String nomenclature, List<Long> ids) {
        return findNomenclatureBaseService(nomenclature).findByIdIn(ids);
    }

    /**
     * Finds and returns the {@link NomenclatureBaseService} for the given nomenclature name.
     *
     * @param nomenclatureName the name of the nomenclature to find the service for
     * @return the {@link NomenclatureBaseService} for the given nomenclature name
     * @throws ClientException if the requested nomenclature type or service does not exist
     */
    private NomenclatureBaseService findNomenclatureBaseService(String nomenclatureName) {
        Optional<Nomenclature> nomenclatureOptional = Optional.ofNullable(Nomenclature.fromValue(nomenclatureName));
        if (nomenclatureOptional.isEmpty()) {
            log.error("Requested nomenclature type does not exist");
            throw new ClientException(ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }

        Nomenclature nomenclature = nomenclatureOptional.get();

        Optional<NomenclatureBaseService> nomenclatureService = nomenclatureBaseServiceList
                .stream()
                .filter(nomenclatureBaseService -> nomenclatureBaseService.getNomenclatureType().equals(nomenclature))
                .findFirst();
        if (nomenclatureService.isEmpty()) {
            log.error("Service does not exist for nomenclature type : %s".formatted(nomenclature));
            throw new ClientException(ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }

        return nomenclatureService.get();
    }

    public List<NomenclatureEnumResponse> getNomenclatureList() {
        return Arrays.stream(Nomenclature.values())
                .map(x->new NomenclatureEnumResponse(x,x.getValue())).toList();
    }

}
