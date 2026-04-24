
import re
from typing import Dict, List, Optional, Tuple
from enum import Enum

class ChatType(Enum):
    RULE = "rule"
    PLAN = "plan"
    COMMAND = "command"
    NORMAL = "normal"

class ChatClassifier:
    def __init__(self):
        # রুলস ডিটেক্ট করার জন্য প্যাটার্ন
        self.rule_patterns = [
            r"সবসময়|সবসময়ের জন্য|আমাদের নিয়ম|নিয়ম হল|নিয়মাবলী|প্রতিবার|যখনই",
            r"always|must|should|never|rule|policy|guideline|requirement",
            r"আমাদের পলিসি|পলিসি অনুযায়ী|নির্দেশনা|নির্দেশিকা|প্রয়োজনীয়তা",
            r"policy|guideline|instruction|requirement|mandatory"
        ]

        # প্ল্যান ডিটেক্ট করার জন্য প্যাটার্ন
        self.plan_patterns = [
            r"পরিকল্পনা|পরিকল্পনা অনুযায়ী|পরবর্তী ধাপ|ধাপসমূহ|কর্মপরিকল্পনা|কৌশল",
            r"plan|strategy|roadmap|timeline|milestone|action plan|next step|steps",
            r"ভবিষ্যতে|আগামী|পরবর্তী|পরবর্তীতে|সময়সূচী|সময়রেখা",
            r"future|upcoming|next|schedule|timeline|deadline|target"
        ]

        # কমান্ড ডিটেক্ট করার জন্য প্যাটার্ন
        self.command_patterns = [
            r"কর|করো|করুন|করতে হবে|শুরু কর|শুরু করুন|বন্ধ কর|বন্ধ করুন|চালাও|চালু কর",
            r"do|execute|run|start|stop|begin|end|terminate|initiate|launch",
            r"পাঠাও|পাঠান|দেখাও|দেখান|চেক কর|চেক করুন|যাচাই কর|যাচাই করুন",
            r"send|show|display|check|verify|validate|confirm|test|examine"
        ]

        # কনফিডেন্স স্কোর থ্রেশহোল্ড
        self.confidence_threshold = 0.6

        # প্রতিটি চ্যাট টাইপের জন্য ওজন
        self.type_weights = {
            ChatType.RULE: 1.2,
            ChatType.PLAN: 1.1,
            ChatType.COMMAND: 1.0
        }

    def classify(self, message: str) -> Tuple[ChatType, float, str]:
        """
        একটি চ্যাট মেসেজ ক্লাসিফাই করে এবং চ্যাট টাইপ, কনফিডেন্স স্কোর এবং কারণ রিটার্ন করে।

        Args:
            message: চ্যাট মেসেজ

        Returns:
            (ChatType, confidence_score, reason): চ্যাট টাইপ, কনফিডেন্স স্কোর এবং কারণ
        """
        message_lower = message.lower()

        # প্রতিটি টাইপের জন্য স্কোর ক্যালকুলেট করা
        rule_score = self._calculate_score(message_lower, self.rule_patterns, ChatType.RULE)
        plan_score = self._calculate_score(message_lower, self.plan_patterns, ChatType.PLAN)
        command_score = self._calculate_score(message_lower, self.command_patterns, ChatType.COMMAND)

        # সর্বোচ্চ স্কোর নির্ধারণ
        max_score = max(rule_score, plan_score, command_score)

        # কনফিডেন্স স্কোর এবং টাইপ নির্ধারণ
        if max_score < self.confidence_threshold:
            return ChatType.NORMAL, 1.0 - max_score, "এটি একটি সাধারণ চ্যাট মেসেজ"

        if max_score == rule_score:
            return ChatType.RULE, rule_score, "এই মেসেজে রুলস বা নিয়মাবলী রয়েছে"
        elif max_score == plan_score:
            return ChatType.PLAN, plan_score, "এই মেসেজে পরিকল্পনা বা প্ল্যান রয়েছে"
        else:
            return ChatType.COMMAND, command_score, "এই মেসেজে কমান্ড বা নির্দেশ রয়েছে"

    def _calculate_score(self, message: str, patterns: List[str], chat_type: ChatType) -> float:
        """
        একটি চ্যাট মেসেজের জন্য নির্দিষ্ট প্যাটার্ন সেটের স্কোর ক্যালকুলেট করে।

        Args:
            message: চ্যাট মেসেজ (ছোট হাতের অক্ষরে)
            patterns: মেসেজ চেক করার জন্য প্যাটার্ন সেট
            chat_type: চ্যাট টাইপ

        Returns:
            float: ক্যালকুলেট করা স্কোর
        """
        score = 0.0
        matched_patterns = []

        for pattern in patterns:
            matches = re.findall(pattern, message)
            if matches:
                score += len(matches) * 0.1
                matched_patterns.append(pattern)

        # চ্যাট টাইপের ওজন দিয়ে স্কোর গুণ করা
        score *= self.type_weights[chat_type]

        # স্কোর 1.0 এর বেশি হলে তা 1.0 এ সীমিত করা
        return min(score, 1.0)

    def extract_content(self, message: str, chat_type: ChatType) -> Dict[str, str]:
        """
        মেসেজ থেকে প্রাসঙ্গিক কন্টেন্ট এক্সট্র্যাক্ট করে।

        Args:
            message: চ্যাট মেসেজ
            chat_type: চ্যাট টাইপ

        Returns:
            Dict[str, str]: এক্সট্র্যাক্ট করা কন্টেন্ট
        """
        result = {"original_message": message, "type": chat_type.value}

        if chat_type == ChatType.RULE:
            result["content"] = self._extract_rule_content(message)
        elif chat_type == ChatType.PLAN:
            result["content"] = self._extract_plan_content(message)
        elif chat_type == ChatType.COMMAND:
            result["content"] = self._extract_command_content(message)
        else:
            result["content"] = message

        return result

    def _extract_rule_content(self, message: str) -> str:
        """মেসেজ থেকে রুল কন্টেন্ট এক্সট্র্যাক্ট করে।"""
        # রুল সম্পর্কিত কীওয়ার্ড ব্যবহার করে কন্টেন্ট এক্সট্র্যাক্ট করা
        rule_keywords = ["সবসময়", "সবসময়ের জন্য", "আমাদের নিয়ম", "নিয়ম হল", "নিয়মাবলী", 
                         "প্রতিবার", "যখনই", "always", "must", "should", "never", "rule", 
                         "policy", "guideline", "requirement", "mandatory"]

        sentences = re.split(r'[।.!?
]', message)
        rule_sentences = []

        for sentence in sentences:
            sentence = sentence.strip()
            if not sentence:
                continue

            for keyword in rule_keywords:
                if keyword.lower() in sentence.lower():
                    rule_sentences.append(sentence)
                    break

        return " ".join(rule_sentences) if rule_sentences else message

    def _extract_plan_content(self, message: str) -> str:
        """মেসেজ থেকে প্ল্যান কন্টেন্ট এক্সট্র্যাক্ট করে।"""
        # প্ল্যান সম্পর্কিত কীওয়ার্ড ব্যবহার করে কন্টেন্ট এক্সট্র্যাক্ট করা
        plan_keywords = ["পরিকল্পনা", "পরিকল্পনা অনুযায়ী", "পরবর্তী ধাপ", "ধাপসমূহ", "কর্মপরিকল্পনা", 
                         "কৌশল", "ভবিষ্যতে", "আগামী", "পরবর্তী", "সময়সূচী", "সময়রেখা",
                         "plan", "strategy", "roadmap", "timeline", "milestone", "action plan", 
                         "next step", "steps", "future", "upcoming", "schedule", "deadline"]

        sentences = re.split(r'[।.!?
]', message)
        plan_sentences = []

        for sentence in sentences:
            sentence = sentence.strip()
            if not sentence:
                continue

            for keyword in plan_keywords:
                if keyword.lower() in sentence.lower():
                    plan_sentences.append(sentence)
                    break

        return " ".join(plan_sentences) if plan_sentences else message

    def _extract_command_content(self, message: str) -> str:
        """মেসেজ থেকে কমান্ড কন্টেন্ট এক্সট্র্যাক্ট করে।"""
        # কমান্ড সম্পর্কিত কীওয়ার্ড ব্যবহার করে কন্টেন্ট এক্সট্র্যাক্ট করা
        command_keywords = ["কর", "করো", "করুন", "করতে হবে", "শুরু কর", "শুরু করুন", 
                           "বন্ধ কর", "বন্ধ করুন", "চালাও", "চালু কর", "পাঠাও", "পাঠান",
                           "দেখাও", "দেখান", "চেক কর", "চেক করুন", "যাচাই কর", "যাচাই করুন",
                           "do", "execute", "run", "start", "stop", "begin", "end", "terminate", 
                           "initiate", "launch", "send", "show", "display", "check", "verify", 
                           "validate", "confirm", "test", "examine"]

        sentences = re.split(r'[।.!?
]', message)
        command_sentences = []

        for sentence in sentences:
            sentence = sentence.strip()
            if not sentence:
                continue

            for keyword in command_keywords:
                if keyword.lower() in sentence.lower():
                    command_sentences.append(sentence)
                    break

        return " ".join(command_sentences) if command_sentences else message
