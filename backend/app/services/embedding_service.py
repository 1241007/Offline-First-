"""Embedding service for generating text embeddings with strict provider consistency."""

import logging
from typing import List, Optional
import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)


class EmbeddingConfigurationError(Exception):
    """Raised when embedding configuration is invalid or inconsistent."""
    pass


class EmbeddingDimensionMismatchError(Exception):
    """Raised when returned embedding dimension does not match configured dimension."""
    pass


class EmbeddingService:
    """
    Provider-consistent embedding service.
    
    Guarantees:
    - Dedicated embedding provider configuration (OpenAI, OpenRouter, or Custom).
    - Prevents using OpenRouter API keys at OpenAI endpoints.
    - Validates returned vector dimensions against expected dimension before pgvector insertion/search.
    - Strict HTTP timeout enforcement matching retrieval deadlines.
    """

    def __init__(
        self,
        provider: Optional[str] = None,
        model: Optional[str] = None,
        base_url: Optional[str] = None,
        api_key: Optional[str] = None,
        dimensions: Optional[int] = None,
        timeout_seconds: Optional[float] = None,
    ):
        self.provider = (provider or settings.embedding_provider).lower()
        self.model = model or settings.embedding_model
        self.base_url = (base_url or settings.embedding_base_url).rstrip("/")
        self.dimensions = dimensions or settings.embedding_dimensions
        self.timeout_seconds = timeout_seconds or settings.embedding_timeout_seconds

        # Determine API key based on provider
        explicit_key = api_key if api_key is not None else settings.embedding_api_key
        if self.provider == "openrouter":
            self.api_key = explicit_key or settings.openrouter_api_key
            if "api.openai.com" in self.base_url:
                self.base_url = "https://openrouter.ai/api/v1"
        else:
            self.api_key = explicit_key or ""

        self._validate_configuration()

    def _validate_configuration(self) -> None:
        """Validate provider and credential consistency."""
        if not self.api_key:
            # Service can be instantiated without key, but embed_query will report not configured
            return

        # STRICT CHECK: Reject OpenRouter key at OpenAI endpoint
        if "api.openai.com" in self.base_url and self.api_key.startswith("sk-or-v1-"):
            raise EmbeddingConfigurationError(
                "OpenRouter API key cannot be used with OpenAI API endpoint (https://api.openai.com). "
                "Provide a valid OpenAI API key in EMBEDDING_API_KEY or configure EMBEDDING_PROVIDER=openrouter."
            )

    @property
    def is_configured(self) -> bool:
        """Check if embedding service has valid credentials."""
        if not self.api_key:
            return False
        if "api.openai.com" in self.base_url and self.api_key.startswith("sk-or-v1-"):
            return False
        return True

    async def embed_query(self, text: str) -> List[float]:
        """
        Generate embedding vector for a single query string.
        
        Enforces strict timeouts and dimension validation.
        """
        if not self.is_configured:
            raise EmbeddingConfigurationError(
                "Embedding service is not configured with a valid API key for provider "
                f"'{self.provider}'. Set EMBEDDING_API_KEY."
            )

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        if self.provider == "openrouter":
            headers["HTTP-Referer"] = "https://edunova.app"
            headers["X-Title"] = "EduNova"

        payload = {
            "model": self.model,
            "input": text.strip(),
        }

        # Enforce underlying HTTP client timeout <= embedding deadline
        client_timeout = httpx.Timeout(
            timeout=self.timeout_seconds,
            connect=min(1.0, self.timeout_seconds),
            read=self.timeout_seconds,
            write=self.timeout_seconds,
        )

        url = f"{self.base_url}/embeddings"

        try:
            async with httpx.AsyncClient(timeout=client_timeout) as client:
                response = await client.post(url, json=payload, headers=headers)

                if response.status_code == 401:
                    raise EmbeddingConfigurationError(
                        f"Embedding API authentication failed (HTTP 401) at {self.base_url}. "
                        "Check EMBEDDING_API_KEY configuration."
                    )
                if response.status_code != 200:
                    raise RuntimeError(
                        f"Embedding API error HTTP {response.status_code}: {response.text[:200]}"
                    )

                data = response.json()
                embedding = data["data"][0]["embedding"]

                if len(embedding) != self.dimensions:
                    raise EmbeddingDimensionMismatchError(
                        f"Embedding dimension mismatch: expected {self.dimensions}, "
                        f"got {len(embedding)} from model '{self.model}'"
                    )

                return embedding

        except httpx.TimeoutException as exc:
            logger.error(f"Embedding request timed out after {self.timeout_seconds}s at {url}")
            raise TimeoutError(f"Embedding request timed out after {self.timeout_seconds}s") from exc
        except (EmbeddingConfigurationError, EmbeddingDimensionMismatchError):
            raise
        except Exception as exc:
            logger.error(f"Failed to generate embedding: {exc}")
            raise


embedding_service = EmbeddingService()
