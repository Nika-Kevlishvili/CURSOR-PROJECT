package bg.energo.phoenix.model.enums.template;

import lombok.Getter;
@Getter
public enum QesSortBy {
    ID("id"),
    UPDATE_TIME("updateTime");
    private final String sortBy;

    QesSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
}
