package com.personaowl.oa.ai.infrastructure.rag;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiDocumentParser {

    private static final Pattern SECTION_PATTERN = Pattern.compile("(?m)^(第[一二三四五六七八九十百千0-9]+[条章节].*|\\d+(?:\\.\\d+)*\\s+.*|[一二三四五六七八九十]+、.*)$");
    private final Tika tika = new Tika();

    public String extractText(byte[] bytes, String fileName, String fallbackTitle) {
        if (bytes == null || bytes.length == 0) {
            return fallbackTitle == null ? "" : fallbackTitle;
        }
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") || lower.endsWith(".log")) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return tika.parseToString(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception ex) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public List<ChunkSection> splitByStructure(String rawText, String defaultTitle, int chunkSize, int overlap) {
        String text = normalize(rawText);
        if (text.isBlank()) {
            return List.of();
        }

        List<String> sections = extractSections(text);
        if (sections.isEmpty()) {
            sections = List.of(text);
        }

        List<ChunkSection> chunks = new ArrayList<>();
        int seq = 1;
        for (String section : sections) {
            List<String> pieces = splitLargeText(section, chunkSize, overlap);
            for (String piece : pieces) {
                String title = inferTitle(piece, defaultTitle);
                chunks.add(new ChunkSection(seq++, title, piece));
            }
        }
        return chunks;
    }

    private List<String> extractSections(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = SECTION_PATTERN.matcher(text);
        List<Integer> positions = new ArrayList<>();
        while (matcher.find()) {
            positions.add(matcher.start());
        }
        if (positions.isEmpty()) {
            return result;
        }
        for (int i = 0; i < positions.size(); i++) {
            int start = positions.get(i);
            int end = i + 1 < positions.size() ? positions.get(i + 1) : text.length();
            String section = text.substring(start, end).trim();
            if (!section.isBlank()) {
                result.add(section);
            }
        }
        return result;
    }

    private List<String> splitLargeText(String text, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        if (text.length() <= chunkSize) {
            result.add(text.trim());
            return result;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String piece = text.substring(start, end).trim();
            if (!piece.isBlank()) {
                result.add(piece);
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, end);
        }
        return result;
    }

    private String inferTitle(String text, String defaultTitle) {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isBlank() && trimmed.length() <= 40) {
                return trimmed;
            }
        }
        return defaultTitle;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\u0000", "")
                .replace("\r", "\n")
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
        return normalized;
    }

    public record ChunkSection(int chunkNo, String chunkTitle, String chunkText) {
    }
}
