"""
Zone router — classify user questions to relevant business domain zone(s).

Uses keyword heuristics first (fast, no LLM call).
Falls back to LLM classification for ambiguous queries.

Zones are aligned with business domains, not technical layers.
"""

import re

ZONE_KEYWORDS: dict[str, list[str]] = {
    "contracts": [
        "product contract", "product-contract", "service contract", "service-contract",
        "express contract", "express-contract", "contract creation", "contract status",
        "contract version", "contract termination", "contract activation",
        "terms group", "terms-group", "termination group",
        "pod request", "point of delivery",
        "contract details", "contract sub status", "contract parameters",
        "resigning", "deal number",
    ],
    "billing": [
        "billing run", "billing-run", "billing group", "billing-group",
        "billing process", "billing profile", "billing by profile",
        "billing by scales", "billing-by-scales", "billing-by-profile",
        "accounting period", "accounting-period",
        "advance payment", "advanced payment", "advanced-payment",
        "iap", "interim advance",
    ],
    "invoicing": [
        "invoice", "invoice cancellation", "invoice-cancellation",
        "credit note", "invoice correction", "invoice status",
        "invoice generation", "invoice list",
    ],
    "payments": [
        "payment", "payment package", "payment-package",
        "deposit", "liability", "customer-liability", "customer liability",
        "receivable", "customer-receivable", "customer receivable",
        "late payment", "latepaymentfine", "late payment fine",
        "interest rate", "interest-rate", "penalty", "penalties",
        "penalty group", "penalty-group",
        "collection", "collection channel", "collection-channel",
        "offsetting", "manual-liability-offsetting", "manual liability",
        "financial", "balance",
    ],
    "customers": [
        "customer", "customer creation", "customer assessment",
        "customer-assessment", "assessment criteria",
        "unwanted customer", "unwanted-customer",
        "connected group", "connected-group",
        "missing customer", "missing-customer",
        "customer data", "customer search", "customer list",
    ],
    "products": [
        "product", "product catalog", "product type",
        "price component", "price-component", "price component group",
        "pricing", "price list", "prices",
        "discount", "tariff", "formula",
        "government compensation", "government-compensation",
        "nomenclature",
    ],
    "service_operations": [
        "service order", "service-order",
        "action", "actions list",
        "process", "task", "task type",
        "disconnection", "disconnection of power", "power supply disconnection",
        "reconnection", "reconnection of power", "power supply reconnection",
        "rescheduling", "reschedule",
        "cancellation of disconnection",
        "power supply", "reminder",
        "mass operation", "mass import", "blocking",
        "lock", "locks",
    ],
    "communications": [
        "email", "email communication", "email-communication",
        "sms", "sms communication", "sms-communication",
        "system message", "system-message", "notification",
        "template", "document", "documents",
        "resource file", "resource-file",
        "system activities", "system-activities",
        "communication",
    ],
    "reference_data": [
        "zip code", "zip-code", "street", "district",
        "municipality", "region", "populated place", "residential area",
        "currency", "currencies", "company details", "company-details",
        "vat rate", "vat-rate", "user type", "user-type",
        "pod", "profile", "preference",
        "grid operator", "grid-operator",
        "xenenergie", "xenergy", "portal",
        "balancing group", "balancing coordinator",
        "qes", "goods", "goods order", "goods-order",
    ],
}


def route_by_keywords(question: str) -> list[str]:
    """Match question to zone(s) using keyword scoring."""
    q_lower = question.lower()
    scores: dict[str, int] = {}

    for zone, keywords in ZONE_KEYWORDS.items():
        score = 0
        for kw in keywords:
            if kw in q_lower:
                score += len(kw.split())
        if score > 0:
            scores[zone] = score

    if not scores:
        return []

    max_score = max(scores.values())
    threshold = max(1, max_score * 2 // 3)
    return sorted(
        [zone for zone, score in scores.items() if score >= threshold],
        key=lambda z: scores[z],
        reverse=True,
    )


def route_question(question: str, llm_client=None) -> list[str]:
    """Route question to zone(s). Keyword-first, LLM fallback."""
    zones = route_by_keywords(question)
    if zones:
        return zones

    if llm_client:
        return llm_client.classify_zone(question)

    return ["contracts"]
