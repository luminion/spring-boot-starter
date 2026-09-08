package io.github.luminion.velo.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 2 缓存自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(
        after = {
                CouchbaseDataAutoConfiguration.class,
                HazelcastAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                RedisAutoConfiguration.class
        },
        before = CacheAutoConfiguration.class
)
@Import(VeloCacheConfiguration.class)
public class VeloCacheAutoConfiguration {
}
