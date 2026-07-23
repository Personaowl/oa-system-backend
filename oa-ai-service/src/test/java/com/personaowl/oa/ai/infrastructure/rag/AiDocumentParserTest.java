package com.personaowl.oa.ai.infrastructure.rag;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void splitByStructureShouldRecognizeMarkdownHeadings() {
        String raw = "# OA 考勤制度\n简介\n## 1. 打卡规则\n员工应按时打卡。\n## 2. 补卡规则\n缺卡后提交补卡申请。";

        List<AiDocumentParser.ChunkSection> chunks = parser.splitByStructure(raw, "考勤制度", 500, 80);

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(1).chunkTitle().contains("打卡规则"));
    }

    @Test
    void splitByStructureShouldKeepConfiguredOverlap() {
        String raw = "abcdefghijklmnopqrstuvwxyz";

        List<AiDocumentParser.ChunkSection> chunks = parser.splitByStructure(raw, "测试", 10, 3);

        assertTrue(chunks.size() > 1);
        assertEquals("hij", chunks.get(1).chunkText().substring(0, 3));
    }
}
