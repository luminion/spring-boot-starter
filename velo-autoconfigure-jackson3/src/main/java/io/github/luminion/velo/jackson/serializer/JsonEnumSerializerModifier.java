package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.HandlerInstantiator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
import tools.jackson.databind.ser.jdk.StringSerializer;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.SimpleBeanPropertyDefinition;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.jackson.annotation.JsonEnum;
import io.github.luminion.velo.jackson.support.JsonEnumMetadata;
import io.github.luminion.velo.jackson.support.JsonEnumMetadataResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class JsonEnumSerializerModifier extends ValueSerializerModifier {

    private final JsonEnumMetadataResolver metadataResolver;
    private final VeloProperties.JacksonProperties jacksonProperties;

    public JsonEnumSerializerModifier(VeloProperties.JacksonProperties jacksonProperties) {
        this.jacksonProperties = jacksonProperties;
        this.metadataResolver = new JsonEnumMetadataResolver(jacksonProperties);
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> newProperties = new ArrayList<>(beanProperties);
        Set<String> existNames = new HashSet<>();
        Map<String, BeanPropertyDefinition> definitions = new HashMap<>();
        for (BeanPropertyDefinition definition : beanDesc.get().findProperties()) {
            definitions.put(definition.getName(), definition);
        }
        PropertyNamingStrategy strategy = namingStrategy(config, beanDesc.getClassInfo());
        for (BeanPropertyWriter writer : beanProperties) {
            existNames.add(writer.getName());
        }

        for (BeanPropertyWriter writer : beanProperties) {
            JsonEnum ann = writer.getAnnotation(JsonEnum.class);
            if (ann == null) {
                continue;
            }

            JsonEnumMetadata metadata;
            try {
                metadata = metadataResolver.resolve(ann.value(), ann.codeField(), ann.nameField(),
                        writer.getType().getRawClass());
            } catch (RuntimeException e) {
                log.warn("Failed to resolve JsonEnum metadata for property: {}", writer.getName(), e);
                continue;
            }
            if (metadata == null) {
                log.warn("No JsonEnum mapping found for property: {}, enum: {}", writer.getName(), ann.value().getName());
                continue;
            }

            String targetName = targetName(config, writer, definitions.get(writer.getName()), strategy, ann.nameSuffix());
            if (existNames.contains(targetName)) {
                log.warn("Skip JsonEnum derived property because target property already exists: {}", targetName);
                continue;
            }

            newProperties.add(new JsonEnumPropertyWriter(config, beanDesc, writer, targetName, metadata));
            existNames.add(targetName);
        }
        return newProperties;
    }

    private PropertyNamingStrategy namingStrategy(SerializationConfig config, AnnotatedClass declaringClass) {
        Object definition = config.getAnnotationIntrospector().findNamingStrategy(config, declaringClass);
        if (definition == null) {
            return config.getPropertyNamingStrategy();
        }
        if (definition instanceof PropertyNamingStrategy) {
            return (PropertyNamingStrategy) definition;
        }
        Class<?> strategyClass = (Class<?>) definition;
        if (strategyClass == PropertyNamingStrategy.class) {
            return null;
        }
        HandlerInstantiator instantiator = config.getHandlerInstantiator();
        PropertyNamingStrategy strategy = instantiator == null ? null
                : instantiator.namingStrategyInstance(config, declaringClass, strategyClass);
        return strategy == null
                ? (PropertyNamingStrategy) ClassUtil.createInstance(strategyClass, config.canOverrideAccessModifiers())
                : strategy;
    }

    private String targetName(SerializationConfig config, BeanPropertyWriter writer, BeanPropertyDefinition definition,
                              PropertyNamingStrategy strategy, String annotationSuffix) {
        String suffix = StringUtils.hasText(annotationSuffix) ? annotationSuffix : jacksonProperties.getEnumNameSuffix();
        String baseName = definition == null || definition.isExplicitlyNamed()
                ? writer.getName() : definition.getInternalName();
        String logicalName = baseName + StringUtils.capitalize(suffix);
        if (strategy == null) {
            return logicalName;
        }
        if (writer.getMember() instanceof AnnotatedMethod) {
            return strategy.nameForGetterMethod(config, (AnnotatedMethod) writer.getMember(), logicalName);
        }
        return strategy.nameForField(config, (AnnotatedField) writer.getMember(), logicalName);
    }

    private static class JsonEnumPropertyWriter extends VirtualBeanPropertyWriter {
        private final SourcePropertyWriter source;
        private final JsonEnumMetadata metadata;

        JsonEnumPropertyWriter(SerializationConfig config, BeanDescription.Supplier beanDesc, BeanPropertyWriter base,
                               String targetName, JsonEnumMetadata metadata) {
            super(SimpleBeanPropertyDefinition.construct(config, base.getMember(), PropertyName.construct(targetName)),
                    beanDesc.getClassAnnotations(), config.constructType(String.class), new StringSerializer(),
                    null, null, JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS),
                    base.getViews());
            this.source = new SourcePropertyWriter(base);
            this.metadata = metadata;
        }

        @Override
        protected Object value(Object bean, JsonGenerator generator, SerializationContext provider) throws Exception {
            Object value = source.get(bean);
            if (source.isSuppressed(value, provider)) {
                return null;
            }
            Object name = metadata.getName(value);
            return name == null ? null : name.toString();
        }

        @Override
        public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass,
                                                    BeanPropertyDefinition definition, JavaType type) {
            return this;
        }
    }

    /**
     * 沿用原字段已经计算好的包含策略，派生值再交由原生虚拟属性输出。
     */
    private static class SourcePropertyWriter extends BeanPropertyWriter {
        SourcePropertyWriter(BeanPropertyWriter source) {
            super(source);
        }

        boolean isSuppressed(Object value, SerializationContext provider) throws Exception {
            if (value == null) {
                return true;
            }
            if (_suppressableValue == MARKER_FOR_EMPTY) {
                ValueSerializer<Object> serializer = _serializer;
                if (serializer == null) {
                    serializer = provider.findPrimaryPropertySerializer(getType(), this);
                }
                return serializer.isEmpty(provider, value);
            }
            return _suppressableValue != null && _suppressableValue.equals(value);
        }
    }
}
