package com.sqleditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration.class,
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration.class
})
@EnableScheduling
public class SqlEditorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SqlEditorApplication.class, args);
    }
}
