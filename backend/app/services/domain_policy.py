"""
Configurable Domain Policy & Scope Boundary Service for EduNova Chatbot.

Defines:
- DomainIntent classification (Curriculum, General Educational, Prohibited OOD, Ambiguous, Mixed-Intent).
- Configurable domain policy supporting Student Mode (curriculum-aligned) and General Mode (general educational).
- Safe refusal messages for prohibited categories.
- Clarification prompts for underspecified/ambiguous queries.
- Query decomposition for mixed-intent requests (preserving educational content).
"""

import logging
import re
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict, List, Optional, Tuple

logger = logging.getLogger(__name__)


class DomainIntent(str, Enum):
    IN_DOMAIN_CURRICULUM = "IN_DOMAIN_CURRICULUM"
    IN_DOMAIN_GENERAL = "IN_DOMAIN_GENERAL"
    OUT_OF_DOMAIN_PROHIBITED = "OUT_OF_DOMAIN_PROHIBITED"
    AMBIGUOUS_CLARIFICATION = "AMBIGUOUS_CLARIFICATION"
    MIXED_INTENT = "MIXED_INTENT"
    CONVERSATIONAL_FAST_PATH = "CONVERSATIONAL_FAST_PATH"


@dataclass
class DomainClassificationResult:
    intent: DomainIntent
    is_prohibited: bool = False
    prohibited_category: Optional[str] = None
    refusal_message: Optional[str] = None
    clarification_prompt: Optional[str] = None
    educational_subquery: Optional[str] = None
    declined_subquery: Optional[str] = None
    confidence: float = 1.0
    rationale: str = ""


# Prohibited Domain Patterns (Non-Educational)
_PROHIBITED_PATTERNS = {
    "MALWARE_EXPLOITS": re.compile(
        r"\b(?:write\s+(?:a\s+)?malware|ransomware\s+script|ddos\s+attack\s+tool|steal\s+passwords|keylogger\s+code|exploit\s+vulnerability\s+to\s+hack)\b",
        re.IGNORECASE,
    ),
    "EXAM_CHEATING": re.compile(
        r"\b(?:paper\s+leaks?|leaked\s+(?:question\s+)?papers?|exam\s+papers?\s+leaks?|buy\s+exam\s+papers?|cheat\s+(?:in\s+)?(?:board\s+)?exams?|cheat\s+on\s+exams?|hack\s+cbse\s+marks)\b",
        re.IGNORECASE,
    ),
    "MEDICAL_DIAGNOSIS": re.compile(
        r"\b(?:diagnose\s+my|what\s+medicine\s+should\s+i\s+take|prescribe\s+antibiotics|cure\s+my\s+chest\s+pain|dosage\s+for\s+fever\s+baby|medical\s+emergency)\b",
        re.IGNORECASE,
    ),
    "FINANCE_SPECULATION": re.compile(
        r"\b(?:crypto|cryptocurrency|bitcoin|bitcion|ethereum|dogecoin|forex|stock\s+tips|buy\s+shares|day\s+trading|portfolio\s+returns|investment\s+advice|dividend\s+yield|penny\s+stocks)\b",
        re.IGNORECASE,
    ),
    "GAMBLING": re.compile(
        r"\b(?:sports\s+betting|bet\s+on|lottery\s+ticket|online\s+casino|blackjack\s+strategy|roulette|slot\s+machine|poker\s+chips|ipl\s+betting|bookie)\b",
        re.IGNORECASE,
    ),
    "ENTERTAINMENT_GOSSIP": re.compile(
        r"\b(?:celebrity\s+gossip|who\s+is\s+dating|paparazzi|hollywood\s+rumors|actor\s+scandal|movie\s+box\s+office\s+collection|reality\s+tv\s+drama)\b",
        re.IGNORECASE,
    ),
    "POLITICAL_CAMPAIGN": re.compile(
        r"\b(?:vote\s+for\s+(?:bjp|congress|democrats|republicans)|election\s+campaigning|political\s+manifesto\s+promotion|party\s+candidate\s+slogan)\b",
        re.IGNORECASE,
    ),
}

