package bg.energo.phoenix.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

//@Component
public class SQLFunctionContributor implements FunctionContributor {



    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
        BasicType<String> stringBasicType = basicTypeRegistry.resolve(StandardBasicTypes.STRING);
        BasicType<Boolean> booleanBasicType = basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN);
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry.registerPattern("string_agg",
                "string_agg(?1, ?2)", stringBasicType);
        functionRegistry.registerPattern("arrays_intersect",
                "cast(?1 as text[]) && cast(?2 as text[])", booleanBasicType);
        functionRegistry.registerNamed(
                "text",
                basicTypeRegistry.resolve(StandardBasicTypes.STRING)
        );
        functionRegistry.registerPattern(
                "array_intersect",
                "arrays_intersect(?1, ?2)", booleanBasicType
        );
        functionRegistry.registerPattern("array_length", "array_length(?1)", basicTypeRegistry.resolve(StandardBasicTypes.INTEGER));
        functionRegistry.registerPattern("array_to_string", "array_to_string(?1, ',')", stringBasicType);
        functionRegistry.registerPattern("to_days", "to_days(?1)", basicTypeRegistry.resolve(StandardBasicTypes.INTEGER));
    }
}

/*implements MetadataBuilderContributor {
    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applySqlFunction("get_account_manager",
                new StandardSQLFunction("get_account_manager",StringType.INSTANCE));
    }


}*/
