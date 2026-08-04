"""
LM Studio (Bionic) wrapper for generation + local sentence-transformers for embeddings.

Generation: OpenAI-compatible API at localhost:1234
Embeddings: sentence-transformers (all-MiniLM-L6-v2, runs on CPU, 384 dimensions)
"""

import os

import yaml
from openai import OpenAI

_CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "zones.yaml")

_embed_model = None


def _load_llm_config() -> dict:
    with open(_CONFIG_PATH, encoding="utf-8") as f:
        return yaml.safe_load(f).get("llm", {})


def _get_embed_model():
    global _embed_model
    if _embed_model is None:
        from sentence_transformers import SentenceTransformer
        _embed_model = SentenceTransformer("all-MiniLM-L6-v2")
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
        model = _get_embed_model()
        embedding = model.encode(text, normalize_embeddings=True)
        return embedding.tolist()

    def embed_batch(self, texts: list[str], batch_size: int = 64) -> list[list[float]]:
        model = _get_embed_model()
        embeddings = model.encode(texts, batch_size=batch_size, normalize_embeddings=True)
        return [e.tolist() for e in embeddings]

    def classify_zone(self, question: str) -> list[str]:
        system = (
            "You classify questions into one or more knowledge zones. "
            "Return ONLY zone names, comma-separated, no explanation.\n"
            "Zones:\n"
            "- contracts: product contracts, service contracts, express contracts, terms, terminations\n"
            "- billing: billing runs, billing groups, billing profiles, accounting periods, IAP\n"
            "- invoicing: invoices, invoice cancellation, credit notes\n"
            "- payments: payments, deposits, liabilities, receivables, fines, penalties, collections\n"
            "- customers: customers, customer assessment, unwanted customers\n"
            "- products: products catalog, pricing, price components, discounts, nomenclature\n"
            "- service_operations: service orders, actions, processes, tasks, disconnection, reconnection\n"
            "- communications: email, SMS, system messages, templates, documents\n"
            "- reference_data: geography, currencies, grid operators, portal, xEnergie, goods\n"
        )
        result = self.generate(question, system_prompt=system, temperature=0.0, max_tokens=100)
        zones = [z.strip().lower() for z in result.split(",")]
        valid_zones = [
            "contracts", "billing", "invoicing", "payments", "customers",
            "products", "service_operations", "communications", "reference_data",
        ]
        return [z for z in zones if z in valid_zones] or ["contracts"]

    def synthesize_answer(self, question: str, graph_context: str,
                          live_evidence: str | None = None) -> str:
        system = (
            "You are a Senior QA / Phoenix Expert assistant. "
            "Answer the question using ONLY the provided context. "
            "Always cite sources (file paths, node names, zone names). "
            "If the context is insufficient, say so clearly. "
            "Format findings as:\n"
            "### Finding: [title]\n"
            "- **Type:** ...\n"
            "- **Gap:** ...\n"
            "- **Recommendation:** ...\n"
        )
        prompt_parts = [f"## Question\n{question}\n", f"## Graph Context\n{graph_context}\n"]
        if live_evidence:
            prompt_parts.append(f"## Live Evidence\n{live_evidence}\n")
        prompt_parts.append("## Your Answer")

        return self.generate("\n".join(prompt_parts), system_prompt=system)
