package com.personaowl.oa.ai.application.service.impl;

import com.personaowl.oa.ai.domain.entity.AiKnowledgeChunk;
import com.personaowl.oa.ai.domain.entity.AiKnowledgeDoc;
import com.personaowl.oa.ai.domain.vo.AiKnowledgeDocVO;
import com.personaowl.oa.ai.infrastructure.mapper.AiKnowledgeChunkMapper;
import com.personaowl.oa.ai.infrastructure.mapper.AiKnowledgeDocMapper;
import com.personaowl.oa.ai.infrastructure.rag.AiDocumentParser;
import com.personaowl.oa.ai.infrastructure.rag.AiVectorStoreGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeDocServiceImplTest {

    @Mock
    private AiKnowledgeDocMapper docMapper;
    @Mock
    private AiKnowledgeChunkMapper chunkMapper;
    @Mock
    private AiVectorStoreGateway vectorStoreGateway;
    @Mock
    private AiDocumentParser parser;

    private AiKnowledgeDocServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiKnowledgeDocServiceImpl(docMapper, chunkMapper, vectorStoreGateway, parser);
    }

    @Test
    void createShouldPersistDocAndChunks() {
        MockMultipartFile file = new MockMultipartFile("file", "policy.txt", "text/plain", "第一条 总则\n第二条 请假".getBytes());
        when(parser.extractText(any(), any(), any())).thenReturn("第一条 总则\n第二条 请假");
        when(parser.splitByStructure(any(), any(), anyInt(), anyInt())).thenReturn(List.of(
                new AiDocumentParser.ChunkSection(1, "第一条 总则", "第一条 总则\n员工应遵守制度。"),
                new AiDocumentParser.ChunkSection(2, "第二条 请假", "第二条 请假\n请假需审批。")
        ));
        when(docMapper.insert(any())).thenReturn(1);
        when(chunkMapper.insertBatch(anyList())).thenReturn(1);

        AiKnowledgeDocVO vo = service.create(1L, file, "考勤制度", "ATTENDANCE", "v1.0", LocalDate.now(), "UPLOAD");

        assertNotNull(vo);
        verify(docMapper).insert(any());
        verify(chunkMapper).insertBatch(anyList());
        verify(vectorStoreGateway, never()).upsert(any(), any(), any(), any(), anyList());
    }

    @Test
    void approveShouldSyncVectorStore() {
        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setId(1L);
        doc.setDocTitle("考勤制度");
        doc.setDocDomain("ATTENDANCE");
        doc.setDocVersion("v1.0");
        doc.setStatus("DRAFT");
        doc.setCreatedAt(LocalDateTime.now());
        when(docMapper.selectById(1L)).thenReturn(doc);
        when(chunkMapper.selectByDocId(1L)).thenReturn(List.of(new AiKnowledgeChunk()));
        when(docMapper.updateById(any())).thenReturn(1);

        AiKnowledgeDocVO vo = service.approve(1L, 1L, "ok");

        assertEquals("APPROVED", vo.status());
        verify(vectorStoreGateway).upsert(any(), any(), any(), any(), anyList());
    }
}
