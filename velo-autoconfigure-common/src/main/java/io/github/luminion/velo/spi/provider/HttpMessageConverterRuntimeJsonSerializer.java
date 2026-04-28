package io.github.luminion.velo.spi.provider;

import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.util.InvocationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime JSON serializer backed by the actual MVC message converters.
 */
public class HttpMessageConverterRuntimeJsonSerializer implements RuntimeJsonSerializer {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private static final String OMITTED_VALUE = "[omitted]";

    private static final String CIRCULAR_VALUE = "[circular]";

    private final List<HttpMessageConverter<?>> converters;

    public HttpMessageConverterRuntimeJsonSerializer(List<HttpMessageConverter<?>> converters) {
        this.converters = converters;
    }

    @Override
    public String toJson(Object value) {
        Object jsonValue = sanitizeValue(value, new IdentityHashMap<>());
        if (jsonValue == null) {
            return "null";
        }
        if (jsonValue instanceof CharSequence || jsonValue instanceof Character) {
            return "\"" + escapeJson(jsonValue.toString()) + "\"";
        }

        Class<?> valueType = jsonValue.getClass();
        for (HttpMessageConverter<?> converter : converters) {
            if (!isJsonConverter(converter)) {
                continue;
            }
            try {
                if (converter instanceof GenericHttpMessageConverter<?>) {
                    GenericHttpMessageConverter<Object> genericConverter =
                            (GenericHttpMessageConverter<Object>) converter;
                    if (genericConverter.canWrite((Type) valueType, valueType, JSON)) {
                        ByteArrayHttpOutputMessage outputMessage = new ByteArrayHttpOutputMessage();
                        genericConverter.write(jsonValue, (Type) valueType, JSON, outputMessage);
                        return outputMessage.getBodyAsString();
                    }
                } else if (converter.canWrite(valueType, JSON)) {
                    HttpMessageConverter<Object> objectConverter = (HttpMessageConverter<Object>) converter;
                    ByteArrayHttpOutputMessage outputMessage = new ByteArrayHttpOutputMessage();
                    objectConverter.write(jsonValue, JSON, outputMessage);
                    return outputMessage.getBodyAsString();
                }
            } catch (IOException ex) {
                return "\"" + escapeJson(InvocationUtils.formatValue(jsonValue, 0)) + "\"";
            } catch (RuntimeException ex) {
                return "\"" + escapeJson(InvocationUtils.formatValue(jsonValue, 0)) + "\"";
            }
        }
        return "\"" + escapeJson(InvocationUtils.formatValue(jsonValue, 0)) + "\"";
    }

    private boolean isJsonConverter(HttpMessageConverter<?> converter) {
        List<MediaType> mediaTypes = converter.getSupportedMediaTypes();
        for (MediaType mediaType : mediaTypes) {
            if (mediaType.getSubtype().toLowerCase(Locale.ENGLISH).contains("json")) {
                return true;
            }
        }
        return false;
    }

    private Object sanitizeValue(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            return null;
        }
        Class<?> valueType = value.getClass();
        if (valueType.isArray()) {
            if (visited.containsKey(value)) {
                return CIRCULAR_VALUE;
            }
            visited.put(value, Boolean.TRUE);
            int length = Array.getLength(value);
            List<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(sanitizeValue(Array.get(value, i), visited));
            }
            visited.remove(value);
            return values;
        }
        if (value instanceof Collection<?>) {
            if (visited.containsKey(value)) {
                return CIRCULAR_VALUE;
            }
            visited.put(value, Boolean.TRUE);
            Collection<?> collection = (Collection<?>) value;
            List<Object> values = new ArrayList<>(collection.size());
            for (Object item : collection) {
                values.add(sanitizeValue(item, visited));
            }
            visited.remove(value);
            return values;
        }
        if (value instanceof Map<?, ?>) {
            if (visited.containsKey(value)) {
                return CIRCULAR_VALUE;
            }
            visited.put(value, Boolean.TRUE);
            Map<?, ?> map = (Map<?, ?>) value;
            Map<Object, Object> values = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                values.put(sanitizeValue(entry.getKey(), visited), sanitizeValue(entry.getValue(), visited));
            }
            visited.remove(value);
            return values;
        }
        if (!InvocationUtils.isLoggableValue(value)) {
            return OMITTED_VALUE;
        }
        return value;
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        return builder.toString();
    }

    private static final class ByteArrayHttpOutputMessage implements HttpOutputMessage {

        private final HttpHeaders headers = new HttpHeaders();

        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public OutputStream getBody() {
            return body;
        }

        private String getBodyAsString() {
            Charset charset = headers.getContentType() != null && headers.getContentType().getCharset() != null
                    ? headers.getContentType().getCharset()
                    : StandardCharsets.UTF_8;
            return new String(body.toByteArray(), charset);
        }
    }
}
