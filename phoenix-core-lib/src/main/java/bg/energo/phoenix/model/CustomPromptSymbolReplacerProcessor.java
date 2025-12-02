package bg.energo.phoenix.model;
import bg.energo.phoenix.util.StringUtil;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class CustomPromptSymbolReplacerProcessor {
    public void process(Object requestObject) {
        Class<?> clazz = requestObject.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    if(field.getName().equals("prompt")){
                        String originalValue = (String) field.get(requestObject);
                        String modifiedValue = StringUtil.underscoreReplacer(originalValue); // originalValue.replace("_", "\\_");
                        field.set(requestObject, modifiedValue);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }
}
