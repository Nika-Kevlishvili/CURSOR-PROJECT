package bg.energo.phoenix.service.archivation.edms;

import java.util.List;

public interface FileExpiration<T> {
    List<T> findExpiredFiles();

    List<T> findFailedArchivationFiles();

    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    T save(T entity);
}
