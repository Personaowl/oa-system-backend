package com.personaowl.oa.ai.infrastructure.config;

import com.personaowl.oa.ai.infrastructure.rag.AiRagProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiRagProperties.class)
public class AiRagConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenAiApi openAiApi(AiRagProperties properties) {
        String baseUrl = properties.baseUrl();
        if (baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
        }
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(properties.apiKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi, AiRagProperties properties) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(properties.chatModel()).temperature(0.2).build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi, AiRagProperties properties) {
        return new OpenAiEmbeddingModel(openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(properties.embeddingModel())
                        .build());
    }

}
