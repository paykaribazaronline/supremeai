from typing import Any

from loguru import logger


class LegalAgent:
    """
    লিগ্যাল এজেন্ট — চুক্তি তৈরি, ক্লজ বিশ্লেষণ, কমপ্লায়েন্স চেক ও ToS/Privacy Policy জেনারেট করে।

    Devin/Cursor-এর মতো নির্দিষ্ট ডোমেইনে সেরা হওয়ার জন্য:
      - চুক্তি তৈরি (NDA, Service Agreement, etc.)
      - ক্লজ বিশ্লেষণ (জুরিসডিকশন অনুযায়ী)
      - Compliance check (GDPR, BD Digital Security Act, etc.)
      - Terms of Service / Privacy Policy
    """

    def __init__(self, model: str = "gpt-4o"):
        self.model = model
        logger.info(f"Initialized LegalAgent with model {self.model}")

    async def generate_contract(
        self,
        contract_type: str,
        parties: list[str],
        terms: dict[str, str],
        jurisdiction: str = "BD",
    ) -> dict[str, Any]:
        """চুক্তি তৈরি করে (NDA, Service Agreement, ইত্যাদি)।"""
        logger.info(f"Generating {contract_type} for jurisdiction {jurisdiction}")
        parties_str = ", ".join(parties) if parties else "[Party A] and [Party B]"
        terms_str = (
            "\n".join(f"- {k}: {v}" for k, v in terms.items()) or "- [Specify terms]"
        )
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                f"You are a licensed legal drafter. Draft a {contract_type} under the laws of {jurisdiction}. "
                f"Parties: {parties_str}. Key terms:\n{terms_str}\n"
                "Return ONLY the legal document text in Markdown, with numbered sections and placeholders "
                "for dates/signatures."
            )
            result = await router.async_route_and_generate(
                prompt, task_type="legal", max_cost=0.03
            )
            doc = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "type": contract_type,
                "jurisdiction": jurisdiction,
                "document": doc.strip(),
            }
        except Exception as exc:
            logger.error(f"Contract generation failed: {exc}")
            return {"status": "error", "error": str(exc)}

    async def analyze_clause(
        self, clause_text: str, jurisdiction: str = "BD"
    ) -> dict[str, Any]:
        """ক্লজ বিশ্লেষণ করে (রিস্ক, আইনি ইমপ্লিকেশন)।"""
        logger.info(f"Analyzing clause under {jurisdiction} jurisdiction...")
        # বাংলা মন্তব্য: স্থানীয় হিউরিস্টিক — একতরফা/অসমান অধিকার ক্লজ থাকলে সতর্ক করা হচ্ছে।
        risks: list[str] = []
        lowered = clause_text.lower()
        if "sole discretion" in lowered or "unilateral" in lowered:
            risks.append(
                "Clause grants unilateral discretion; may be unconscionable under consumer protection law."
            )
        if "waive" in lowered and "liability" in lowered:
            risks.append(
                "Liability waiver detected; enforceability varies by jurisdiction and may be void if gross negligence."
            )
        if "indemnify" in lowered:
            risks.append(
                "Indemnification clause present; ensure mutual scope and caps are defined."
            )
        if "governing law" not in lowered and "jurisdiction" not in lowered:
            risks.append(
                f"No governing law specified; defaulting to {jurisdiction} courts may be contested."
            )
        return {
            "status": "success",
            "jurisdiction": jurisdiction,
            "risk_count": len(risks),
            "risks": risks or ["No obvious red-flag clauses detected."],
            "summary": clause_text[:200],
        }

    async def check_compliance(self, document: str, regulation: str) -> dict[str, Any]:
        """কমপ্লায়েন্স চেক করে (GDPR, BD Digital Security Act, ইত্যাদি)।"""
        logger.info(f"Checking compliance against {regulation}...")
        findings: list[dict[str, Any]] = []
        lowered = document.lower()
        if regulation.upper() in ("GDPR", "EU"):
            if "data subject" not in lowered and "personal data" not in lowered:
                findings.append(
                    {
                        "severity": "high",
                        "message": "No reference to data subject rights (GDPR Art. 12-22).",
                    }
                )
            if "consent" not in lowered:
                findings.append(
                    {
                        "severity": "medium",
                        "message": "No lawful basis / consent mechanism mentioned.",
                    }
                )
        if "BD" in regulation.upper() or "bangladesh" in lowered:
            if "data protection" not in lowered and "privacy" not in lowered:
                findings.append(
                    {
                        "severity": "medium",
                        "message": "No data protection clause referencing Bangladesh Digital Security Act.",
                    }
                )
        if not findings:
            findings.append(
                {
                    "severity": "info",
                    "message": f"Document appears consistent with {regulation} baseline requirements.",
                }
            )
        return {
            "status": "success",
            "regulation": regulation,
            "findings": findings,
            "compliant": all(f["severity"] != "high" for f in findings),
        }

    async def generate_tos(
        self, product_description: str, jurisdiction: str = "BD"
    ) -> dict[str, Any]:
        """Terms of Service / Privacy Policy জেনারেট করে।"""
        logger.info(f"Generating ToS/Privacy Policy for {jurisdiction}...")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                f"You are a legal compliance expert. Draft a Terms of Service and a Privacy Policy for the "
                f"following product under {jurisdiction} law: {product_description}. "
                "Return ONLY the combined legal text in Markdown with clear section headings."
            )
            result = await router.async_route_and_generate(
                prompt, task_type="legal", max_cost=0.03
            )
            doc = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "jurisdiction": jurisdiction,
                "document": doc.strip(),
            }
        except Exception as exc:
            logger.error(f"ToS generation failed: {exc}")
            return {"status": "error", "error": str(exc)}
