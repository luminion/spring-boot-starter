package io.github.luminion.velo.jackson;

import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializerBase;

/** 仅对测试标记类型生效，用于验证 Boot 的模块发现开关。 */
public class ServiceLoaderModuleFixture extends SimpleModule {

    public ServiceLoaderModuleFixture() {
        super("velo-test-service-loader");
        addSerializer(DiscoveryPayload.class, new ToStringSerializerBase(DiscoveryPayload.class) {
            @Override
            public String valueToString(Object value) {
                return "discovered";
            }
        });
    }

    /** 用于验证是否加载测试模块的简单负载。 */
    public static class DiscoveryPayload {

        /** 未发现测试模块时保留的默认值。 */
        public String value = "native";
    }
}
