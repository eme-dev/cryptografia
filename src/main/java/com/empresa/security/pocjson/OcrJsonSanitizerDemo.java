package com.empresa.security.pocjson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OcrJsonSanitizerDemo {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        demo("ss","Apro❛bado »");
        // ─────────────────────────────────────────────────────────────────────
        // CASE 1 — Double quotes inside comment (breaks JSON without sanitizing)
        // ─────────────────────────────────────────────────────────────────────
        demo("Double quotes",
                "The client approved \"the order\" without observations.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 2 — Backslash (breaks JSON without sanitizing)
        // ─────────────────────────────────────────────────────────────────────
        demo("Backslash",
                "Path found: C:\\Users\\Admin\\report.pdf");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 3 — Multiline comment with paragraphs (real OCR output)
        // ─────────────────────────────────────────────────────────────────────
        demo("Multiline",
                "First observation from the auditor.\nSecond line of the comment.\n\nNew paragraph: see attached.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 4 — Excessive line breaks (OCR noise)
        // ─────────────────────────────────────────────────────────────────────
        demo("Excessive line breaks",
                "Line one.\n\n\n\n\nLine two after OCR noise.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 5 — Typographic quotes from Azure OCR (" " instead of " ")
        // ─────────────────────────────────────────────────────────────────────
        demo("Typographic double quotes",
                "The report states \u201Capproved\u201D by the fiscal officer.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 6 — Heavy single curved quote ❛ (U+275B — your original case)
        // ─────────────────────────────────────────────────────────────────────
        demo("Heavy single quote ❛",
                "Status: \u275Bapproved\u275C by management.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 7 — Typographic dashes (em dash, en dash)
        // ─────────────────────────────────────────────────────────────────────
        demo("Typographic dashes",
                "Period 2023\u20132024 \u2014 see annex B for details.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 8 — Non-breaking spaces (very common in PDFs)
        // ─────────────────────────────────────────────────────────────────────
        demo("Non-breaking spaces",
                "Total:\u00A0$1,500\u00A0USD\u00A0approved.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 9 — OCR ligatures (fi, fl, ff extracted as single glyph)
        // ─────────────────────────────────────────────────────────────────────
        demo("OCR ligatures",
                "Con\uFB01rmed by the \uFB01scal o\uFB03cer on the speci\uFB01ed date.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 10 — BOM + OCR replacement character
        // ─────────────────────────────────────────────────────────────────────
        demo("BOM + replacement char",
                "\uFEFFText with BOM and unreadable ch\uFFFDracter from Azure.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 11 — Null byte (breaks MySQL and C-based parsers silently)
        // ─────────────────────────────────────────────────────────────────────
        demo("Null byte",
                "Comment\u0000with embedded null byte.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 12 — Zero-width invisible characters (break string matching)
        // ─────────────────────────────────────────────────────────────────────
        demo("Zero-width characters",
                "Appro\u200Bved\u200Cby\u200Dmanagement.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 13 — Windows CRLF line endings
        // ─────────────────────────────────────────────────────────────────────
        demo("Windows CRLF",
                "Line 1\r\nLine 2\r\nLine 3\r\n");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 14 — Illegal JSON control characters (RFC 8259)
        // ─────────────────────────────────────────────────────────────────────
        demo("Control characters",
                "Text\u0001with\u0003illegal\u001Fcontrol chars.");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 15 — Null and empty input
        // ─────────────────────────────────────────────────────────────────────
        demo("Null input",  null);
        demo("Empty input", "");

        // ─────────────────────────────────────────────────────────────────────
        // CASE 16 — Full real-world OCR output combining multiple issues
        // ─────────────────────────────────────────────────────────────────────
        String realWorldOcr =
                "\uFEFF"                                          // BOM
                        + "The client \u201CJuan P\u00E9rez\u201D "      // typographic quotes
                        + "con\uFB01rmed\u00A0order N\u00BA\u00A012345"  // ligature + nbsp
                        + "\u2013A.\n\n\n\n"                             // en dash + OCR noise
                        + "Observation: \uFFFDcheck attached document.\r\n" // replacement char + CRLF
                        + "Amount: $1,500\u00A0USD \u2014 approved.";    // nbsp + em dash

        demo("Real-world OCR output", realWorldOcr);

        // ─────────────────────────────────────────────────────────────────────
        // CASE 17 — buildJsonNode: recommended for complete JSON payloads
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("CASE 17 — buildJsonNode (recommended for production)");
        System.out.println("══════════════════════════════════════════════════════");

        ObjectNode node = OcrJsonSanitizer.buildJsonNode("comment", realWorldOcr);
        node.put("documentId", "DOC-2024-001");
        node.put("processed",  true);

        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node));
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — prints cleanText() and sanitize() results side by side
    // ─────────────────────────────────────────────────────────────────────────

    private static void demo(String label, String raw) {
        System.out.println("══════════════════════════════════════════════════════");
        System.out.printf ("CASE — %s%n", label);
        System.out.println("══════════════════════════════════════════════════════");

        String display = raw != null ? raw.replace("\n", "↵").replace("\r", "←") : "null";
        System.out.println("INPUT     : " + display);

        // --- cleanText: use with Jackson ---
        String cleaned = OcrJsonSanitizer.cleanText(raw);
        System.out.println("cleanText : " + cleaned.replace("\n", "↵"));

        // --- sanitize: use for manual JSON embedding ---
        String escaped = OcrJsonSanitizer.sanitize(raw);
        System.out.println("sanitize  : " + escaped.replace("\n", "↵"));

        // --- manual JSON assembly using sanitize() ---
        String manualJson = "{\"comment\":\"" + escaped + "\"}";
        System.out.println("JSON      : " + manualJson);

        System.out.println();
    }
}