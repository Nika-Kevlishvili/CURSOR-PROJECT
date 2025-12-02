package bg.energo.phoenix.config;

import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import org.springframework.core.convert.converter.Converter;

public class NomenclatureConverter implements Converter<String, Nomenclature> {

    @Override
    public Nomenclature convert(String source) {
        return Nomenclature.fromValue(source);
    }

}