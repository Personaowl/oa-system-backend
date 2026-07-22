package com.personaowl.oa.ai.infrastructure.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiRagServiceImplTest {

    @Test
    void answerShouldReturnFallbackWhenNoDocs() {
        VectorStore vectorStore = mock(VectorStore.class);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));
        AiRagProperties properties = new AiRagProperties(3, 0.75, 600, 80, "oa:knowledge:", "oa-knowledge-index", "deepseek-chat", "BAAI/bge-m3", "https://api.siliconflow.cn/v1", "test", "localhost", 6379);
        AiRagServiceImpl service = new AiRagServiceImpl(vectorStore, chatClientBuilder, properties);

        AiRagService.RagResult result = service.answer("迟到多久算迟到", "ATTENDANCE", 3);

        assertNotNull(result);
        assertFalse(result.hitFlag());
    }
}
