import re
from typing import Any

from loguru import logger


try:
    from tools.domain_adapter import DomainAdapter

    _DOMAIN_ADAPTER_AVAILABLE = True
except Exception:
    _DOMAIN_ADAPTER_AVAILABLE = False


class LegalAgent:
    """
    Legal document analysis: contract review, NDA flags, risk assessment.
    Disclaimer-first output.
    Closes Gap #45
    """

    RISK_INDICATORS = {
        "unlimited_liability": [
            r"unlimited\s+liability",
            r"liability\s+shall\s+not\s+be\s+limited",
            r"no\s+cap\s+on\s+damages",
        ],
        "indemnification": [r"indemnify", r"indemnification", r"hold\s+harmless"],
        "ip_assignment": [
            r"assign\s+all\s+rights",
            r"work\s+for\s+hire",
            r"intellectual\s+property\s+transfer",
        ],
        "non_compete": [r"non[\s-]?compete", r"shall\s+not\s+compete"],
        "termination": [
            r"terminat\w*\s+for\s+convenience",
            r"terminate\s+this\s+agreement",
        ],
        "auto_renewal": [
            r"auto[\s-]?renew",
            r"automatic\s+renewal",
            r"renew\s+automatically",
        ],
        "jurisdiction": [
            r"exclusive\s+jurisdiction",
            r"governed\s+by\s+the\s+laws\s+of",
        ],
        "ambiguous": [
            r"reasonable\s+efforts",
            r"best\s+efforts",
            r"material\s+adverse\s+effect",
        ],
    }

    SYSTEM_PROMPT = (
        "You are a legal analyst assistant. Your role is to identify risks, flag missing clauses, and summarize "
        "obligations in legal documents. You are NOT a lawyer. Always lead with a clear disclaimer."
    )

    def __init__(self):
        self.domain_adapter = DomainAdapter() if _DOMAIN_ADAPTER_AVAILABLE else None
        logger.info("Initialized LegalAgent")

    def analyze(self, document_text: str, doc_type: str = "contract") -> dict[str, Any]:
        risks = self._rule_based_scan(document_text)
        llm_summary = self._llm_summary(document_text, doc_type, risks)
        return {
            "doc_type": doc_type,
            "risk_score": self._risk_score(risks),
            "risks": risks,
            "summary": llm_summary,
            "disclaimer": (
                "Disclaimer: This analysis is for informational purposes only and does not constitute legal advice. "
                "Consult a licensed attorney for legal decisions."
            ),
        }

    def _rule_based_scan(self, text: str) -> list[dict[str, Any]]:
        findings: list[dict[str, Any]] = []
        lower = text.lower()
        for category, patterns in self.RISK_INDICATORS.items():
            for pat in patterns:
                for m in re.finditer(pat, lower, re.IGNORECASE):
                    line = text[: m.start()].count("\n") + 1
                    findings.append(
                        {
                            "category": category,
                            "pattern": pat,
                            "line": line,
                            "snippet": text[
                                max(0, m.start() - 40) : m.end() + 40
                            ].strip(),
                            "severity": (
                                "high"
                                if category
                                in {"unlimited_liability", "indemnification"}
                                else "medium"
                            ),
                        }
                    )
        return findings

    def _risk_score(self, risks: list[dict[str, Any]]) -> float:
        score = 1.0
        for r in risks:
            sev = r.get("severity", "low")
            if sev == "critical":
                score -= 0.15
            elif sev == "high":
                score -= 0.08
            elif sev == "medium":
                score -= 0.03
            else:
                score -= 0.01
        return max(0.0, round(score, 2))

    def _llm_summary(
        self, text: str, doc_type: str, risks: list[dict[str, Any]]
    ) -> str:
        if self.domain_adapter:
            summary_prompt = (
                f"Analyze this {doc_type}. First state the disclaimer. "
                f"Then summarize obligations, highlight {len(risks)} risk flags, "
                f"and list missing clauses.\n\nDocument:\n{text[:8000]}"
            )
            try:
                result = self.domain_adapter.adapt_request("legal", summary_prompt)
                return result.get("response", "")
            except Exception as exc:
                logger.debug(f"Legal LLM summary failed: {exc}")
        return (
            f"[Rule-based review] Document type: {doc_type}. "
            f"Identified {len(risks)} risk indicators. "
            "Enable 'legal' domain in DomainAdapter for LLM-enhanced analysis."
        )

    def review_nda(self, nda_text: str) -> dict[str, Any]:
        return self.analyze(nda_text, doc_type="nda")

    def review_contract(self, contract_text: str) -> dict[str, Any]:
        return self.analyze(contract_text, doc_type="contract")
