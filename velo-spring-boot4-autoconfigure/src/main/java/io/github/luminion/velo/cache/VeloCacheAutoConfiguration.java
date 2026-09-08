package io.github.luminion.velo.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 4 缓存自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.data.couchbase.autoconfigure.DataCouchbaseAutoConfiguration",
                "org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration",
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
        },
        beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration"
)
@Import(VeloCacheConfiguration.class)
public class VeloCacheAutoConfiguration {
}
