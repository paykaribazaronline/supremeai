# বাংলা মন্তব্য: Anthropic Constitutional AI প্যাটার্ন ইমপ্লিমেন্টেশন।
# মডেলের রেসপন্স ইউজারের কাছে পাঠানোর আগে এটি নির্দিষ্ট কিছু প্রিন্সিপল অনুযায়ী যাচাই করবে।

from loguru import logger

from core.llm.llm_gateway import GatewayManager


CONSTITUTIONAL_PRINCIPLES = [
    "Avoid generating harmful or dangerous content",
    "Do not assist with illegal activities",
    "Protect user privacy and do not leak PII",
    "Be honest about AI limitations and do not hallucinate facts",
]


class PromptFirewall:
    def __init__(self, gateway: GatewayManager = None):
        self.gateway = gateway or GatewayManager()
        self.cheap_model = "gemini/gemini-2.5-flash"  # Free tier for quick critique

    async def constitutional_filter(self, response_text: str) -> tuple[str, bool]:
        """
        Anthropic Constitutional AI pattern.
        Returns: (filtered_response, was_revised)
        """
        for principle in CONSTITUTIONAL_PRINCIPLES:
            try:
                # 1. Critique phase
                critique_prompt = f"Does the following response violate this principle: '{principle}'?\n\nResponse:\n{response_text}\n\nAnswer ONLY with 'YES' or 'NO'."

                critique_response = await self.gateway.acompletion(prompt=critique_prompt, model=self.cheap_model)

                critique_text = critique_response.get("text", "").strip().upper()

                if "YES" in critique_text:
                    logger.warning(f"Constitutional AI triggered on principle: '{principle}'")

                    # 2. Revision phase
                    revision_prompt = f"The following response violates the principle: '{principle}'. Please revise it to be compliant while preserving the original intent.\n\nResponse:\n{response_text}"
                    revised_response = await self.gateway.acompletion(prompt=revision_prompt, model=self.cheap_model)

                    return revised_response.get("text", response_text), True

            except Exception as e:  # noqa: BLE001
                logger.error(f"Error during constitutional filtering: {e}")
                # Fall open on error to not block execution
                continue

        return response_text, False


# Singleton instance
firewall = PromptFirewall()
