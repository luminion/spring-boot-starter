package io.github.luminion.velo.jackson.serializer;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.jackson.annotation.JsonEnum;
import io.github.luminion.velo.jackson.support.JsonEnumMetadata;
import io.github.luminion.velo.jackson.support.JsonEnumMetadataResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

            String targetName = targetName(config, writer.getName(), ann.nameSuffix());
            if (existNames.contains(targetName)) {
                log.warn("Skip JsonEnum derived property because target property already exists: {}", targetName);
                continue;
            }

            newProperties.add(new JsonEnumPropertyWriter(writer, targetName, metadata));
            existNames.add(targetName);
        }
        return newProperties;
    }

    private String targetName(SerializationConfig config, String baseName, String annotationSuffix) {
        String suffix = StringUtils.hasText(annotationSuffix) ? annotationSuffix : jacksonProperties.getEnumNameSuffix();
        String logicalName = baseName + StringUtils.capitalize(suffix);
        if (config.getPropertyNamingStrategy() == null) {
            return logicalName;
        }
        return config.getPropertyNamingStrategy().nameForField(config, null, logicalName);
    }

    private static class JsonEnumPropertyWriter extends BeanPropertyWriter {
        private final String targetName;
        private final JsonEnumMetadata metadata;

        public JsonEnumPropertyWriter(BeanPropertyWriter base, String targetName, JsonEnumMetadata metadata) {
            super(base, PropertyName.construct(targetName));
            this.targetName = targetName;
            this.metadata = metadata;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctxt) throws Exception {
            Object value;
            try {
                value = get(bean);
            } catch (Exception e) {
                log.warn("Failed to read JsonEnum source property: {}", getName(), e);
                return;
            }
            if (value == null) {
                return;
            }

            Object name;
            try {
                name = metadata.getName(value);
            } catch (RuntimeException e) {
                log.warn("Failed to resolve JsonEnum derived value for property: {}", getName(), e);
                return;
            }
            if (name != null) {
                gen.writeStringProperty(targetName, name.toString());
            }
        }
    }
}
