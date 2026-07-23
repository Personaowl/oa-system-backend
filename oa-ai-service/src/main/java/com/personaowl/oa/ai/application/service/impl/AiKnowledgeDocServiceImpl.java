package com.personaowl.oa.ai.application.service.impl;

import com.personaowl.oa.ai.application.service.AiKnowledgeDocService;
import com.personaowl.oa.ai.domain.entity.AiKnowledgeChunk;
import com.personaowl.oa.ai.domain.entity.AiKnowledgeDoc;
import com.personaowl.oa.ai.domain.vo.AiKnowledgeChunkVO;
import com.personaowl.oa.ai.domain.vo.AiKnowledgeDocVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import com.personaowl.oa.ai.infrastructure.mapper.AiKnowledgeChunkMapper;
import com.personaowl.oa.ai.infrastructure.mapper.AiKnowledgeDocMapper;
import com.personaowl.oa.ai.infrastructure.rag.AiDocumentParser;
import com.personaowl.oa.ai.infrastructure.rag.AiVectorStoreGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AiKnowledgeDocServiceImpl implements AiKnowledgeDocService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 80;

    private final AiKnowledgeDocMapper aiKnowledgeDocMapper;
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;
    private final AiVectorStoreGateway vectorStoreGateway;
    private final AiDocumentParser aiDocumentParser;

    public AiKnowledgeDocServiceImpl(AiKnowledgeDocMapper aiKnowledgeDocMapper,
                                     AiKnowledgeChunkMapper aiKnowledgeChunkMapper,
                                     AiVectorStoreGateway vectorStoreGateway,
                                     AiDocumentParser aiDocumentParser) {
        this.aiKnowledgeDocMapper = aiKnowledgeDocMapper;
        this.aiKnowledgeChunkMapper = aiKnowledgeChunkMapper;
        this.vectorStoreGateway = vectorStoreGateway;
        this.aiDocumentParser = aiDocumentParser;
    }

    @Override
    @Transactional
    public AiKnowledgeDocVO create(Long userId, MultipartFile file, String docTitle, String docDomain, String docVersion, LocalDate effectiveDate, String sourceType) {
        byte[] bytes;
        String fileName = null;
        try {
            bytes = file == null ? new byte[0] : file.getBytes();
            fileName = file == null ? null : file.getOriginalFilename();
        } catch (Exception ex) {
            bytes = new byte[0];
        }
        return create(userId, docTitle, docDomain, docVersion, effectiveDate, sourceType, fileName, bytes);
    }

    @Override
    @Transactional
    public AiKnowledgeDocVO create(Long userId, String docTitle, String docDomain, String docVersion, LocalDate effectiveDate, String sourceType, String fileName, byte[] fileBytes) {
        AiKnowledgeDoc entity = new AiKnowledgeDoc();
        entity.setDocTitle(docTitle);
        entity.setDocDomain(normalizeDomain(docDomain));
        entity.setDocVersion(docVersion);
        entity.setFileName(fileName);
        entity.setFileUrl(buildFileUrl(fileName));
        entity.setContentHash(sha256(fileBytes));
        entity.setStatus("DRAFT");
        entity.setSourceType(sourceType == null ? "UPLOAD" : sourceType);
        entity.setEffectiveDate(effectiveDate);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        entity.setVersion(0);
        aiKnowledgeDocMapper.insert(entity);

        parseAndPersistDocument(entity, fileBytes, fileName);
        return toVO(entity);
    }

    @Override
    public PageResultVO<AiKnowledgeDocVO> page(Integer page, Integer size, String keyword, String docDomain, String status) {
        int p = safePage(page);
        int s = safeSize(size);
        List<AiKnowledgeDoc> docs = aiKnowledgeDocMapper.selectPage(keyword, docDomain == null ? null : docDomain.toUpperCase(Locale.ROOT), status, offset(p, s), s);
        List<AiKnowledgeDocVO> list = docs.stream().map(this::toVO).toList();
        return new PageResultVO<>(list, p, s, (int) list.size());
    }

    @Override
    public AiKnowledgeDocVO get(Long id) {
        AiKnowledgeDoc entity = aiKnowledgeDocMapper.selectById(id);
        return entity == null ? AiKnowledgeDocVO.empty() : toVO(entity);
    }

    @Override
    @Transactional
    public AiKnowledgeDocVO update(Long userId, Long id, String docTitle, String docDomain, String docVersion, LocalDate effectiveDate) {
        AiKnowledgeDoc entity = aiKnowledgeDocMapper.selectById(id);
        if (entity == null) {
            return AiKnowledgeDocVO.empty();
        }
        entity.setDocTitle(docTitle);
        entity.setDocDomain(normalizeDomain(docDomain));
        entity.setDocVersion(docVersion);
        entity.setEffectiveDate(effectiveDate);
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        aiKnowledgeDocMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public AiKnowledgeDocVO approve(Long userId, Long id, String remark) {
        AiKnowledgeDoc entity = aiKnowledgeDocMapper.selectById(id);
        if (entity == null) {
            return AiKnowledgeDocVO.empty();
        }
        entity.setStatus("APPROVED");
        entity.setApprovedBy(userId);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        aiKnowledgeDocMapper.updateById(entity);
        if ("APPROVED".equals(entity.getStatus())) {
            syncVectorStore(entity);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public AiKnowledgeDocVO reindex(Long userId, Long id) {
        AiKnowledgeDoc entity = aiKnowledgeDocMapper.selectById(id);
        if (entity == null) {
            return AiKnowledgeDocVO.empty();
        }
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        aiKnowledgeDocMapper.updateById(entity);
        syncVectorStore(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public AiKnowledgeDocVO retire(Long userId, Long id) {
        AiKnowledgeDoc entity = aiKnowledgeDocMapper.selectById(id);
        if (entity == null) {
            return AiKnowledgeDocVO.empty();
        }
        entity.setStatus("RETIRED");
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        aiKnowledgeDocMapper.updateById(entity);
        vectorStoreGateway.deleteByDocId(id);
        aiKnowledgeChunkMapper.deleteByDocId(id);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        AiKnowledgeDoc entity = aiKnowledgeDocMapper.selectById(id);
        if (entity == null) {
            return;
        }
        entity.setDeleted(1);
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        aiKnowledgeDocMapper.updateById(entity);
        vectorStoreGateway.deleteByDocId(id);
        aiKnowledgeChunkMapper.deleteByDocId(id);
    }

    @Override
    public PageResultVO<AiKnowledgeChunkVO> chunks(Long docId, Integer page, Integer size) {
        int p = safePage(page);
        int s = safeSize(size);
        List<AiKnowledgeChunk> chunks = aiKnowledgeChunkMapper.selectByDocId(docId);
        List<AiKnowledgeChunkVO> vos = chunks == null ? Collections.emptyList() : chunks.stream().map(this::toVO).toList();
        return new PageResultVO<>(vos, p, s, (int) vos.size());
    }

    private void parseAndPersistDocument(AiKnowledgeDoc entity, byte[] fileBytes, String fileName) {
        String rawText = aiDocumentParser.extractText(fileBytes, fileName, entity.getDocTitle());
        List<AiDocumentParser.ChunkSection> sections = aiDocumentParser.splitByStructure(rawText, entity.getDocTitle(), CHUNK_SIZE, CHUNK_OVERLAP);
        persistChunks(entity.getId(), entity.getDocTitle(), entity.getDocVersion(), sections);
    }

    private void syncVectorStore(AiKnowledgeDoc entity) {
        List<AiKnowledgeChunk> chunks = aiKnowledgeChunkMapper.selectByDocId(entity.getId());
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        vectorStoreGateway.upsert(entity.getId(), entity.getDocTitle(), entity.getDocDomain(),
                entity.getDocVersion(), chunks.stream().map(AiKnowledgeChunk::getChunkText).toList());
    }

    private void persistChunks(Long docId, String docTitle, String docVersion, List<AiDocumentParser.ChunkSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<AiKnowledgeChunk> chunks = new ArrayList<>();
        for (AiDocumentParser.ChunkSection section : sections) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setDocId(docId);
            chunk.setChunkNo(section.chunkNo());
            chunk.setChunkTitle(section.chunkTitle() == null ? docTitle : section.chunkTitle());
            chunk.setChunkText(section.chunkText());
            chunk.setChunkHash(UUID.randomUUID().toString().replace("-", ""));
            chunk.setVectorKey("oa:knowledge:" + docId + ":" + section.chunkNo());
            chunk.setMetadataJson("{\"docId\":" + docId + ",\"docVersion\":\"" + docVersion
                    + "\",\"chunkNo\":" + section.chunkNo() + "}");
            chunk.setEmbeddingModel("BAAI/bge-m3");
            chunk.setEmbeddingDim(1024);
            chunk.setStatus("ACTIVE");
            chunk.setCreatedAt(LocalDateTime.now());
            chunks.add(chunk);
        }
        aiKnowledgeChunkMapper.insertBatch(chunks);
    }

    private List<String> splitText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replace("\r", "\n").replaceAll("\\n{2,}", "\n").trim();
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            result.add(normalized.substring(start, end));
            if (end >= normalized.length()) {
                break;
            }
            start = end - Math.min(CHUNK_OVERLAP, CHUNK_SIZE - 1);
        }
        return result;
    }

    private AiKnowledgeDocVO toVO(AiKnowledgeDoc entity) {
        List<AiKnowledgeChunk> entities = aiKnowledgeChunkMapper.selectByDocId(entity.getId());
        List<AiKnowledgeChunkVO> chunks = entities == null
                ? Collections.emptyList()
                : entities.stream().map(this::toVO).toList();
        return new AiKnowledgeDocVO(
                entity.getId(),
                entity.getDocTitle(),
                entity.getDocDomain(),
                entity.getDocVersion(),
                entity.getFileName(),
                entity.getFileUrl(),
                entity.getContentHash(),
                entity.getStatus(),
                entity.getSourceType(),
                entity.getEffectiveDate(),
                entity.getApprovedBy(),
                entity.getApprovedAt() == null ? null : entity.getApprovedAt().atOffset(java.time.ZoneOffset.ofHours(8)),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(8)),
                chunks);
    }

    private AiKnowledgeChunkVO toVO(AiKnowledgeChunk entity) {
        return new AiKnowledgeChunkVO(
                entity.getId(),
                entity.getDocId(),
                entity.getChunkNo(),
                entity.getChunkTitle(),
                entity.getChunkText(),
                entity.getStatus(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(8)));
    }

    private String normalizeDomain(String docDomain) {
        return docDomain == null ? null : docDomain.trim().toUpperCase(Locale.ROOT);
    }

    private String buildFileUrl(String fileName) {
        return fileName == null ? null : "/upload/ai/" + fileName;
    }

    private String sha256(byte[] content) {
        try {
            byte[] bytes = content == null ? new byte[0] : content;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("无法计算知识文档内容摘要", ex);
        }
    }

    private int safePage(Integer page) { return page == null || page < 1 ? 1 : page; }
    private int safeSize(Integer size) { return size == null || size < 1 ? 20 : Math.min(size, 100); }
    private int offset(int page, int size) { return (page - 1) * size; }
}
