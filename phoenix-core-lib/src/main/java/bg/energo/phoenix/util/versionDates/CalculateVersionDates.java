package bg.energo.phoenix.util.versionDates;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CalculateVersionDates {
    public static List<VersionWithDatesModel> calculateVersionEndDates(List<VersionWithDatesModel> versionWithDatesModelList,
                                                                       LocalDate newStartDate,
                                                                       Integer versionId) {
        versionWithDatesModelList.removeIf(model -> Objects.equals(model.getVersionId(), versionId));

        return updateDates(newStartDate, versionId, versionWithDatesModelList);
    }

    private static List<VersionWithDatesModel> updateDates(LocalDate newStartDate,
                                                           Integer versionId,
                                                           List<VersionWithDatesModel> versionWithDatesModelList) {
        VersionWithDatesModel newModel = VersionWithDatesModel.builder()
                .versionId(versionId)
                .startDate(newStartDate)
                .build();

        versionWithDatesModelList.add(newModel);

        versionWithDatesModelList.sort(Comparator.comparing(VersionWithDatesModel::getStartDate));

        for (int i = 0; i < versionWithDatesModelList.size(); i++) {
            VersionWithDatesModel currentVersion = versionWithDatesModelList.get(i);
            if (i + 1 < versionWithDatesModelList.size()) {
                VersionWithDatesModel nextVersion = versionWithDatesModelList.get(i + 1);
                currentVersion.setEndDate(nextVersion.getStartDate().minusDays(1));
            } else {
                currentVersion.setEndDate(null);
            }
        }

        return versionWithDatesModelList;
    }
}