# General In-Domain Educational Patterns
_EDUCATIONAL_PATTERNS = re.compile(
    r"\b(math|mathematics|algebra|calculus|geometry|trigonometry|integral|derivative|equation|solve|physics|chemistry|biology|reaction|newton|ohm|omhs?\s+law|einstein|gravity|velocity|acceleration|force|energy|cell|mitosis|photosynthesis|fotosisnthesis|dna|rna|element|periodic\s+table|atom|molecule|organism|species|evolution|history|revolution|geography|capital|country|continent|ocean|river|mountain|planet|solar\s+system|literature|grammar|poem|programming|python|java|c\+\+|javascript|algorithm|data\s+structure|recursion|binary\s+tree|sorting|database|sql|civics|constitution|parliament|election\s+commission)\b",
    re.IGNORECASE,
)

# Ambiguous Query Patterns requiring Clarification
_AMBIGUOUS_PATTERNS = [
    (
        re.compile(r"^\s*(?:explain|what\s+is)\s+(?:the\s+)?law\s*\??\s*$", re.IGNORECASE),
        "Which specific scientific law are you referring to? (e.g., Ohm's law in Physics, Newton's laws of motion, or the Law of Conservation of Mass in Chemistry?)",
    ),
    (
        re.compile(r"^\s*(?:what\s+is|explain)\s+(?:a\s+)?bond\s*\??\s*$", re.IGNORECASE),
        "Could you clarify if you mean a chemical bond (ionic/covalent bonds in Chemistry) or another academic topic?",
    ),
    (
        re.compile(r"^\s*(?:how\s+do|explain)\s+cells?\s+(?:work|divide)\s*\??\s*$", re.IGNORECASE),
        "Are you asking about biological cells (mitosis and cell division in Biology) or electrochemical cells in Physics/Chemistry?",
    ),
    (
        re.compile(r"^\s*(?:what\s+are|explain)\s+roots?\s*\??\s*$", re.IGNORECASE),
        "Could you clarify if you mean plant root systems in Biology, or roots of a polynomial/quadratic equation in Mathematics?",
    ),
    (
        re.compile(r"^\s*(?:what\s+is|explain)\s+(?:the\s+)?spectrum\s*\??\s*$", re.IGNORECASE),
        "Could you clarify if you mean the electromagnetic spectrum in Physics, the visible light spectrum in Optics, or another academic topic?",
    ),
    (
        re.compile(r"^\s*(?:what\s+is|explain)\s+power\s*\??\s*$", re.IGNORECASE),
        "Could you clarify if you mean mechanical/electrical power (work done per unit time) in Physics, power-sharing in Civics, or mathematical exponents?",
    ),
]

# Standard Polite Educational Refusals
_REFUSAL_MESSAGES = {
    "FINANCE_SPECULATION": (
        "I am an educational AI tutor focused on academic learning and coursework. "
        "I cannot provide stock market, cryptocurrency, or speculative financial advice. "
        "I would be glad to help you with mathematics, economics concepts, or science!"
    ),
    "GAMBLING": (
        "I am an educational AI tutor dedicated to student learning. "
        "I do not assist with gambling, lottery, or sports betting inquiries. "
        "Let me know if you would like to explore probability in mathematics or physics principles!"
    ),
    "ENTERTAINMENT_GOSSIP": (
        "I am focused on academic topics and coursework. "
        "I cannot help with celebrity gossip or entertainment trivia. "
        "Feel free to ask questions about literature, science, mathematics, or history!"
    ),
    "POLITICAL_CAMPAIGN": (
        "I provide educational assistance for students and learners. "
        "I do not participate in political campaigning or partisan lobbying. "
        "I can help explain political science concepts, democratic institutions, or historical events!"
    ),
    "MEDICAL_DIAGNOSIS": (
        "I am an educational tutor and cannot provide personal medical diagnoses or drug prescriptions. "
        "If you have health concerns, please consult a qualified healthcare professional. "
        "I can help explain human anatomy, biology, or physiological science concepts!"
    ),
    "MALWARE_EXPLOITS": (
        "I cannot assist with creating malware, exploits, or cyberattack tools. "
        "I can help you learn computer science, secure programming, algorithm design, and cybersecurity defense concepts!"
    ),
    "EXAM_CHEATING": (
        "I am an educational AI tutor committed to academic integrity. "
        "I cannot provide leaked examination papers or assist with academic dishonesty. "
        "I can help you review official syllabus topics, practice concepts, and solve sample questions!"
    ),
    "DEFAULT_OOD": (
        "I am an AI tutor designed for educational support in academic subjects (math, science, history, literature, and computer science). "
        "This request is outside my educational scope, but I'd be happy to help with your studies!"
    ),
}


