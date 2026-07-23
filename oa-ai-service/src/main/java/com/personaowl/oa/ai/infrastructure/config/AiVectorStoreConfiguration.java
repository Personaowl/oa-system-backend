package com.personaowl.oa.ai.infrastructure.config;

import com.personaowl.oa.ai.infrastructure.rag.AiRagProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class AiVectorStoreConfiguration {

    @Bean
    public VectorStore aiRedisVectorStore(JedisConnectionFactory connectionFactory,
                                          EmbeddingModel embeddingModel,
                                          AiRagProperties properties) {
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(connectionFactory.isUseSsl())
                .clientName(connectionFactory.getClientName())
                .timeoutMillis(connectionFactory.getTimeout())
                .password(connectionFactory.getPassword())
                .build();
        JedisPooled jedis = new JedisPooled(
                new HostAndPort(connectionFactory.getHostName(), connectionFactory.getPort()),
                clientConfig);
        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName(properties.indexName())
                .prefix(properties.knowledgePrefix())
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("docId"),
                        RedisVectorStore.MetadataField.tag("docDomain"))
                .initializeSchema(true)
                .build();
    }
}
