//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
package io.deephaven.engine.util;

import io.deephaven.engine.context.ExecutionContext;
import io.deephaven.engine.context.QueryCompilerImpl;
import io.deephaven.engine.context.QueryCompilerRequest;

import java.util.*;
import java.util.function.Supplier;

/**
 * Utilities for dynamic compilation.
 */
public class DynamicCompileUtils {

    public static <T> Supplier<T> compileSimpleFunction(final Class<? extends T> resultType, final String code) {
        return compileSimpleFunction(resultType, code, Collections.emptyList(), Collections.emptyList());
    }

    public static <T> Supplier<T> compileSimpleStatement(final Class<? extends T> resultType, final String code,
            final String... imports) {
        final List<Class<?>> importClasses = new ArrayList<>();
        for (final String importString : imports) {
            try {
                importClasses.add(Class.forName(importString));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not find class", e);
            }
        }

        return compileSimpleFunction(resultType, "return " + code, importClasses, Collections.emptyList());
    }

    public static <T> Supplier<T> compileSimpleFunction(final Class<? extends T> resultType, final String code,
            final Collection<Class<?>> imports, final Collection<Class<?>> staticImports) {
        final StringBuilder classBody = new StringBuilder();

        classBody.append("import ").append(resultType.getName()).append(";\n");
        for (final Class<?> im : imports) {
            classBody.append("import ").append(im.getName()).append(";\n");
        }
        for (final Class<?> sim : staticImports) {
            classBody.append("import static ").append(sim.getName()).append(".*;\n");
        }

        classBody.append("public class $CLASSNAME$ implements ").append(Supplier.class.getCanonicalName()).append("<")
                .append(resultType.getCanonicalName()).append(">").append(" ").append("{\n");
        classBody.append("  @Override\n");
        classBody.append("  public ").append(resultType.getCanonicalName()).append(" get() {\n");
        classBody.append(code).append(";\n");
        classBody.append("  }\n");
        classBody.append("}\n");

        final Class<?> partitionClass = ExecutionContext.getContext().getQueryCompiler().compile(
                QueryCompilerRequest.builder()
                        .description("Simple Function: " + code)
                        .className("Function")
                        .classBody(classBody.toString())
                        .packageNameRoot(QueryCompilerImpl.FORMULA_CLASS_PREFIX)
                        .build());

        try {
            // noinspection unchecked
            return ((Supplier<T>) partitionClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not instantiate function.", e);
        }
    }

    public static Class<?> getClassThroughCompilation(final String object) {
        final StringBuilder classBody = new StringBuilder();
        classBody.append("public class $CLASSNAME$ implements ").append(Supplier.class.getCanonicalName())
                .append("<Class>{ \n");
        classBody.append("  @Override\n");
        classBody.append("  public Class get() { return ").append(object).append(".class; }\n");
        classBody.append("}\n");

        final Class<?> partitionClass = ExecutionContext.getContext().getQueryCompiler().compile(
                QueryCompilerRequest.builder()
                        .description("Formula: return " + object + ".class")
                        .className("Function")
                        .classBody(classBody.toString())
                        .packageNameRoot(QueryCompilerImpl.FORMULA_CLASS_PREFIX)
                        .build());

        try {
            // noinspection unchecked
            return ((Supplier<Class<?>>) partitionClass.newInstance()).get();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not instantiate function.", e);
        }
    }
}
