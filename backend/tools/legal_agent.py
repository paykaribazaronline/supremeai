from typing import Any

from loguru import logger


class LegalAgent:
    async def generate_document(self, doc_type: str, context: dict[str, str]) -> dict[str, Any]:
        logger.info(f"Generating legal document: {doc_type}")
        company = context.get("company_name", "[Company Name]")
        effective_date = context.get("effective_date", "[Date]")
        jurisdiction = context.get("jurisdiction", "State of California")
        doc = f"""
# {doc_type.upper()}
**Effective Date:** {effective_date}
**Jurisdiction:** {jurisdiction}

## 1. Introduction
Welcome to {company}. By accessing or using our services, you agree to be bound by these terms.

## 2. Definitions
- "Service" means the software platform provided by {company}.
- "User" means any individual or entity using the Service.

## 3. Liability Limitation
To the maximum extent permitted by law, {company} shall not be liable for indirect, incidental, or consequential damages.

## 4. Governing Law
These terms shall be governed by the laws of {jurisdiction}.
"""
        return {
            "status": "success",
            "type": doc_type,
            "document": doc.strip(),
        }

    async def extract_clauses(self, document_text: str) -> dict[str, Any]:
        logger.info("Extracting clauses from document...")
        clauses: dict[str, str] = {
            "liability": "Not explicitly stated.",
            "termination": "Not explicitly stated.",
            "governing_law": "Not explicitly stated.",
            "confidentiality": "Not explicitly stated.",
        }
        lower_text = document_text.lower()
        for key, phrase in [
            ("liability", ["liability", "liable", "damages", "indemnify"]),
            ("termination", ["terminate", "termination", "end of agreement"]),
            ("governing_law", ["governed by", "governing law", "jurisdiction"]),
            ("confidentiality", ["confidential", "non-disclosure", "nda"]),
        ]:
            for word in phrase:
                if word in lower_text:
                    idx = lower_text.index(word)
                    clauses[key] = document_text[max(0, idx - 50) : idx + 200].strip()
                    break
        return {
            "status": "success",
            "clauses": clauses,
        }