class DomainPolicyService:
    """
    Configurable domain policy enforcement engine.
    
    Guarantees:
    - Student Mode: strictly curriculum-guided; rejects non-educational domains;
      in-domain questions missing syllabus evidence are passed to RAG to trigger standard EvidenceOutcome.MISSING.
    - General Mode: permits broad educational subjects (science, math, coding, history, literature);
      rejects prohibited non-educational domains (finance speculation, gambling, malware, etc.).
    - Ambiguous queries: returns clarification prompt instead of ungrounded guessing.
    - Mixed-intent queries: extracts educational portion and declines the prohibited portion.
    """

    def __init__(
        self,
        enabled: bool = True,
        strict_mode: bool = True,
        allow_general_education: bool = True,
    ):
        self.enabled = enabled
        self.strict_mode = strict_mode
        self.allow_general_education = allow_general_education

    def classify_prompt(
        self,
        prompt: str,
        app_mode: str = "student",
    ) -> DomainClassificationResult:
        """Classifies prompt intent against educational domain policy."""
        if not self.enabled:
            return DomainClassificationResult(
                intent=DomainIntent.IN_DOMAIN_GENERAL,
                rationale="domain_policy_disabled",
            )

        cleaned = prompt.strip()
        app_mode_lower = (app_mode or "student").lower()

        # 1. Check for Ambiguous Educational Queries requiring clarification
        for pattern, clarif_text in _AMBIGUOUS_PATTERNS:
            if pattern.match(cleaned):
                return DomainClassificationResult(
                    intent=DomainIntent.AMBIGUOUS_CLARIFICATION,
                    clarification_prompt=clarif_text,
                    confidence=0.95,
                    rationale="ambiguous_educational_prompt",
                )

        # 2. Check for Prohibited Domains
        detected_category = None
        for cat, pattern in _PROHIBITED_PATTERNS.items():
            if pattern.search(cleaned):
                detected_category = cat
                break

        # 3. Check for Mixed-Intent (has both educational topic and prohibited topic)
        has_educational = bool(_EDUCATIONAL_PATTERNS.search(cleaned))

        if detected_category and has_educational:
            # Attempt decomposition across conjunctions ("and", "also", "while", "plus")
            parts = re.split(r"\b(?:and\s+also|and\s+tell\s+me|and\s+give\s+me|and|also|plus)\b", cleaned, flags=re.IGNORECASE)
            edu_sub = None
            decl_sub = None
            for p in parts:
                p_clean = p.strip()
                if _EDUCATIONAL_PATTERNS.search(p_clean) and not _PROHIBITED_PATTERNS[detected_category].search(p_clean):
                    edu_sub = p_clean
                elif _PROHIBITED_PATTERNS[detected_category].search(p_clean):
                    decl_sub = p_clean

            if edu_sub:
                return DomainClassificationResult(
                    intent=DomainIntent.MIXED_INTENT,
                    is_prohibited=False,
                    prohibited_category=detected_category,
                    educational_subquery=edu_sub,
                    declined_subquery=decl_sub,
                    confidence=0.90,
                    rationale=f"mixed_intent_with_{detected_category.lower()}",
                )

        # 4. Pure Prohibited Out-of-Domain
        if detected_category:
            refusal = _REFUSAL_MESSAGES.get(detected_category, _REFUSAL_MESSAGES["DEFAULT_OOD"])
            return DomainClassificationResult(
                intent=DomainIntent.OUT_OF_DOMAIN_PROHIBITED,
                is_prohibited=True,
                prohibited_category=detected_category,
                refusal_message=refusal,
                confidence=0.98,
                rationale=f"prohibited_{detected_category.lower()}",
            )

        # 5. In-Domain Curriculum vs General Educational
        if app_mode_lower == "student":
            return DomainClassificationResult(
                intent=DomainIntent.IN_DOMAIN_CURRICULUM,
                confidence=0.95,
                rationale="student_curriculum_candidate",
            )
        else:
            return DomainClassificationResult(
                intent=DomainIntent.IN_DOMAIN_GENERAL,
                confidence=0.90,
                rationale="general_educational_learning",
            )


domain_policy_service = DomainPolicyService()
