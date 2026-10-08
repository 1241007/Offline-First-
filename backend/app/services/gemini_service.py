import asyncio
import logging
from typing import Optional, AsyncIterator
import google.generativeai as genai
from app.core.config import settings

logger = logging.getLogger(__name__)

_SYSTEM_INSTRUCTIONS = {
    "teacher": (
        "You are a patient, encouraging teacher for students. "
        "Explain concepts step-by-step with real-world examples. "
        "Use simple language. Add practice questions at the end. "
        "Praise effort and guide understanding. Be supportive and positive."
    ),
    "general": (
        "You are a concise, helpful learning assistant. "
        "Answer questions directly and clearly. "
        "Be informative but not verbose."
    ),
    "explainable": (
        "You are an analytical tutor. Explain your reasoning thoroughly. "
        "Make assumptions explicit. Use structured explanations. "
        "Cover edge cases and trade-offs. Explain the 'why' behind concepts."
    ),
}

_FALLBACK_MODELS = [
    "gemini-3.5-flash",
    "gemini-3.6-flash",
    "gemini-3.7-flash",
    "gemini-3-flash-preview",
]


def _normalize_history(history: list[dict]) -> tuple[list[dict], str]:
    """
    Validates and normalizes conversation history for Google Gemini.
    Gemini requires:
    - History must start with a 'user' turn.
    - Turns must alternate strictly between 'user' and 'model'.
    - The last turn in prior_history must be 'model' so that sending current_message
      creates the alternating 'user' turn.
    Returns (sanitized_prior_history, current_user_message).
    """
    if not history:
        return [], ""

    current_message = history[-1]["parts"][0] if history[-1].get("parts") else ""
    raw_prior = history[:-1]

    if not raw_prior:
        return [], current_message

    # 1. Filter out turns with empty parts
    valid_turns = []
    for turn in raw_prior:
        parts = [p for p in turn.get("parts", []) if p and str(p).strip()]
        if parts:
            valid_turns.append({"role": turn["role"], "parts": parts})

    if not valid_turns:
        return [], current_message

    # 2. Ensure history starts with 'user'
    while valid_turns and valid_turns[0]["role"] != "user":
        valid_turns.pop(0)

    if not valid_turns:
        return [], current_message

    # 3. Collapse consecutive turns of the same role
    collapsed = []
    for turn in valid_turns:
        if collapsed and collapsed[-1]["role"] == turn["role"]:
            collapsed[-1]["parts"].extend(turn["parts"])
        else:
            collapsed.append({"role": turn["role"], "parts": list(turn["parts"])})

    # 4. If the last turn in prior_history is 'user', pop it and append to current_message
    # because Gemini chat.send_message_async sends a 'user' turn, so prior_history must end with 'model'
    if collapsed and collapsed[-1]["role"] == "user":
        last_user_text = "\n\n".join(collapsed.pop()["parts"])
        if current_message:
            current_message = f"{last_user_text}\n\n{current_message}"
        else:
            current_message = last_user_text

    return collapsed, current_message


class GeminiService:
    def __init__(self):
        # Do NOT log the API key
        genai.configure(api_key=settings.gemini_api_key)
        self._model_name = settings.gemini_model
        logger.info(f"GeminiService initialized with model: {self._model_name}")

    def _build_model(
        self,
        explanation_mode: str = "general",
        memory_context: Optional[str] = None,
        model_name: Optional[str] = None,
    ) -> genai.GenerativeModel:
        instruction = _SYSTEM_INSTRUCTIONS.get(
            explanation_mode, _SYSTEM_INSTRUCTIONS["general"]
        )
        if memory_context:
            instruction = f"{instruction}\n\n[USER RELEVANT MEMORY & PREFERENCES]\n{memory_context}"

        target_model = model_name or self._model_name
        return genai.GenerativeModel(
            model_name=target_model,
            system_instruction=instruction,
            generation_config=genai.GenerationConfig(
                max_output_tokens=settings.gemini_max_output_tokens,
            ),
        )

    async def generate_response(
        self,
        history: list[dict],
        explanation_mode: str = "general",
        memory_context: Optional[str] = None,
    ) -> str:
        """
        history: list of {"role": "user"|"model", "parts": [str]}
        Normalizes history and falls back across models if quota or transient errors occur.
        """
        prior_history, current_message = _normalize_history(history)
        if not current_message and not prior_history:
            return ""

        models_to_try = [self._model_name] + [m for m in _FALLBACK_MODELS if m != self._model_name]
        last_error = None

        for model_name in models_to_try:
            model = self._build_model(explanation_mode, memory_context, model_name=model_name)
            for attempt in range(2):
                try:
                    chat = model.start_chat(history=prior_history)
                    response = await chat.send_message_async(current_message)
                    logger.info(f"Gemini response received from {model_name}, length={len(response.text)}")
                    return response.text
                except Exception as e:
                    last_error = e
                    err_str = str(e)
                    is_fatal_for_model = any(k in err_str for k in ("429", "ResourceExhausted", "404", "not found"))
                    if is_fatal_for_model:
                        logger.warning(f"Model {model_name} quota/availability error ({e}). Trying fallback model...")
                        break  # Break attempt loop to try next model
                    elif ("503" in err_str or "Unavailable" in err_str) and attempt < 1:
                        await asyncio.sleep(2.0)
                    else:
                        break

        raise last_error or RuntimeError("All Gemini models exhausted")

    async def generate_response_stream(
        self,
        history: list[dict],
        explanation_mode: str = "general",
        memory_context: Optional[str] = None,
    ) -> AsyncIterator[str]:
        """
        Streams response chunks from Gemini asynchronously with automatic fallback across models.
        """
        prior_history, current_message = _normalize_history(history)
        if not current_message and not prior_history:
            return

        models_to_try = [self._model_name] + [m for m in _FALLBACK_MODELS if m != self._model_name]
        last_error = None

        for model_name in models_to_try:
            model = self._build_model(explanation_mode, memory_context, model_name=model_name)
            for attempt in range(2):
                try:
                    chat = model.start_chat(history=prior_history)
                    response_stream = await chat.send_message_async(current_message, stream=True)
                    yielded_any = False
                    async for chunk in response_stream:
                        try:
                            text = chunk.text
                        except (ValueError, AttributeError):
                            text = None
                        if text:
                            yielded_any = True
                            yield text
                    if yielded_any:
                        return
                    # If stream finished normally but yielded nothing, return
                    return
                except Exception as e:
                    last_error = e
                    err_str = str(e)
                    is_fatal_for_model = any(k in err_str for k in ("429", "ResourceExhausted", "404", "not found"))
                    if is_fatal_for_model:
                        logger.warning(f"Model {model_name} quota/availability error in stream ({e}). Trying fallback model...")
                        break  # Break attempt loop to try next model
                    elif ("503" in err_str or "Unavailable" in err_str) and attempt < 1:
                        await asyncio.sleep(2.0)
                    else:
                        break

        raise last_error or RuntimeError("All Gemini models exhausted")


from app.services.openrouter_service import openrouter_service

# Singleton: OpenRouter replaces Gemini provider
gemini_service = openrouter_service
