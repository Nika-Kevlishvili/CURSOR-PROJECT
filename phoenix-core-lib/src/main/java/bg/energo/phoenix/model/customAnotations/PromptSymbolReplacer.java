package bg.energo.phoenix.model.customAnotations;

import bg.energo.phoenix.model.CustomPromptSymbolReplacerProcessor;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PromptSymbolReplacer.promptSymbolReplacerImpl.class})
public @interface PromptSymbolReplacer {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class promptSymbolReplacerImpl implements ConstraintValidator<PromptSymbolReplacer, Object> {

        private final CustomPromptSymbolReplacerProcessor promptSymbolReplacerProcessor;

        public promptSymbolReplacerImpl(CustomPromptSymbolReplacerProcessor promptSymbolReplacerProcessor) {
            this.promptSymbolReplacerProcessor = promptSymbolReplacerProcessor;
        }

        @Override
        public boolean isValid(Object listingObject, ConstraintValidatorContext context) {
            promptSymbolReplacerProcessor.process(listingObject);
            return true;
        }

    }
}
