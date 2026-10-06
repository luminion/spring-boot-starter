package io.github.luminion.velo.jackson.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class JsonEnumMetadataResolverTests {
    private final JsonEnumMetadataResolver resolver =
            new JsonEnumMetadataResolver(new VeloProperties.JacksonProperties());

    @Test
    void sameNamedEnumsFromDifferentClassLoadersKeepTheirOwnMappings() throws Exception {
        byte[] bytecode = fixtureBytecode();
        Class<? extends Enum<?>> first = isolatedFixture(bytecode, "loader-A");
        Class<? extends Enum<?>> second = isolatedFixture(bytecode, "loader-B");

        assertThat(first.getName()).isEqualTo(second.getName());
        assertThat(first).isNotSameAs(second);
        assertThat(resolver.resolve(first, "code", "name", Integer.class).getName(1))
                .isEqualTo("loader-A");
        assertThat(resolver.resolve(second, "code", "name", Integer.class).getName(1))
                .isEqualTo("loader-B");
        assertThat(resolver.resolve(first, "code", "name", Integer.class).getName(1))
                .isEqualTo("loader-A");
    }

    @Test
    void differentFieldPairsOnTheSameEnumKeepTheirOwnMappings() {
        JsonEnumMetadata name = resolver.resolve(Fixture.class, "code", "name", Integer.class);
        JsonEnumMetadata alternate =
                resolver.resolve(Fixture.class, "code", "alternateName", int.class);

        assertThat(name.getName(1)).isEqualTo("name");
        assertThat(alternate.getName(1)).isEqualTo("alternate");
    }

    private byte[] fixtureBytecode() throws IOException {
        String resource = "/" + Fixture.class.getName().replace('.', '/') + ".class";
        try (InputStream input = Fixture.class.getResourceAsStream(resource);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) {
                throw new IOException("Missing enum fixture: " + resource);
            }
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Enum<?>> isolatedFixture(byte[] bytecode, String name) throws Exception {
        String className = Fixture.class.getName();
        ClassLoader loader = new ClassLoader(Fixture.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                if (!className.equals(requested)) {
                    return super.loadClass(requested, resolve);
                }
                synchronized (getClassLoadingLock(requested)) {
                    Class<?> loaded = findLoadedClass(requested);
                    if (loaded == null) {
                        loaded = defineClass(requested, bytecode, 0, bytecode.length);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
        };
        Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) loader.loadClass(className);
        Field field = type.getDeclaredField("name");
        field.setAccessible(true);
        field.set(type.getEnumConstants()[0], name);
        return type;
    }

    public enum Fixture {
        ONE;

        private final int code = 1;
        private String name = "name";
        private final String alternateName = "alternate";
    }
}
