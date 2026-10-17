package com.offline_First.data.local

import java.text.Normalizer
import java.util.Locale

/**
 * Locale-independent query normalizer for EduNova:
 * 1. Normalizes Unicode to NFC.
 * 2. Trims leading and trailing whitespace.
 * 3. Collapses consecutive whitespace (spaces, tabs, newlines) into a single space.
 * 4. Converts text to lowercase using locale-independent Locale.ROOT.
 *
 * Preserves structured input (code blocks, file paths, URLs, LaTeX, programming constructs)
 * to avoid altering case-sensitive syntax, indentation, and expressions.
 */
object QueryNormalizer {

    private val WHITESPACE_REGEX = Regex("\\s+")

    private val CODE_INDICATORS = listOf(
        "def ", "class ", "import ", "from ", "return ",
        "function ", "const ", "let ", "var ", "console.log",
        "public class ", "public static void ", "System.out.",
        "SELECT ", "INSERT INTO ", "UPDATE ", "DELETE FROM ", "CREATE TABLE ",
        "#include <", "using namespace ", "int main("
    )

    private val PATH_EXTENSIONS = listOf(
        ".py", ".kt", ".java", ".cpp", ".c", ".h", ".js", ".ts",
        ".html", ".css", ".json", ".xml", ".sql", ".sh"
    )

    private val LATEX_COMMANDS = listOf(
        "\\frac", "\\sqrt", "\\int", "\\sum", "\\alpha",
        "\\beta", "\\theta", "\\partial", "\\begin", "\\end"
    )

    fun isStructuredInput(text: String): Boolean {
        if (text.isBlank()) return false

        // 1. Markdown code blocks
        if (text.contains("```")) return true

        val stripped = text.trim()

        // 2. Programming constructs (at start of string or at start of any line)
        if (CODE_INDICATORS.any { stripped.startsWith(it) }) return true
        if (text.lines().any { line -> CODE_INDICATORS.any { line.trim().startsWith(it) } }) return true

        // 3. File paths and URLs
        if (stripped.startsWith("http://") || stripped.startsWith("https://") ||
            stripped.startsWith("/") || stripped.startsWith("file://") ||
            stripped.startsWith("C:\\", ignoreCase = true) || stripped.startsWith("c:\\", ignoreCase = true)) {
            return true
        }
        if (!stripped.contains(" ") && PATH_EXTENSIONS.any { stripped.endsWith(it) }) {
            return true
        }

        // 4. LaTeX / Mathematical formulas
        if (text.contains("\\") && LATEX_COMMANDS.any { text.contains(it) }) {
            return true
        }

        return false
    }

    /**
     * Normalizes an ordinary natural-language query to its canonical form.
     */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""

        val nfc = Normalizer.normalize(text, Normalizer.Form.NFC)

        if (isStructuredInput(nfc)) {
            return nfc.trim()
        }

        // 1. Trim leading and trailing whitespace
        val trimmed = nfc.trim()
        // 2. Collapse consecutive whitespace characters (spaces, tabs, newlines) into one space
        val collapsed = trimmed.replace(WHITESPACE_REGEX, " ")
        // 3. Convert text to lowercase using locale-independent Locale.ROOT
        return collapsed.lowercase(Locale.ROOT)
    }
}
