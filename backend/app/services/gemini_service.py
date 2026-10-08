"""
Backwards-compatibility shim for the legacy Gemini service.
All AI operations have migrated to OpenRouter (openrouter_service).
"""
from app.services.openrouter_service import (
    OpenRouterService,
    openrouter_service,
    _SYSTEM_INSTRUCTIONS,
    _FALLBACK_MODELS,
    _normalize_model_name,
)

# Compatibility aliases
GeminiService = OpenRouterService
gemini_service = openrouter_service
