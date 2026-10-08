import asyncio
import json
import logging
from typing import Optional, AsyncIterator
import httpx
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

# Verified OpenRouter model slugs
_FALLBACK_MODELS = [
    "google/gemini-3.8-flash",
    "google/gemini-2.5-flash",
    "google/gemini-3.5-flash",
]


def _normalize_model_name(model_name: str) -> str:
    """Ensure OpenRouter model slug includes the provider prefix if omitted."""
    name = (model_name or "").strip()
    if not name:
        return "google/gemini-3.8-flash"
    if "/" not in name and name.startswith("gemini"):
        return f"google/{name}"
    return name


def _format_messages_for_openrouter(
    history: list[dict],
    system_instruction: str,
) -> list[dict]:
    """
    Transforms conversation history into standard OpenAI/OpenRouter message list.
    Supports both legacy Gemini format ({"role": "user"|"model", "parts": [...]})
    and standard chat format ({"role": "user"|"assistant", "content": "..."}).
    """
    messages: list[dict] = []
    if system_instruction:
        messages.append({"role": "system", "content": system_instruction})

    for item in history:
        role = item.get("role", "user")
        if role == "model":
            role = "assistant"
        elif role not in ("system", "user", "assistant"):
            role = "user"

        content = ""
        if "content" in item and item["content"] is not None:
            content = str(item["content"]).strip()
        elif "parts" in item and item["parts"]:
            parts_str = [str(p).strip() for p in item["parts"] if p and str(p).strip()]
            content = "\n\n".join(parts_str).strip()

        if content:
            messages.append({"role": role, "content": content})

    return messages


