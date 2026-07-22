package com.personaowl.oa.ai.infrastructure.rag;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDocumentParserTest {

    private final AiDocumentParser parser = new AiDocumentParser();

    @Test
    void extractTextShouldReadUtf8TextFile() {
        byte[] bytes = "第一条 考勤制度\n第二条 请假制度".getBytes(StandardCharsets.UTF_8);
        String text = parser.extractText(bytes, "policy.txt", "fallback");

        assertTrue(text.contains("第一条"));
        assertTrue(text.contains("请假制度"));
    }

    @Test
    void splitByStructureShouldCreateChunks() {
        String raw = "第一条 总则\n员工应遵守制度。\n第二条 打卡\n员工应按时打卡。";

        List<AiDocumentParser.ChunkSection> chunks = parser.splitByStructure(raw, "考勤制度", 30, 5);

        assertFalse(chunks.isEmpty());
        assertNotNull(chunks.getFirst().chunkText());
        assertNotNull(chunks.getFirst().chunkTitle());
    }
}
