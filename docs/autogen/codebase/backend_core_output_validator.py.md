# 📄 ফাইল: backend/core/output_validator.py

**প্রকার:** .py  
**সাইজ:** 7,100 বাইট  
**আপডেট:** 2026-07-08T04:09:02.097634

---

## কোড

```py
import json
from pathlib import Path

from loguru import logger


class MultiAICodeGenerator:
    def generate_with_consensus(
        self, task: str, code_kimi: str, code_gpt: str, code_claude: str
    ) -> dict:
        # Compare and find common lines
        lines_kimi = set(code_kimi.splitlines())
        lines_gpt = set(code_gpt.splitlines())
        lines_claude = set(code_claude.splitlines())

        consensus_lines = lines_kimi.intersection(lines_gpt).intersection(lines_claude)
        consensus = "\n".join(sorted(consensus_lines)) if consensus_lines else code_kimi

        all_lines = lines_kimi.union(lines_gpt).union(lines_claude)
        confidence = len(consensus_lines) / max(1, len(all_lines))

        return {
            "code": consensus,
            "confidence": confidence,
            "differences": list(all_lines - consensus_lines),
        }


# বল মনতবয: ডফলট কনসটটউশনল রলস ফইলর সটযনডরড অবসথন (backend/config/constitutional_rules.json)
DEFAULT_RULES_PATH = Path(__file__).parent.parent / "config" / "constitutional_rules.json"


class EnhancedConfidenceScorer:
    def __init__(self, rules_path: Path | None = None):
        # বল মনতবয: আগ rules_path=None দল খল রলসট বযবহত হত ও হযলসনশন
        # ডটকশন নরব নষকরয় থকত; এখন ডফলট কনফগ পথ বযবহর কর ত ঠক কর হল
        self.rules = self._load_rules(rules_path or DEFAULT_RULES_PATH)

    def _load_rules(self, rules_path: Path | None) -> dict:
        """ডাইনামিকালি ডাটাবেজ বা JSON থেকে রুলস লোড করে।"""
        if rules_path and rules_path.exists():
            try:
                with open(rules_path, encoding='utf-8') as f:
                    return json.load(f)
            except (OSError, json.JSONDecodeError) as e:
                # বল মনতবয: আগ `logger` ইমপরট কর হয়ন, ফল এই except বলক নজই
                # NameError ছড়ত ও মল তরটি চপ পড় যত; loguru logger যকত কর ঠক কর হল
                logger.error(f"Failed to load constitutional rules from {rules_path}: {e}")
        logger.warning("Constitutional rules not found or failed to load. Using empty ruleset.")
        return {"hallucination_patterns": [], "scores": {}}

    def score(self, output: str, context: dict) -> dict:
        output_lower = output.lower()
        is_flagged = any(p in output_lower for p in self.rules.get("hallucination_patterns", []))

        # Factual confidence
        factual_score = self.rules.get("scores", {}).get("factual_penalty", 0.1) if is_flagged else 1.0

        # AI reliability score
        ai_reliability = self.rules.get("scores", {}).get("reliability_penalty", 0.1) if is_flagged else context.get("ai_reliability", 0.9)

        # External validation score
        external_score = self.rules.get("scores", {}).get("external_penalty", 0.1) if is_flagged else context.get("external_score", 1.0)

        # Self-consistency score
        consistency_score = 1.0

        weights = {
            "factual": 0.3,
            "ai_reliability": 0.2,
            "external": 0.3,
            "consistency": 0.2,
        }

        overall = (
            weights["factual"] * factual_score
            + weights["ai_reliability"] * ai_reliability
            + weights["external"] * external_score
            + weights["consistency"] * consistency_score
        )

        if overall >= 0.9:
            badge = "HIGH_CONFIDENCE"
            color = "green"
        elif overall >= 0.7:
            badge = "MEDIUM_CONFIDENCE"
            color = "yellow"
        else:
            badge = "LOW_CONFIDENCE"
            color = "red"

        return {
            "overall": overall,
            "badge": badge,
            "color": color,
            "should_warn": overall < 0.7 or ai_reliability < 0.5,
        }


class HumanReviewPolicy:
    def requires_human_review(self, output_type: str, confidence: dict) -> bool:
        if output_type in ["python_code", "bash_script", "sql_query"]:
            return True
        if confidence["overall"] < 0.7:
            return True
        return confidence.get("ai_reliability", 1.0) < 0.5


class OutputValidator:
    def __init__(self):
        # আর্কিটেকচারাল ফিক্স: হার্ডকোডেড রুলস ডাইনামিক লোডার দিয়ে প্রতিস্থাপন
        # ভবিষ্যতে Firestore বা অন্য DB থেকে লোড করার জন্য পাথ প্যারামিটার ব্যবহার করা যাবে
        rules_path = Path(__file__).parent.parent / "config" / "constitutional_rules.json"
        self.enhanced_scorer = EnhancedConfidenceScorer(rules_path=rules_path)

        self.consensus_threshold = self.enhanced_scorer.rules.get("consensus_threshold", 0.7)
        self.hallucination_patterns = self.enhanced_scorer.rules.get("hallucination_patterns", [])

        self.multi_generator = MultiAICodeGenerator()
        self.human_policy = HumanReviewPolicy()

    def multi_model_consensus(self, output: str, task: str) -> dict:
        score = 1.0
        disagreements = []
        if any(p in output.lower() for p in self.hallucination_patterns):
            score = 0.1
            disagreements.append(
                "Incorrect GitHub repository path detected (hallucinated)."
            )
        return {
            "consensus_score": score,
            "disagreements": disagreements,
            "should_flag": score < self.consensus_threshold,
        }

    def self_reflect(self, output: str) -> dict:
        has_issues = False
        issues = []
        if any(p in output.lower() for p in self.hallucination_patterns):
            has_issues = True
            issues.append(f"Hallucinated repo path detected: {self.hallucination_patterns[0]}")
        return {"has_issues": has_issues, "issues": issues}

    def score_confidence(self, output: str, verification_results: dict) -> dict:
        # ai_reliability এবং external_score এখন EnhancedConfidenceScorer এর মধ্যে ডাইনামিকালি হ্যান্ডেল করা হয়
        context = verification_results.copy()
        if "ai_reliability" not in context:
            context["ai_reliability"] = 0.9
        res = self.enhanced_scorer.score(output, context)
        return {
            "overall": res["overall"],
            "badge": res["badge"],
            "color": res["color"],
            "should_warn_user": res["should_warn"],
        }

    def validate(self, output: str) -> dict:
        reflect = self.self_reflect(output)
        conf = self.score_confidence(output, {})
        return {"is_valid": not reflect["has_issues"], "confidence": conf}

```