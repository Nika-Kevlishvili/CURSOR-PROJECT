package bg.energo.phoenix.service.nomenclature;

import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NomenclatureBaseService {

    Nomenclature getNomenclatureType();

    Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request);

    void changeOrder(NomenclatureItemsSortOrderRequest request);

    void sortAlphabetically();

    void delete(Long id);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
