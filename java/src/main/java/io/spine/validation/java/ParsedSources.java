/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.validation.java;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.spine.text.Text;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Annotation;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.JavaClass;
import org.jboss.forge.roaster.model.JavaInterface;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.Property;
import org.jboss.forge.roaster.model.SyntaxError;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.TypeVariable;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.PropertyHolderSource;
import org.jboss.forge.roaster.model.source.PropertySource;
import org.jboss.forge.roaster.model.source.TypeVariableSource;

import java.util.List;

/**
 * Parses the source code via {@code Roaster} and caches the results for further use.
 */
final class ParsedSources {

    /**
     * Cached results of parsing the Java source code.
     */
    private final LoadingCache<Text, JavaSource<?>> cache =
            CacheBuilder.newBuilder()
                    .maximumSize(300)
                    .build(loader());

    private static CacheLoader<Text, JavaSource<?>> loader() {
        return new CacheLoader<>() {
            @Override
            public JavaSource<?> load(Text code) {
                var result = Roaster.parse(JavaSource.class, code.getValue());
                if(result.isClass()) {
                    return new CachingJavaClassSource((JavaClassSource) result);
                }
                return result;
            }
        };
    }

    /**
     * Parses the Java code and returns it as the parsed {@code JavaSource},
     * caching it for future use.
     *
     * <p>If the code was parsed previously, most likely the cached result
     * is returned right away, as the cache stores 300 items max.
     */
    JavaSource<?> get(Text code) {
        var result = cache.getUnchecked(code);
        return result;
    }

    @SuppressWarnings("OverlyComplexClass") // Delegating everything!
    private static final class CachingJavaClassSource implements JavaClassSource {
        private final JavaClassSource delegate;

        /**
         * Cached value of {@link JavaClassSource#getNestedTypes()
         * JavaClassSource.getNestedTypes()}.
         */
        private @Nullable List<JavaSource<?>> nestedTypes;

        private CachingJavaClassSource(JavaClassSource delegate) {
            this.delegate = delegate;
        }

        @Override
        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
        public List<JavaSource<?>> getNestedTypes() {
            if(nestedTypes == null) {
                nestedTypes = delegate.getNestedTypes();
            }
            return nestedTypes;
        }

        // *** The rest of methods are just delegating their calls. ***

        @Override
        public boolean isLocalClass() {
            return delegate.isLocalClass();
        }

        @Override
        public String getCanonicalName() {
            return delegate.getCanonicalName();
        }

        @Override
        public String getQualifiedName() {
            return delegate.getQualifiedName();
        }

        @Override
        public List<SyntaxError> getSyntaxErrors() {
            return delegate.getSyntaxErrors();
        }

        @Override
        public boolean hasSyntaxErrors() {
            return delegate.hasSyntaxErrors();
        }

        @Override
        public boolean isClass() {
            return delegate.isClass();
        }

        @Override
        public boolean isEnum() {
            return delegate.isEnum();
        }

        @Override
        public boolean isInterface() {
            return delegate.isInterface();
        }

        @Override
        public boolean isAnnotation() {
            return delegate.isAnnotation();
        }

        @Override
        public boolean isRecord() {
            return delegate.isRecord();
        }


        @Override
        public String toUnformattedString() {
            return delegate.toUnformattedString();
        }

        @Override
        public String getPackage() {
            return delegate.getPackage();
        }

