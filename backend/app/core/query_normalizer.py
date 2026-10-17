"""
Query Normalization Utility for EduNova.

Provides locale-independent, canonical normalization for ordinary natural-language queries:
1. Normalizes Unicode to NFC.
2. Trims leading and trailing whitespace.
3. Collapses consecutive whitespace (spaces, tabs, newlines) into a single space.
4. Converts text to lowercase using locale-independent casefold().

Structured input (code blocks, file paths, URLs, LaTeX, programming constructs)
is preserved to protect syntax, indentation, and case sensitivity.
"""

import re
import unicodedata

_WHITESPACE_COLLAPSE_RE = re.compile(r"\s+")

_CODE_INDICATORS = (
    "def ", "class ", "import ", "from ", "return ",
    "function ", "const ", "let ", "var ", "console.log",
    "public class ", "public static void ", "System.out.",
    "SELECT ", "INSERT INTO ", "UPDATE ", "DELETE FROM ", "CREATE TABLE ",
    "#include <", "using namespace ", "int main(",
)

_PATH_EXTENSIONS = (
    ".py", ".kt", ".java", ".cpp", ".c", ".h", ".js", ".ts",
    ".html", ".css", ".json", ".xml", ".sql", ".sh",
)

_LATEX_COMMANDS = (
    "\\frac", "\\sqrt", "\\int", "\\sum", "\\alpha",
    "\\beta", "\\theta", "\\partial", "\\begin", "\\end",
)


def is_structured_input(text: str) -> bool:
    """
    Detects whether the input text is structured code, a file path, URL,
    or mathematical/LaTeX formula where case and whitespace must be preserved.
    """
    if not text:
        return False

    # 1. Markdown code blocks
    if "```" in text:
        return True

    stripped = text.strip()

    # 2. Programming constructs (at start of text or at start of any line)
    if any(stripped.startswith(ind) for ind in _CODE_INDICATORS):
        return True
    if any(line.strip().startswith(ind) for line in text.splitlines() for ind in _CODE_INDICATORS):
        return True

    # 3. File paths and URLs
    if stripped.startswith(("http://", "https://", "/", "file://", "C:\\", "c:\\")):
        return True
    if " " not in stripped and any(stripped.endswith(ext) for ext in _PATH_EXTENSIONS):
        return True

    # 4. LaTeX / Mathematical formulas
    if "\\" in text and any(cmd in text for cmd in _LATEX_COMMANDS):
        return True

    return False


def normalize_query(text: str) -> str:
    """
    Normalizes an ordinary natural-language query to its canonical form.
    
    If the text is structured input (code, math, paths), Unicode NFC normalization
    and whitespace trimming are performed while preserving internal whitespace and casing.
    """
    if not text:
        return ""

    # 1. Normalize Unicode to NFC
    nfc_text = unicodedata.normalize("NFC", text)

    # If structured, preserve case and internal formatting
    if is_structured_input(nfc_text):
        return nfc_text.strip()

    # 2. Trim leading and trailing whitespace
    trimmed = nfc_text.strip()

    # 3. Collapse consecutive whitespace characters (spaces, tabs, newlines) into one space
    collapsed = _WHITESPACE_COLLAPSE_RE.sub(" ", trimmed)

    # 4. Convert text to lowercase using locale-independent casefold()
    return collapsed.casefold()
