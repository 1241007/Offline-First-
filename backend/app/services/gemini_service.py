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
    ) -> genai.GenerativeModel:
        instruction = _SYSTEM_INSTRUCTIONS.get(
            explanation_mode, _SYSTEM_INSTRUCTIONS["general"]
        )
        if memory_context:
            instruction = f"{instruction}\n\n[USER RELEVANT MEMORY & PREFERENCES]\n{memory_context}"

        return genai.GenerativeModel(
            model_name=self._model_name,
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
        The last item in history is the current user message.
        All preceding items are conversation context.
        """
        model = self._build_model(explanation_mode, memory_context)
        prior_history = history[:-1] if len(history) > 1 else []
        current_message = history[-1]["parts"][0] if history else ""

        chat = model.start_chat(history=prior_history)

        for attempt in range(3):
            try:
                response = await chat.send_message_async(current_message)
                logger.info(f"Gemini response received, length={len(response.text)}")
                return response.text
            except Exception as e:
                err_str = str(e)
                if ("503" in err_str or "429" in err_str or "ResourceExhausted" in err_str or "Unavailable" in err_str) and attempt < 2:
                    wait_time = (attempt + 1) * 2.0
                    logger.warning(f"Gemini transient error on attempt {attempt + 1}, retrying in {wait_time}s: {e}")
                    await asyncio.sleep(wait_time)
                else:
                    raise e

    async def generate_response_stream(
        self,
        history: list[dict],
        explanation_mode: str = "general",
        memory_context: Optional[str] = None,
    ) -> AsyncIterator[str]:
        """
        Streams response chunks from Gemini asynchronously.
        Includes retry for transient high-demand (503) or rate-limit (429) spikes.
        """
        model = self._build_model(explanation_mode, memory_context)
        prior_history = history[:-1] if len(history) > 1 else []
        current_message = history[-1]["parts"][0] if history else ""

        chat = model.start_chat(history=prior_history)

        for attempt in range(3):
            try:
                response_stream = await chat.send_message_async(current_message, stream=True)
                async for chunk in response_stream:
                    try:
                        text = chunk.text
                    except (ValueError, AttributeError):
                        text = None
                    if text:
                        yield text
                return
            except Exception as e:
                err_str = str(e)
                if ("503" in err_str or "429" in err_str or "ResourceExhausted" in err_str or "Unavailable" in err_str) and attempt < 2:
                    wait_time = (attempt + 1) * 2.0
                    logger.warning(f"Gemini transient error on attempt {attempt + 1}, retrying in {wait_time}s: {e}")
                    await asyncio.sleep(wait_time)
                else:
                    raise e


# Singleton
gemini_service = GeminiService()