        @Override
        public boolean isDefaultPackage() {
            return delegate.isDefaultPackage();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public boolean isPackagePrivate() {
            return delegate.isPackagePrivate();
        }

        @Override
        public boolean isPublic() {
            return delegate.isPublic();
        }

        @Override
        public boolean isPrivate() {
            return delegate.isPrivate();
        }

        @Override
        public boolean isProtected() {
            return delegate.isProtected();
        }

        @Override
        public Visibility getVisibility() {
            return delegate.getVisibility();
        }

        @Override
        public List<AnnotationSource<JavaClassSource>> getAnnotations() {
            return delegate.getAnnotations();
        }

        @Override
        public boolean hasAnnotation(Class<? extends java.lang.annotation.Annotation> type) {
            return delegate.hasAnnotation(type);
        }

        @Override
        public boolean hasAnnotation(String type) {
            return delegate.hasAnnotation(type);
        }

        @Override
        public AnnotationSource<JavaClassSource> getAnnotation(
                Class<? extends java.lang.annotation.Annotation> type) {
            return delegate.getAnnotation(type);
        }

        @Override
        public AnnotationSource<JavaClassSource> getAnnotation(String type) {
            return delegate.getAnnotation(type);
        }

        @Override
        public Object getInternal() {
            return delegate.getInternal();
        }

        @Override
        public JavaClassSource getOrigin() {
            return delegate.getOrigin();
        }

        @Override
        public JavaDocSource<JavaClassSource> getJavaDoc() {
            return delegate.getJavaDoc();
        }

        @Override
        public boolean hasJavaDoc() {
            return delegate.hasJavaDoc();
        }

        @Override
        public boolean hasProperty(String name) {
            return delegate.hasProperty(name);
        }

        @Override
        public boolean hasProperty(Property<JavaClassSource> property) {
            return delegate.hasProperty(property);
        }

        @Override
        public PropertySource<JavaClassSource> getProperty(String name) {
            return delegate.getProperty(name);
        }

        @Override
        public List<PropertySource<JavaClassSource>> getProperties(Class<?> type) {
            return delegate.getProperties(type);
        }

        @Override
        public List<PropertySource<JavaClassSource>> getProperties() {
            return delegate.getProperties();
        }

        @Override
        public boolean hasMethod(Method<JavaClassSource, ?> name) {
            return delegate.hasMethod(name);
        }

        @Override
        public boolean hasMethodSignature(Method<?, ?> method) {
            return delegate.hasMethodSignature(method);
        }

        @Override
        public boolean hasMethodSignature(String name) {
            return delegate.hasMethodSignature(name);
        }

        @Override
        public boolean hasMethodSignature(String name, String... paramTypes) {
            return delegate.hasMethodSignature(name, paramTypes);
        }

        @Override
        public boolean hasMethodSignature(String name, Class<?>... paramTypes) {
            return delegate.hasMethodSignature(name, paramTypes);
        }

        @Override
        public MethodSource<JavaClassSource> getMethod(String name) {
            return delegate.getMethod(name);
        }

        @Override
        public MethodSource<JavaClassSource> getMethod(String name, String... paramTypes) {
            return delegate.getMethod(name, paramTypes);
        }

        @Override
        public MethodSource<JavaClassSource> getMethod(String name, Class<?>... paramTypes) {
            return delegate.getMethod(name, paramTypes);
        }

        @Override
        public List<MethodSource<JavaClassSource>> getMethods() {
            return delegate.getMethods();
        }

        @Override
        public List<MemberSource<JavaClassSource, ?>> getMembers() {
            return delegate.getMembers();
        }

        @Override
        public boolean hasField(String name) {
            return delegate.hasField(name);
        }

        @Override
        public boolean hasField(Field<JavaClassSource> field) {
            return delegate.hasField(field);
        }

        @Override
        public FieldSource<JavaClassSource> getField(String name) {
            return delegate.getField(name);
        }

        @Override
        public List<FieldSource<JavaClassSource>> getFields() {
            return delegate.getFields();
        }

        @Override
        public List<String> getInterfaces() {
            return delegate.getInterfaces();
        }

        @Override
        public boolean hasInterface(String type) {
            return delegate.hasInterface(type);
        }

        @Override
        public boolean hasInterface(Class<?> type) {
            return delegate.hasInterface(type);
        }

        @Override
        public boolean hasInterface(JavaInterface<?> type) {
            return delegate.hasInterface(type);
        }

        @Override
        public List<TypeVariableSource<JavaClassSource>> getTypeVariables() {
            return delegate.getTypeVariables();
        }

        @Override
        public TypeVariableSource<JavaClassSource> getTypeVariable(String name) {
            return delegate.getTypeVariable(name);
        }

        @Override
        public boolean hasTypeVariable(String name) {
            return delegate.hasTypeVariable(name);
        }

        @Override
        public String getSuperType() {
            return delegate.getSuperType();
        }

        @Override
        public boolean isAbstract() {
            return delegate.isAbstract();
        }

        @Override
        public boolean hasNestedType(String name) {
            return delegate.hasNestedType(name);
        }

        @Override
        public boolean hasNestedType(JavaType<?> type) {
            return delegate.hasNestedType(type);
        }

        @Override
        public boolean hasNestedType(Class<?> type) {
            return delegate.hasNestedType(type);
        }

        @Override
        public JavaSource<?> getNestedType(String name) {
            return delegate.getNestedType(name);
        }

        @Override
        public boolean isFinal() {
            return delegate.isFinal();
        }

        @Override
        public boolean isStatic() {
            return delegate.isStatic();
        }

        @Override
        public JavaClassSource setPackage(String name) {
            return delegate.setPackage(name);
        }

        @Override
        public JavaClassSource setDefaultPackage() {
            return delegate.setDefaultPackage();
        }

        @Override
        public boolean hasImport(Class<?> type) {
            return delegate.hasImport(type);
        }

        @Override
        public boolean hasImport(String type) {
            return delegate.hasImport(type);
        }

        @Override
        public boolean requiresImport(Class<?> type) {
            return delegate.requiresImport(type);
        }

        @Override
        public boolean requiresImport(String type) {
            return delegate.requiresImport(type);
        }

        @Override
        public <T extends JavaType<T>> boolean hasImport(T type) {
            return delegate.hasImport(type);
        }

        @Override
        public boolean hasImport(Import imprt) {
            return delegate.hasImport(imprt);
        }

        @Override
        public Import getImport(String literalValue) {
            return delegate.getImport(literalValue);
        }

        @Override
        public Import getImport(Class<?> type) {
            return delegate.getImport(type);
        }

        @Override
        public <T extends JavaType<?>> Import getImport(T type) {
            return delegate.getImport(type);
        }

        @Override
        public Import getImport(Import imprt) {
            return delegate.getImport(imprt);
        }

        @Override
        public List<Import> getImports() {
            return delegate.getImports();
        }

        @Override
        public String resolveType(String type) {
            return delegate.resolveType(type);
        }

        @Override
        public Import addImport(String className) {
            return delegate.addImport(className);
        }

        @Override
        public Import addImport(Class<?> type) {
            return delegate.addImport(type);
        }

        @Override
        public Import addImport(Import imprt) {
            return delegate.addImport(imprt);
        }

        @Override
        public <T extends JavaType<?>> Import addImport(T type) {
            return delegate.addImport(type);
        }

        @Override
        public Import addImport(Type<?> type) {
            return delegate.addImport(type);
        }

        @Override
        public JavaClassSource removeImport(String name) {
            return delegate.removeImport(name);
        }

        @Override
        public JavaClassSource removeImport(Class<?> type) {
            return delegate.removeImport(type);
        }

        @Override
        public <T extends JavaType<?>> JavaClassSource removeImport(T type) {
            return delegate.removeImport(type);
        }

        @Override
        public JavaClassSource removeImport(Import imprt) {
            return delegate.removeImport(imprt);
        }

        @Override
        public JavaClassSource setName(String name) {
            return delegate.setName(name);
        }

        @Override
        public JavaClassSource setPackagePrivate() {
            return delegate.setPackagePrivate();
        }

        @Override
        public JavaClassSource setPublic() {
            return delegate.setPublic();
        }

        @Override
        public JavaClassSource setPrivate() {
            return delegate.setPrivate();
        }

        @Override
        public JavaClassSource setProtected() {
            return delegate.setProtected();
        }

        @Override
        public JavaClassSource setVisibility(Visibility scope) {
            return delegate.setVisibility(scope);
        }

        @Override
        public AnnotationSource<JavaClassSource> addAnnotation() {
            return delegate.addAnnotation();
        }

        @Override
        public AnnotationSource<JavaClassSource> addAnnotation(
                Class<? extends java.lang.annotation.Annotation> type) {
            return delegate.addAnnotation(type);
        }

        @Override
        public AnnotationSource<JavaClassSource> addAnnotation(String className) {
            return delegate.addAnnotation(className);
        }

        @Override
        public JavaClassSource removeAnnotation(Annotation<JavaClassSource> annotation) {
            return delegate.removeAnnotation(annotation);
        }

        @Override
        public void removeAllAnnotations() {
            delegate.removeAllAnnotations();
        }

        @Override
        public JavaClassSource removeJavaDoc() {
            return delegate.removeJavaDoc();
        }

        @Override
        public int getStartPosition() {
            return delegate.getStartPosition();
        }

        @Override
        public int getEndPosition() {
            return delegate.getEndPosition();
        }

        @Override
        public int getLineNumber() {
            return delegate.getLineNumber();
        }

        @Override
        public int getColumnNumber() {
            return delegate.getColumnNumber();
        }

        @Override
        public JavaClassSource addInterface(String type) {
            return delegate.addInterface(type);
        }

        @Override
        public JavaClassSource addInterface(Class<?> type) {
            return delegate.addInterface(type);
        }

        @Override
        public JavaClassSource implementInterface(Class<?> type) {
            return delegate.implementInterface(type);
        }

        @Override
        public JavaClassSource implementInterface(JavaInterface<?> type) {
            return delegate.implementInterface(type);
        }

        @Override
        public JavaClassSource addInterface(JavaInterface<?> type) {
            return delegate.addInterface(type);
        }

        @Override
        public JavaClassSource removeInterface(String type) {
            return delegate.removeInterface(type);
        }

        @Override
        public JavaClassSource removeInterface(Class<?> type) {
            return delegate.removeInterface(type);
        }

        @Override
        public JavaClassSource removeInterface(JavaInterface<?> type) {
            return delegate.removeInterface(type);
        }

        @Override
        public TypeVariableSource<JavaClassSource> addTypeVariable() {
            return delegate.addTypeVariable();
        }

        @Override
        public TypeVariableSource<JavaClassSource> addTypeVariable(String name) {
            return delegate.addTypeVariable(name);
        }

        @Override
        public JavaClassSource removeTypeVariable(String name) {
            return delegate.removeTypeVariable(name);
        }

        @Override
        public JavaClassSource removeTypeVariable(TypeVariable<?> typeVariable) {
            return delegate.removeTypeVariable(typeVariable);
        }

        @Override
        public JavaClassSource setSuperType(JavaType<?> type) {
            return delegate.setSuperType(type);
        }

        @Override
        public JavaClassSource setSuperType(Class<?> type) {
            return delegate.setSuperType(type);
        }

        @Override
        public JavaClassSource extendSuperType(Class<?> type) {
            return delegate.extendSuperType(type);
        }

        @Override
        public JavaClassSource extendSuperType(JavaClass<?> type) {
            return delegate.extendSuperType(type);
        }

        @Override
        public JavaClassSource setSuperType(String type) {
            return delegate.setSuperType(type);
        }

        @Override
        public JavaClassSource setAbstract(boolean abstrct) {
            return delegate.setAbstract(abstrct);
        }

        @Override
        public PropertySource<JavaClassSource> addProperty(String type, String name) {
            return delegate.addProperty(type, name);
        }

        @Override
        public PropertySource<JavaClassSource> addProperty(Class<?> type, String name) {
            return delegate.addProperty(type, name);
        }

        @Override
        public PropertySource<JavaClassSource> addProperty(JavaType<?> type, String name) {
            return delegate.addProperty(type, name);
        }

        @Override
        public PropertyHolderSource<JavaClassSource> removeProperty(
                Property<JavaClassSource> property) {
            return delegate.removeProperty(property);
        }

        @Override
        public MethodSource<JavaClassSource> addMethod() {
            return delegate.addMethod();
        }

        @Override
        public MethodSource<JavaClassSource> addMethod(String method) {
            return delegate.addMethod(method);
        }

        @Override
        public MethodSource<JavaClassSource> addMethod(java.lang.reflect.Method method) {
            return delegate.addMethod(method);
        }

        @Override
        public MethodSource<JavaClassSource> addMethod(Method<?, ?> method) {
            return delegate.addMethod(method);
        }

        @Override
        public JavaClassSource removeMethod(Method<JavaClassSource, ?> method) {
            return delegate.removeMethod(method);
        }

        @Override
        public FieldSource<JavaClassSource> addField() {
            return delegate.addField();
        }

        @Override
        public FieldSource<JavaClassSource> addField(String declaration) {
            return delegate.addField(declaration);
        }

        @Override
        public JavaClassSource removeField(Field<JavaClassSource> field) {
            return delegate.removeField(field);
        }

        @Override
        public <NESTED_TYPE extends JavaSource<?>> NESTED_TYPE addNestedType(
                Class<NESTED_TYPE> type) {
            return delegate.addNestedType(type);
        }

        @Override
        public <NESTED_TYPE extends JavaSource<?>> NESTED_TYPE addNestedType(String declaration) {
            return delegate.addNestedType(declaration);
        }

        @Override
        public <NESTED_TYPE extends JavaSource<?>> NESTED_TYPE addNestedType(NESTED_TYPE type) {
            return delegate.addNestedType(type);
        }

        @Override
        public JavaClassSource removeNestedType(JavaSource<?> type) {
            return delegate.removeNestedType(type);
        }

        @Override
        public JavaClassSource setFinal(boolean finl) {
            return delegate.setFinal(finl);
        }

        @Override
        public JavaClassSource setStatic(boolean value) {
            return delegate.setStatic(value);
        }

        @Override
        public JavaSource<?> getEnclosingType() {
            return delegate.getEnclosingType();
        }
    }
}
