package bg.energo.phoenix.model.enums.shared;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

@Getter
public enum InvoicedOptions {
    YES(Set.of(true)),
    NO(Set.of(false)),
    ALL(Set.of(true, false));

    private final Set<Boolean> options;

    InvoicedOptions(Set<Boolean> options) {
        this.options = options;
    }

    public static String fromOptions(Set<Boolean> options) {
        if (CollectionUtils.isEmpty(options)) return null;
        for (InvoicedOptions invoicedOption : InvoicedOptions.values()) {
            if (invoicedOption.getOptions().equals(options)) {
                return invoicedOption.name();
            }
        }
        throw new IllegalArgumentException("No matching options for the given options: " + options);
    }
}
