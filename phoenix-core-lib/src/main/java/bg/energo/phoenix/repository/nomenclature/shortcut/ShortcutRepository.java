package bg.energo.phoenix.repository.nomenclature.shortcut;

import bg.energo.phoenix.model.entity.nomenclature.shortcut.Shortcut;
import bg.energo.phoenix.model.enums.nomenclature.shortcut.UserShortcuts;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortcutRepository extends JpaRepository<Shortcut, Long> {

    @Query("""
            select s from Shortcut s
            where s.username = :username
            order by s.orderingId asc
            """)
    List<Shortcut> findAllByUsername(String username);
    Optional<Shortcut> findByUsernameAndShortcut(String username,UserShortcuts userShortcut);

    boolean existsByUsernameAndShortcut(String username, UserShortcuts shortcut);

    void deleteByUsernameAndShortcut(String username, UserShortcuts shortcut);

    @Query("select max(b.orderingId) from Shortcut b where b.username=:username")
    Long findLastOrderingId(String username);


    @Query(
            "select s from Shortcut as s" +
                    " where s.id <> :currentId " +
                    " and (s.orderingId >= :start and s.orderingId <= :end) "
    )
    List<Shortcut> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );
}
