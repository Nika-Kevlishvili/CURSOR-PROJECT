package bg.energo.phoenix.repository.template;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractTemplateFileRepository extends JpaRepository<ContractTemplateFiles, Long> {

    Optional<ContractTemplateFiles> findByIdAndStatus(Long id, EntityStatus status);

    @Query("""
                select tf
                from ContractTemplateFiles tf
                where tf.id = :id
                and tf.status = :status
                and not exists(select td
                from ContractTemplateDetail td
                where td.templateFileId = :id)
            """)
    Optional<ContractTemplateFiles> findByIdAndStatusAndNotAttachedToTemplate(@Param("id") Long id,
                                                                              @Param("status") EntityStatus status);

    @Query("""
                select tf
                from ContractTemplateFiles tf
                where tf.id = :fileId
                and tf.status = :status
                and not exists(select td
                from ContractTemplateDetail td
                where td.templateFileId = :fileId
                and td.templateId <> :templateId)
            """)
    Optional<ContractTemplateFiles> findByIdAndStatusAndIsNotAttachedToOtherTemplate(@Param("fileId") Long fileId,
                                                               @Param("status") EntityStatus entityStatus,
                                                               @Param("templateId") Long templateId);
}
