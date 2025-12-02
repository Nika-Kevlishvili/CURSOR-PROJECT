package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.SystemMessage;
import bg.energo.phoenix.model.response.nomenclature.systemMessage.SystemMessageResponse;
import bg.energo.phoenix.model.response.nomenclature.systemMessage.SystemMessageResponseListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemMessageRepository extends JpaRepository<SystemMessage, Long> {
    /**
     * <h1>filter system message</h1>
     * selects system message id , name and key from the system_messages table and orders by system message create date
     * @param prompt search key
     * @param pageable pagination object
     * @return list of system message object of id,name and key
     */
    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.systemMessage.SystemMessageResponseListItem(sm.id,sm.name,sm.key) " +
                    "from SystemMessage as sm" +
                    " where (:prompt is null or lower(sm.name) like :prompt)" +
                    " order by sm.createDate asc"
    )
    Page<SystemMessageResponseListItem> filter(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    /**
     * <h1>System message findByKeys</h1>
     * selects full system message object from the system_messages table according to the system message kays
     * @param key system message key array
     * @return full object of the system message record
     */
    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.systemMessage.SystemMessageResponse(sm) " +
                    "from SystemMessage as sm" +
                    " where sm.key in (:keys)" +
                    " order by sm.createDate asc"
    )
    Optional<List<SystemMessageResponse>> findByKeys(@Param("keys") List<String> key);

    boolean existsByKey(String key);


    @Query(
            value = """
                    select sm from SystemMessage as sm
                        where (:prompt is null or lower(sm.name) like :prompt)
                        order by sm.createDate desc
                    """
    )
    Page<SystemMessage> list(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(
            value = """
                    select sm from SystemMessage as sm
                        where (:prompt is null or cast(sm.id as string) = :prompt)
                        order by sm.createDate desc
                    """
    )
    Page<SystemMessage> listById(
            @Param("prompt") String prompt,
            Pageable pageable
    );
}
