package bg.energo.phoenix.util.epb;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class EPBListUtils {

    /**
     * Check if list of objects contains duplicate values.
     *
     * @param list {@link List} of objects
     * @param <T>  type of objects
     * @return true if all objects are unique and false otherwise.
     */
    public static <T> boolean notAllUnique(List<T> list) {
        return new HashSet<>(list).size() != list.size();
    }


    /**
     * Check if list of objects contains duplicate values.
     *
     * @param list            {@link List} of objects
     * @param errorMessageKey error message key
     * @return aggregated error message if list contains duplicate values and empty string otherwise
     */
    public static <T> String validateDuplicateValuesByIndexes(List<T> list, String errorMessageKey) {
        StringBuilder sb = new StringBuilder();

        Set<T> uniqueValues = new HashSet<>();
        Map<T, List<Integer>> duplicateValuesMap = new HashMap<>();

        for (int i = 0; i < list.size(); i++) {
            T id = list.get(i);
            if (!uniqueValues.add(id)) {
                duplicateValuesMap.computeIfAbsent(id, k -> new ArrayList<>()).add(i);
            }
        }

        for (Map.Entry<T, List<Integer>> entry : duplicateValuesMap.entrySet()) {
            T id = entry.getKey();
            List<Integer> indexes = entry.getValue();
            for (Integer i : indexes) {
                sb.append("%s[%s]-Duplicate value %s;".formatted(errorMessageKey, i, id));
            }
        }

        return sb.toString();
    }


    /**
     * Takes two lists, merges them and returns a list of unique values from both lists.
     *
     * @param firstList  first list
     * @param secondList second list
     * @param <T>        type of objects
     * @return a list of unique values from both lists
     */
    public static <T> List<T> getUniqueCombinedList(List<T> firstList, List<T> secondList) {
        Set<T> uniqueValues = new HashSet<>();
        uniqueValues.addAll(firstList);
        uniqueValues.addAll(secondList);
        return new ArrayList<>(uniqueValues);
    }

    /**
     * Converts a formatted string representation of enum names into a list of enum values.
     * This method takes a string formatted as an array of enum names (e.g., "{ENUM1,ENUM2,ENUM3}") and converts it into a list of corresponding enum values.
     * If the input string is {@code null} or empty, an empty list is returned. Any invalid or blank enum names are ignored.
     *
     * @param <T>           the type of the enum, which must extend {@link Enum}
     * @param enumClass     the {@link Class} object of the enum type. Must not be {@code null}.
     * @param stringAsArray the string representation of the enum names, formatted as an array (e.g., "{ENUM1,ENUM2,ENUM3}"). May be {@code null} or empty.
     * @return a list of enum values corresponding to the names in the input string. If the input string is {@code null} or empty, an empty list is returned.
     * @throws IllegalArgumentException if any enum name in the input string does not match a valid enum constant in the specified enum class.
     * @throws NullPointerException     if {@code enumClass} is {@code null}.
     */
    public static <T extends Enum<T>> List<T> convertDBEnumStringArrayIntoListEnum(Class<T> enumClass,
                                                                                   String stringAsArray
    ) {
        if (StringUtils.isEmpty(stringAsArray)) {
            return new ArrayList<>();
        }
        return Arrays
                .stream(stringAsArray.replaceAll("[{}]", "").split(","))
                .filter(StringUtils::isNotBlank)
                .map(s -> Enum.valueOf(enumClass, s))
                .collect(Collectors.toList());
    }

    /**
     * Converts a database string representation of an array into a list of strings.
     *
     * @param stringAsArray the string representation of an array from the database,
     *                      typically enclosed in curly braces and separated by commas.
     * @return a list of strings extracted from the input array string. If the input is empty or null,
     * an empty list is returned.
     */
    public static List<String> convertDBStringArrayIntoListString(String stringAsArray) {
        if (StringUtils.isEmpty(stringAsArray)) {
            return new ArrayList<>();
        }

        return Arrays
                .stream(stringAsArray.replaceAll("[{}]", "").split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of enum values into a formatted string suitable for database storage.
     * This method transforms a list of enum values into a single string where each enum name is separated by a comma, and the entire string is enclosed in curly braces.
     * If the input list is {@code null} or empty, the method returns {@code null}.
     * The resulting string format is useful for representing an array of enum names, for example: "{ENUM1,ENUM2,ENUM3}".
     *
     * @param <T>           the type of the enum, which must extend {@link Enum}
     * @param listToConvert the list of enum values to be converted. May be {@code null}.
     * @return a formatted string representing the enum names separated by commas and enclosed in curly braces, or {@code null} if the input list is {@code null} or empty.
     * @throws NullPointerException if the input list contains {@code null} elements and the {@code filter(Objects::nonNull)} is not used appropriately.
     */
    public static <T extends Enum<T>> String convertEnumListToDBEnumArray(List<T> listToConvert) {
        String convertedString;

        if (CollectionUtils.isEmpty(listToConvert)) {
            return null;
        }

        convertedString = listToConvert
                .stream()
                .filter(Objects::nonNull)
                .map(T::name)
                .collect(Collectors.joining(",", "{", "}"));

        return convertedString;
    }

    /**
     * Converts a list of enum values into a list of their names.
     * This method takes a list of enum values and returns a list of their names as strings.
     * If the input list is {@code null} or empty, an empty list is returned. Null elements in the input list are filtered out and do not appear in the resulting list.
     *
     * @param <T>           the type of the enum, which must extend {@link Enum}
     * @param listToConvert the list of enum values to be converted. May be {@code null}.
     * @return a list of strings representing the names of the enum values. If the input list is {@code null} or empty, an empty list is returned.
     * @throws NullPointerException if the input list is {@code null}, and {@code CollectionUtils} or {@code Objects} methods are used inappropriately.
     */
    public static <T extends Enum<T>> List<String> convertEnumListIntoStringListIfNotNull(List<T> listToConvert) {
        if (CollectionUtils.isEmpty(listToConvert)) {
            return new ArrayList<>();
        }

        return listToConvert
                .stream()
                .filter(Objects::nonNull)
                .map(Enum::name)
                .toList();
    }

    /**
     * Returns a list of elements that were present in the old list but are not in the new list.
     * This method compares two lists and returns a list of elements that exist in {@code oldList} but are missing in {@code newList}.
     * The resulting list contains only the elements that have been removed from the new list compared to the old list.
     *
     * @param <T>     the type of elements in both lists
     * @param oldList the original list to compare against. Must not be {@code null}.
     * @param newList the list of items to be checked for deletions. Must not be {@code null}.
     * @return a list containing elements that are in {@code oldList} but not in {@code newList}. If there are no such elements, an empty list is returned.
     * @throws NullPointerException if either {@code oldList} or {@code newList} is {@code null}.
     */
    public static <T> List<T> getDeletedElementsFromList(List<T> oldList, List<T> newList) {
        return oldList.stream().filter(t -> !newList.contains(t)).toList();
    }

    /**
     * Returns a list of elements that are present in the new list but not in the old list.
     * This method compares two lists and returns a list of elements that exist in {@code newList} but are not present in {@code oldList}.
     * The resulting list contains only the elements that have been added to the new list.
     *
     * @param <T>     the type of elements in both lists
     * @param oldList the original list to compare against. Must not be {@code null}.
     * @param newList the list of items to be checked for additions. Must not be {@code null}.
     * @return a list containing elements that are in {@code newList} but not in {@code oldList}. If there are no such elements, an empty list is returned.
     * @throws NullPointerException if either {@code oldList} or {@code newList} is {@code null}.
     */
    public static <T> List<T> getAddedElementsFromList(List<T> oldList, List<T> newList) {
        return newList.stream().filter(t -> !oldList.contains(t)).toList();
    }

    /**
     * Transforms a list of elements into a list of transformed elements using a mapping function.
     * This method processes a given list by applying the specified mapping function to each element in the list, and then collects the results into a new list.
     *
     * @param <T>    the type of elements in the source list
     * @param <R>    the type of elements in the resulting list, which is the result of applying the mapping function
     * @param list   the list of items to be transformed. Must not be {@code null}.
     * @param mapper a function that transforms each element of the source list into an element of the resulting list. Must not be {@code null}.
     * @return a new list containing the results of applying the mapping function to each element of the source list.
     * @throws NullPointerException if {@code list} or {@code mapper} is {@code null}.
     */
    public static <T, R> List<R> transform(List<T> list, Function<T, R> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * Transforms a collection into another collection by applying a mapping function.
     * This method processes a given collection by applying a specified mapping function to each element, and then collects the results into a target collection using the provided collector.
     *
     * @param <T>        the type of elements in the source collection
     * @param <R>        the type of elements in the target collection, which is the result of applying the mapping function
     * @param <C>        the type of the source collection, which must extend {@link Collection}
     * @param <D>        the type of the target collection, which must extend {@link Collection}
     * @param collection the collection of items to be transformed. Must not be {@code null}.
     * @param mapper     a function that transforms each element of the source collection into an element of the target collection. Must not be {@code null}.
     * @param collector  a {@link Collector} used to accumulate the transformed elements into the target collection. Must not be {@code null}.
     * @return a new collection of type {@code D} containing the results of applying the mapping function to each element of the source collection, accumulated using the provided collector.
     * @throws NullPointerException if {@code collection}, {@code mapper}, or {@code collector} is {@code null}.
     */
    public static <T, R, C extends Collection<T>, D extends Collection<R>> D transform(
            C collection,
            Function<T, R> mapper,
            Collector<? super R, ?, D> collector
    ) {
        return collection
                .stream()
                .map(mapper)
                .collect(collector);
    }

    /**
     * Transforms a list into a map using the specified mapping function.
     * This method converts a given list of items into a map, where each key in the map is produced by applying the provided mapping function to each item in the list. The values in the map are the items themselves.
     *
     * @param <T>    the type of items in the list
     * @param <R>    the type of the map's keys, generated by the mapping function
     * @param list   the list of items to be transformed into a map. Must not be {@code null}.
     * @param mapper a function that extracts the key for each item in the list. Must not be {@code null}.
     * @return a map where each key is produced by applying the mapping function to an item in the list, and the value is the item itself.
     * @throws NullPointerException     if {@code list} or {@code mapper} is {@code null}.
     * @throws IllegalArgumentException if the mapping function produces duplicate keys.
     */
    public static <T, R> Map<R, T> transformToMap(List<T> list, Function<T, R> mapper) {
        return list.stream().collect(Collectors.toMap(mapper, Function.identity()));
    }

    /**
     * Transforms a collection into a map using the specified mapping function.
     * This method converts a given collection of items into a map, where the keys of the map are generated by applying the provided mapping function to each item in the collection. The values in the map are the items themselves.
     *
     * @param <T>        the type of items in the collection
     * @param <R>        the type of the map's keys, generated by the mapping function
     * @param collection the collection of items to be transformed into a map. Must not be {@code null}.
     * @param mapper     a function that extracts the key for each item in the collection. Must not be {@code null}.
     * @return a map where each key is produced by applying the mapping function to an item in the collection, and the value is the item itself.
     * @throws NullPointerException     if {@code collection} or {@code mapper} is {@code null}.
     * @throws IllegalArgumentException if the mapping function produces duplicate keys.
     */
    public static <T, R> Map<R, T> transformToMap(Collection<T> collection, Function<T, R> mapper) {
        return collection.stream().collect(Collectors.toMap(mapper, Function.identity()));
    }

}
