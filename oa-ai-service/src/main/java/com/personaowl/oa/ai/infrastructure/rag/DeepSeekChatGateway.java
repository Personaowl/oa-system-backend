package com.personaowl.oa.ai.infrastructure.rag;

public interface DeepSeekChatGateway {

    String generate(String systemPrompt, String userPrompt);
}
