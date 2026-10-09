package bg.energo.phoenix.service.salesportal.customer;

import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.service.translation.TranslationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BilingualTextResolverTest {

    private static final String BASE = "Иван";
    private static final String TRANSL = "Ivan";

    @Mock
    private TranslationService translationService;

    @InjectMocks
    private BilingualTextResolver resolver;

    @Test
    void resolve_bulgarian_returnsBaseColumn_withoutTranslation() {
        assertThat(resolver.resolve(Language.BULGARIAN, BASE, TRANSL)).isEqualTo(BASE);
        verifyNoInteractions(translationService);
    }

    @Test
    void resolve_nullLanguage_returnsBaseColumn() {
        assertThat(resolver.resolve(null, BASE, TRANSL)).isEqualTo(BASE);
        verifyNoInteractions(translationService);
    }

    @Test
    void resolve_english_withTransl_returnsTransl_withoutTransliterating() {
        assertThat(resolver.resolve(Language.ENGLISH, BASE, TRANSL)).isEqualTo(TRANSL);
        verifyNoInteractions(translationService);
    }

    @Test
    void resolve_english_blankTransl_fallsBackToTransliteration() {
        when(translationService.translateByCharacters(BASE, Language.ENGLISH)).thenReturn(TRANSL);
        assertThat(resolver.resolve(Language.ENGLISH, BASE, "   ")).isEqualTo(TRANSL);
    }

    @Test
    void resolve_english_nullTransl_fallsBackToTransliteration() {
        when(translationService.translateByCharacters(BASE, Language.ENGLISH)).thenReturn(TRANSL);
        assertThat(resolver.resolve(Language.ENGLISH, BASE, null)).isEqualTo(TRANSL);
    }

    @Test
    void resolve_english_transliterationBlank_fallsBackToBase() {
        when(translationService.translateByCharacters(BASE, Language.ENGLISH)).thenReturn("");
        assertThat(resolver.resolve(Language.ENGLISH, BASE, null)).isEqualTo(BASE);
    }

    @Test
    void resolveTransliterated_bulgarian_returnsBase() {
        assertThat(resolver.resolveTransliterated(Language.BULGARIAN, BASE)).isEqualTo(BASE);
        verifyNoInteractions(translationService);
    }

    @Test
    void resolveTransliterated_english_transliterates() {
        when(translationService.translateByCharacters(BASE, Language.ENGLISH)).thenReturn(TRANSL);
        assertThat(resolver.resolveTransliterated(Language.ENGLISH, BASE)).isEqualTo(TRANSL);
    }

    @Test
    void resolveTransliterated_english_nullBase_returnsNull_withoutTranslation() {
        assertThat(resolver.resolveTransliterated(Language.ENGLISH, null)).isNull();
        verifyNoInteractions(translationService);
    }

    @Test
    void resolveTransliterated_english_blankBase_returnsBlank_withoutTranslation() {
        assertThat(resolver.resolveTransliterated(Language.ENGLISH, "   ")).isEqualTo("   ");
        verifyNoInteractions(translationService);
    }
}
