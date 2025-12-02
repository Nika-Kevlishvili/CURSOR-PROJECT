package bg.energo.phoenix.util.epb;

import org.springframework.data.domain.Sort;

import java.util.Objects;
import java.util.function.Function;

public abstract class EPBListingUtils {

    /**
     * Extracts the name of the target search field enum value or falls back to a default value if the target is {@code null}.
     * This method returns the name of the provided {@code targetSearchField} enum value.
     * If {@code targetSearchField} is {@code null}, it returns the name of the {@code defaultSearchField} enum value instead.
     * Both the target and default search fields must be non-null enums of the same type.
     *
     * @param <T> the type of the enum, which must extend {@link Enum}
     * @param targetSearchField the enum value to be used for extraction. May be {@code null}.
     * @param defaultSearchField the fallback enum value used if {@code targetSearchField} is {@code null}. Must not be {@code null}.
     *
     * @return the name of the {@code targetSearchField} if it is non-null, otherwise the name of the {@code defaultSearchField}.
     *
     * @throws NullPointerException if {@code defaultSearchField} is {@code null}.
     */
    public static <T extends Enum<T>> String extractSearchBy(
            T targetSearchField,
            T defaultSearchField
    ) {
        return Objects.requireNonNullElse(targetSearchField, defaultSearchField).name();
    }

    /**
     * Creates a {@link Sort} object based on the specified sort direction and enum values.
     * This method constructs a {@code Sort} object for sorting data. It uses the provided {@code sortDirection} for the sort direction.
     * If {@code sortDirection} is {@code null}, the method defaults to {@code Sort.Direction.ASC}.
     * It also uses the name of the {@code targetEnum} for the sort property; if {@code targetEnum} is {@code null}, the method defaults to {@code defaultEnum}.
     * The {@code columnNameExtractor} function is used to extract the column name from the enum.
     *
     * @param <E> the type of the enum, which must extend {@link Enum}
     * @param sortDirection the direction of the sort. If {@code null}, defaults to {@code Sort.Direction.ASC}.
     * @param targetEnum the primary enum value used for the sort property. If {@code null}, defaults to {@code defaultEnum}.
     * @param defaultEnum the fallback enum value used if {@code targetEnum} is {@code null}. Must not be {@code null}.
     * @param columnNameExtractor a function that extracts the column name from the enum value. Must not be {@code null}.
     *
     * @return a {@link Sort} object configured with the specified direction and the column name extracted from the enum.
     *
     * @throws NullPointerException if {@code defaultEnum} or {@code columnNameExtractor} is {@code null}.
     */

    public static <E extends Enum<E>> Sort extractSortBy(
            Sort.Direction sortDirection,
            E targetEnum,
            E defaultEnum,
            Function<E, String> columnNameExtractor
    ) {
        return Sort.by(
                Objects.requireNonNullElse(sortDirection, Sort.Direction.ASC),
                columnNameExtractor.apply(Objects.requireNonNullElse(targetEnum, defaultEnum))
        );
    }

}
