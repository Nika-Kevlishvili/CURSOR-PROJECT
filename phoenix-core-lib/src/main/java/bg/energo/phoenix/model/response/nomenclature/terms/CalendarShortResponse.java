package bg.energo.phoenix.model.response.nomenclature.terms;

import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;

public record CalendarShortResponse(Long id, String name) {
    public CalendarShortResponse(Calendar calendar) {
        this(calendar.getId(), calendar.getName());
    }
}