class OpenRouterService:
    def __init__(self):
        self._api_key = (
            getattr(settings, "openrouter_api_key", None)
            or getattr(settings, "gemini_api_key", "")
            or ""
        ).strip()
        raw_model = (
            getattr(settings, "openrouter_model", None)
            or getattr(settings, "gemini_model", "google/gemini-3.8-flash")
        )
        self._model_name = _normalize_model_name(raw_model)
        self._base_url = (
            getattr(settings, "openrouter_base_url", None)
            or "https://openrouter.ai/api/v1"
        ).rstrip("/")
        self._max_tokens = getattr(settings, "openrouter_max_tokens", 1024)
        self._client: Optional[httpx.AsyncClient] = None

        # Do NOT log the API key
        logger.info(
            f"OpenRouterService initialized with model: {self._model_name}, base_url: {self._base_url}"
        )

    async def get_client(self) -> httpx.AsyncClient:
        """Returns a pooled, persistent AsyncClient with configured connection limits."""
        if self._client is None or self._client.is_closed:
            limits = httpx.Limits(max_keepalive_connections=20, max_connections=50)
            self._client = httpx.AsyncClient(
                timeout=httpx.Timeout(connect=15.0, read=90.0, write=30.0, pool=15.0),
                limits=limits,
            )
        return self._client

    async def close(self):
        """Cleanly releases pooled HTTP connections upon application shutdown."""
        if self._client is not None and not self._client.is_closed:
            await self._client.aclose()
            self._client = None

    def _get_headers(self) -> dict[str, str]:
        # Never expose API key in logs
        return {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
            "HTTP-Referer": "https://edunova.app",
            "X-Title": "EduNova",
        }

    def _build_payload(
        self,
        history: list[dict],
        explanation_mode: str = "general",
        memory_context: Optional[str] = None,
        model_name: Optional[str] = None,
        stream: bool = False,
    ) -> tuple[str, dict]:
        target_model = _normalize_model_name(model_name or self._model_name)
        instruction = _SYSTEM_INSTRUCTIONS.get(
            explanation_mode, _SYSTEM_INSTRUCTIONS["general"]
        )
        if memory_context:
            instruction = (
                f"{instruction}\n\n[USER RELEVANT MEMORY & PREFERENCES]\n{memory_context}"
            )

        messages = _format_messages_for_openrouter(history, instruction)
        payload = {
            "model": target_model,
            "messages": messages,
            "stream": stream,
            "max_tokens": self._max_tokens,
        }
        return target_model, payload

    async def generate_response(
        self,
        history: list[dict],
        explanation_mode: str = "general",
        memory_context: Optional[str] = None,
    ) -> str:
        """
        Sends request to OpenRouter and returns full assistant response.
        Falls back across models if transient or quota errors occur.
        """
        if not self._api_key:
            raise RuntimeError("OPENROUTER_API_KEY is not configured")

        models_to_try = [self._model_name] + [
            m for m in _FALLBACK_MODELS if m != self._model_name
        ]
        last_error = None

        for model_name in models_to_try:
            _, payload = self._build_payload(
                history,
                explanation_mode=explanation_mode,
                memory_context=memory_context,
                model_name=model_name,
                stream=False,
            )

            for attempt in range(2):
                try:
                    client = await self.get_client()
                    response = await client.post(
                        f"{self._base_url}/chat/completions",
                        headers=self._get_headers(),
                        json=payload,
                    )

                    # Fatal auth errors: do not retry or cycle models
                    if response.status_code in (401, 403):
                        logger.error(
                            f"OpenRouter authentication error HTTP {response.status_code}"
                        )
                        raise RuntimeError(
                            f"OpenRouter authentication failed (HTTP {response.status_code})"
                        )

                    # Model not found or invalid model: try fallback model immediately
                    if response.status_code in (400, 404):
                        logger.warning(
                            f"OpenRouter model error for {model_name} (HTTP {response.status_code}): {response.text[:200]}"
                        )
                        last_error = RuntimeError(
                            f"Model {model_name} unavailable (HTTP {response.status_code})"
                        )
                        break

                    # Transient errors (429 rate limit or 5xx server error): retry or switch model
                    if response.status_code in (429, 500, 502, 503, 504):
                        logger.warning(
                            f"OpenRouter transient error HTTP {response.status_code} for {model_name} (attempt {attempt + 1})"
                        )
                        last_error = RuntimeError(
                            f"OpenRouter transient failure (HTTP {response.status_code})"
                        )
                        if attempt == 0:
                            await asyncio.sleep(1.5)
                            continue
                        break

                    if response.status_code != 200:
                        last_error = RuntimeError(
                            f"OpenRouter returned unexpected status HTTP {response.status_code}"
                        )
                        break

                    data = response.json()
                    choices = data.get("choices", [])
                    if choices:
                        message = choices[0].get("message", {})
                        content = message.get("content") or ""
                        logger.info(
                            f"OpenRouter response received from {model_name}, length={len(content)}"
                        )
                        return content
                    return ""

                except (httpx.TimeoutException, httpx.NetworkError) as net_err:
                    last_error = net_err
                    logger.warning(
                        f"Network/timeout error connecting to OpenRouter ({type(net_err).__name__}) on attempt {attempt + 1}"
                    )
                    if attempt == 0:
                        await asyncio.sleep(1.5)
                        continue
                    break
                except Exception as exc:
                    if "authentication failed" in str(exc).lower():
                        raise
                    last_error = exc
                    logger.warning(f"Error calling OpenRouter with {model_name}: {exc}")
                    break

        raise last_error or RuntimeError("All OpenRouter models exhausted")

    async def generate_response_stream(
        self,
        history: list[dict],
        explanation_mode: str = "general",
        memory_context: Optional[str] = None,
    ) -> AsyncIterator[str]:
        """
        Streams response tokens from OpenRouter asynchronously with automatic fallback across models.
        """
        if not self._api_key:
            raise RuntimeError("OPENROUTER_API_KEY is not configured")

        models_to_try = [self._model_name] + [
            m for m in _FALLBACK_MODELS if m != self._model_name
        ]
        last_error = None

        for model_name in models_to_try:
            _, payload = self._build_payload(
                history,
                explanation_mode=explanation_mode,
                memory_context=memory_context,
                model_name=model_name,
                stream=True,
            )

            for attempt in range(2):
                try:
                    yielded_any = False
                    client = await self.get_client()
                    async with client.stream(
                        "POST",
                        f"{self._base_url}/chat/completions",
                        headers=self._get_headers(),
                        json=payload,
                    ) as response:
                            if response.status_code in (401, 403):
                                logger.error(
                                    f"OpenRouter stream authentication error HTTP {response.status_code}"
                                )
                                raise RuntimeError(
                                    f"OpenRouter authentication failed (HTTP {response.status_code})"
                                )

                            if response.status_code in (400, 404):
                                logger.warning(
                                    f"OpenRouter stream model error for {model_name} HTTP {response.status_code}"
                                )
                                last_error = RuntimeError(
                                    f"Model {model_name} unavailable (HTTP {response.status_code})"
                                )
                                break

                            if response.status_code in (429, 500, 502, 503, 504):
                                logger.warning(
                                    f"OpenRouter stream transient error HTTP {response.status_code} for {model_name} (attempt {attempt + 1})"
                                )
                                last_error = RuntimeError(
                                    f"OpenRouter transient failure (HTTP {response.status_code})"
                                )
                                if attempt == 0:
                                    await asyncio.sleep(1.5)
                                    continue
                                break

                            if response.status_code != 200:
                                last_error = RuntimeError(
                                    f"OpenRouter stream returned unexpected HTTP {response.status_code}"
                                )
                                break

                            async for line in response.aiter_lines():
                                line_clean = line.strip()
                                if not line_clean:
                                    continue
                                if line_clean.startswith("data: "):
                                    data_str = line_clean[6:].strip()
                                    if data_str == "[DONE]":
                                        break
                                    try:
                                        chunk_data = json.loads(data_str)
                                    except json.JSONDecodeError:
                                        continue

                                    if "error" in chunk_data:
                                        err_detail = chunk_data["error"].get(
                                            "message", "OpenRouter stream error"
                                        )
                                        logger.error(
                                            f"Error chunk from OpenRouter stream: {err_detail}"
                                        )
                                        raise RuntimeError(err_detail)

                                    choices = chunk_data.get("choices", [])
                                    if choices:
                                        delta = choices[0].get("delta", {})
                                        content = delta.get("content")
                                        if content:
                                            yielded_any = True
                                            yield content

                    if yielded_any:
                        return
                    return

                except (httpx.TimeoutException, httpx.NetworkError) as net_err:
                    last_error = net_err
                    if yielded_any:
                        # Partial stream already sent to client, cannot switch model midway
                        raise
                    logger.warning(
                        f"Network/timeout error in OpenRouter stream ({type(net_err).__name__}) on attempt {attempt + 1}"
                    )
                    if attempt == 0:
                        await asyncio.sleep(1.5)
                        continue
                    break
                except Exception as exc:
                    if "authentication failed" in str(exc).lower():
                        raise
                    if yielded_any:
                        raise
                    last_error = exc
                    logger.warning(
                        f"Error in OpenRouter stream with {model_name}: {exc}"
                    )
                    break

        raise last_error or RuntimeError("All OpenRouter models exhausted")


# Singleton
openrouter_service = OpenRouterService()
