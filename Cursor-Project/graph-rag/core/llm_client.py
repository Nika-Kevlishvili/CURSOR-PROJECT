"""
LM Studio (Bionic) wrapper for generation + local sentence-transformers for embeddings.

Generation: OpenAI-compatible API at localhost:1234
Embeddings: sentence-transformers (all-MiniLM-L6-v2, runs on CPU, 384 dimensions)
"""

import os
import threading

import yaml
from openai import OpenAI

_CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "zones.yaml")

_embed_model = None
_embed_ready = threading.Event()
_embed_lock = threading.Lock()


def _load_llm_config() -> dict:
    with open(_CONFIG_PATH, encoding="utf-8") as f:
        return yaml.safe_load(f).get("llm", {})


def _get_embed_model():
    global _embed_model
    with _embed_lock:
        if _embed_model is None:
            from sentence_transformers import SentenceTransformer
            _embed_model = SentenceTransformer("all-MiniLM-L6-v2")
            _embed_ready.set()
    return _embed_model


def preload_embed_model():
    """Load in background thread; callers wait via _embed_ready event."""
    _get_embed_model()


def wait_for_embed_model(timeout: float = 60.0):
    """Block until the embedding model is loaded (max timeout seconds)."""
    _embed_ready.wait(timeout=timeout)
    return _embed_model


class LLMClient:

    def __init__(self, base_url: str | None = None, model: str | None = None):
        cfg = _load_llm_config()
        self._base_url = base_url or os.environ.get(
            "LLM_BASE_URL", cfg.get("base_url", "http://localhost:1234/v1"))
        self._model = model or os.environ.get(
            "LLM_MODEL", cfg.get("model", "qwen2.5-3b-instruct"))
        self._temperature = cfg.get("temperature", 0.1)
        self._max_tokens = cfg.get("max_tokens", 2048)

        self._client = OpenAI(base_url=self._base_url, api_key="lm-studio")

    def generate(self, prompt: str, system_prompt: str | None = None,
                 temperature: float | None = None,
                 max_tokens: int | None = None) -> str:
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        response = self._client.chat.completions.create(
            model=self._model,
            messages=messages,
            temperature=temperature or self._temperature,
            max_tokens=max_tokens or self._max_tokens,
        )
        return response.choices[0].message.content or ""

    def embed(self, text: str) -> list[float]:
        _embed_ready.wait(timeout=60.0)
        model = _get_embed_model()
        embedding = model.encode(text, normalize_embeddings=True)
        return embedding.tolist()

    def embed_batch(self, texts: list[str], batch_size: int = 64) -> list[list[float]]:
        _embed_ready.wait(timeout=60.0)
        model = _get_embed_model()
        embeddings = model.encode(texts, batch_size=batch_size, normalize_embeddings=True)
        return [e.tolist() for e in embeddings]

    def classify_zone(self, question: str) -> list[str]:
        system = (
            "You classify questions into one or more knowledge zones. "
            "Return ONLY zone names, comma-separated, no explanation.\n"
            "Zones:\n"
            "- phoenix_domain: business meaning, entities, validations, processes\n"
            "- api_and_repo_layout: HTTP endpoints, DTOs, Swagger, enums\n"
            "- test_cases: markdown test cases, steps, expected results\n"
            "- playwright_automation: Playwright specs, fixtures, EnergoTS\n"
        )
        result = self.generate(question, system_prompt=system, temperature=0.0, max_tokens=100)
        zones = [z.strip().lower() for z in result.split(",")]
        valid_zones = [
            "phoenix_domain", "api_and_repo_layout",
            "test_cases", "playwright_automation",
        ]
        return [z for z in zones if z in valid_zones] or [
            "phoenix_domain", "api_and_repo_layout",
        ]

    def synthesize_answer(self, question: str, graph_context: str,
                          live_evidence: str | None = None) -> str:
        system = (
            "You list which sources to open. Use ONLY the graph context. "
            "Start with a bullet list of source_path values and Confluence page IDs. "
            "Do not invent files, wiki IDs, or business rules that are not in the context. "
            "Do not write a full product encyclopedia. "
            "If a Finding/mismatch is in the context, one short sentence is enough."
        )
        prompt_parts = [f"## Question\n{question}\n", f"## Graph Context\n{graph_context}\n"]
        if live_evidence:
            prompt_parts.append(f"## Live Evidence\n{live_evidence}\n")
        prompt_parts.append("## Your Answer")

        return self.generate("\n".join(prompt_parts), system_prompt=system)
