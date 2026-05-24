package com.empresa.security.pocjson;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Sanitizes raw OCR text from Azure Document Intelligence
 * for safe inclusion in JSON payloads.
 *
 * <p>Usage:</p>
 * <pre>
 *   // Option A — embed value manually
 *   String json = "{\"comment\":\"" + OcrJsonSanitizer.sanitize(raw) + "\"}";
 *
 *   // Option B — let Jackson build the full JSON (recommended)
 *   ObjectNode node = OcrJsonSanitizer.buildJsonNode("comment", raw);
 *   String json = new ObjectMapper().writeValueAsString(node);
 * </pre>
 */
@Slf4j
@UtilityClass
public class OcrJsonSanitizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_LENGTH = 5_000;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Cleans OCR text without JSON escaping.
     * Use when Jackson handles the final serialization.
     */
    public String cleanText(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        String s = raw;
        s = removeArtifacts(s);
        s = removeInvalidUnicode(s);
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        s = normalizeLigatures(s);
        s = normalizeQuotes(s);
        s = normalizeDashes(s);
        s = normalizeSpaces(s);
        s = normalizeLineBreaks(s);
        s = removeControlChars(s);
        s = collapseWhitespacePerLine(s);
        s = s.trim();
        return truncateSafely(s);
    }

    /**
     * Cleans and JSON-escapes OCR text for manual string embedding.
     * The returned value does NOT include surrounding double quotes.
     */
    public String sanitize(String raw) {
        try {
            String serialized = MAPPER.writeValueAsString(cleanText(raw));
            return serialized.substring(1, serialized.length() - 1);
        } catch (Exception e) {
            log.error("OcrJsonSanitizer: JSON escaping failed", e);
            return "";
        }
    }

    /**
     * Builds a Jackson ObjectNode with the cleaned comment value.
     * Recommended for production — Jackson handles escaping internally.
     *
     * @throws IllegalArgumentException if fieldName is null or blank
     */
    public ObjectNode buildJsonNode(String fieldName, String rawComment) {
        if (fieldName == null || fieldName.isBlank())
            throw new IllegalArgumentException("fieldName must not be null or blank.");

        ObjectNode node = MAPPER.createObjectNode();
        node.put(fieldName, cleanText(rawComment));
        return node;
    }

    // -------------------------------------------------------------------------
    // Pipeline steps (private)
    // -------------------------------------------------------------------------

    /** Removes BOM, OCR replacement chars, null bytes and legacy control chars. */
    private String removeArtifacts(String s) {
        return s
                .replace("\uFEFF", "")   // BOM
                .replace("\uFFFD", "")   // OCR replacement character
                .replace("\u0000", "")   // null byte — breaks MySQL and C parsers
                .replace("\u001A", "")   // DOS EOF (Ctrl+Z)
                .replace("\u001B", "")   // ESC — ANSI sequences in PDF metadata
                .replace("\u00AD", "");  // soft hyphen — invisible, breaks string comparison
    }

    /** Removes lone surrogates that cause Jackson to throw JsonProcessingException. */
    private String removeInvalidUnicode(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1)))
                    sb.append(c).append(s.charAt(++i)); // valid surrogate pair — keep
                // else: lone high surrogate — discard
            } else if (!Character.isLowSurrogate(c)) {
                sb.append(c); // normal character — keep
            }
            // else: lone low surrogate — discard
        }
        return sb.toString();
    }

    /** Expands typographic ligatures that OCR extracts as single glyphs. */
    private String normalizeLigatures(String s) {
        return s
                .replace("\uFB00", "ff").replace("\uFB01", "fi").replace("\uFB02", "fl")
                .replace("\uFB03", "ffi").replace("\uFB04", "ffl")
                .replace("\uFB05", "st").replace("\uFB06", "st");
    }

    /** Normalizes typographic quotes to their ASCII equivalents. */
    private String normalizeQuotes(String s) {
        return s
                // Double quotes → "
                .replace("\u201C", "\"").replace("\u201D", "\"")  // " "
                .replace("\u00AB", "\"").replace("\u00BB", "\"")  // « »
                .replace("\u275D", "\"").replace("\u275E", "\"")  // ❝ ❞
                .replace("\u201E", "\"")                          // „
                // Single quotes → '
                .replace("\u2018", "'").replace("\u2019", "'")    // ' '
                .replace("\u201A", "'")                           // ‚
                .replace("\u2039", "'").replace("\u203A", "'")    // ‹ ›
                .replace("\u275B", "'").replace("\u275C", "'");   // ❛ ❜
    }

    /** Normalizes typographic dashes to ASCII hyphen. */
    private String normalizeDashes(String s) {
        return s
                .replace("\u2014", "-").replace("\u2013", "-")    // — –
                .replace("\u2010", "-").replace("\u2011", "-")    // ‐ ‑
                .replace("\u2012", "-").replace("\u2015", "-")    // ‒ ―
                .replace("\uFE58", "-").replace("\uFE63", "-")    // ﹘ ﹣
                .replace("\uFF0D", "-");                          // －
    }

    /** Normalizes non-standard spaces and removes zero-width characters. */
    private String normalizeSpaces(String s) {
        return s
                // Non-standard spaces → single ASCII space
                .replace("\u00A0", " ").replace("\u2002", " ").replace("\u2003", " ")
                .replace("\u2004", " ").replace("\u2005", " ").replace("\u2006", " ")
                .replace("\u2007", " ").replace("\u2008", " ").replace("\u2009", " ")
                .replace("\u200A", " ").replace("\u3000", " ")
                // Zero-width characters → remove (invisible, break string matching)
                .replace("\u200B", "").replace("\u200C", "").replace("\u200D", "")
                .replace("\u2060", "").replace("\uFFA0", "");
    }

    /**
     * Unifies all line-break styles to \n and collapses
     * more than 2 consecutive breaks (OCR noise) into exactly 2.
     */
    private String normalizeLineBreaks(String s) {
        return s
                .replace("\r\n", "\n").replace("\r", "\n")  // Windows / classic Mac
                .replace("\u0085", "\n")                    // NEL — common in PDFs
                .replace("\u2028", "\n")                    // Unicode Line Separator
                .replace("\u2029", "\n")                    // Unicode Paragraph Separator
                .replaceAll("\n{3,}", "\n\n");              // max 2 consecutive breaks
    }

    /** Removes control characters illegal in JSON strings (RFC 8259), keeps \n and \t. */
    private String removeControlChars(String s) {
        return s.chars()
                .filter(c -> c >= 0x20 || c == '\n' || c == '\t')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /** Collapses multiple spaces/tabs per line without touching line breaks. */
    private String collapseWhitespacePerLine(String s) {
        // split with -1 preserves trailing empty strings (blank last lines)
        return Arrays.stream(s.split("\n", -1))
                .map(line -> line.replaceAll("[ \\t]{2,}", " ").trim())
                .collect(Collectors.joining("\n"));
    }

    /** Truncates at word boundary to avoid cutting surrogate pairs. */
    private String truncateSafely(String s) {
        if (s.length() <= MAX_LENGTH) return s;

        int lastSpace = s.lastIndexOf(' ', MAX_LENGTH);
        int end = (lastSpace >= MAX_LENGTH / 2) ? lastSpace : MAX_LENGTH;
        if (Character.isHighSurrogate(s.charAt(end - 1))) end--;

        String truncated = s.substring(0, end).trim();
        log.warn("OcrJsonSanitizer: text truncated from {} to {} chars", s.length(), truncated.length());
        return truncated;
    }
}
