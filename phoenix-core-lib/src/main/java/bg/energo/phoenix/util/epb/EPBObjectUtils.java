package bg.energo.phoenix.util.epb;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
public class EPBObjectUtils {

    public static boolean isAnyFieldNotNull(Object object) {
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(object);
                if (value != null) {
                    return true;
                }
            } catch (IllegalAccessException e) {
                log.error("Error accessing field: {}", field.getName());
            }
        }
        return false;
    }

}
