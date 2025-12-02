package bg.energo.phoenix.repository.translation;

import bg.energo.phoenix.model.entity.translation.Translations;
import bg.energo.phoenix.model.enums.translation.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationRepository extends JpaRepository<Translations, Long> {


    @Query(nativeQuery = true, value = """
                    WITH RECURSIVE
                        translations_ordered AS (
                            SELECT value,
                                   translated_value,
                                   regexp_replace(value, '([\\\\^$*+?().|\\\\[\\\\]{}])', '\\\\\\\\$1', 'g') AS escaped_value,
                                   row_number() OVER (ORDER BY length(value) DESC) AS id
                            FROM translation.translations
                            WHERE text(dest_language) = :destLanguage
                        ),
                        translation_process AS (
                            SELECT :input AS current_text,
                                   1 AS iteration
                    
                            UNION ALL
                            SELECT regexp_replace(
                                           tp.current_text,
                                           '(?i)' || t.escaped_value,
                                           t.translated_value,
                                           'g'
                                   ) AS current_text,
                                   tp.iteration + 1 AS iteration
                            FROM translation_process tp
                                     JOIN translations_ordered t ON t.id = tp.iteration
                            WHERE tp.iteration <= (SELECT count(*) FROM translations_ordered)
                        )
                    SELECT current_text AS translated_text
                    FROM translation_process
                    WHERE iteration = (SELECT count(*) + 1 FROM translations_ordered)
                    LIMIT 1
            """)
    String translate(@Param("input") String input, @Param("destLanguage") String language);
}
