
from typing import Dict, List, Tuple, Optional
import re
from datetime import datetime
import json

class PlanAnalyzer:
    def __init__(self):
        # প্ল্যান অ্যানালিসিসের জন্য প্যাটার্ন এবং কীওয়ার্ড
        self.action_keywords = {
            'তৈরি': ['তৈরি করা', 'তৈরি করব', 'তৈরি করতে হবে', 'তৈরি করতে চাই', 'তৈরি করা হবে'],
            'পরিবর্তন': ['পরিবর্তন করা', 'পরিবর্তন করব', 'পরিবর্তন করতে হবে', 'পরিবর্তন করতে চাই', 'পরিবর্তন করা হবে'],
            'মুছে ফেলা': ['মুছে ফেলা', 'মুছে ফেলব', 'মুছে ফেলতে হবে', 'মুছে ফেলতে চাই', 'মুছে ফেলা হবে'],
            'যোগ করা': ['যোগ করা', 'যোগ করব', 'যোগ করতে হবে', 'যোগ করতে চাই', 'যোগ করা হবে'],
            'আপডেট': ['আপডেট করা', 'আপডেট করব', 'আপডেট করতে হবে', 'আপডেট করতে চাই', 'আপডেট করা হবে'],
            'বন্ধ করা': ['বন্ধ করা', 'বন্ধ করব', 'বন্ধ করতে হবে', 'বন্ধ করতে চাই', 'বন্ধ করা হবে'],
            'চালু করা': ['চালু করা', 'চালু করব', 'চালু করতে হবে', 'চালু করতে চাই', 'চালু করা হবে'],
            'create': ['create', 'creating', 'will create', 'need to create', 'want to create'],
            'change': ['change', 'changing', 'will change', 'need to change', 'want to change'],
            'delete': ['delete', 'deleting', 'will delete', 'need to delete', 'want to delete'],
            'add': ['add', 'adding', 'will add', 'need to add', 'want to add'],
            'update': ['update', 'updating', 'will update', 'need to update', 'want to update'],
            'stop': ['stop', 'stopping', 'will stop', 'need to stop', 'want to stop'],
            'start': ['start', 'starting', 'will start', 'need to start', 'want to start']
        }

        # টাইমলাইন সংক্রান্ত কীওয়ার্ড
        self.timeline_keywords = {
            'আজ': ['আজ', 'আজকে', 'আজই'],
            'আগামীকাল': ['আগামীকাল', 'আগামীকালকে'],
            'এই সপ্তাহে': ['এই সপ্তাহে', 'এই সপ্তাহের মধ্যে', 'এই সপ্তাহের শেষে'],
            'পরের সপ্তাহে': ['পরের সপ্তাহে', 'পরের সপ্তাহের মধ্যে', 'পরের সপ্তাহের শেষে'],
            'এই মাসে': ['এই মাসে', 'এই মাসের মধ্যে', 'এই মাসের শেষে'],
            'পরের মাসে': ['পরের মাসে', 'পরের মাসের মধ্যে', 'পরের মাসের শেষে'],
            'today': ['today', 'this day'],
            'tomorrow': ['tomorrow', 'next day'],
            'this week': ['this week', 'within this week', 'by the end of this week'],
            'next week': ['next week', 'within next week', 'by the end of next week'],
            'this month': ['this month', 'within this month', 'by the end of this month'],
            'next month': ['next month', 'within next month', 'by the end of next month']
        }

        # সামঞ্জস্যতা স্কোর থ্রেশহোল্ড
        self.compatibility_threshold = 0.5

    def analyze_plan_compatibility(self, new_plan: str, existing_plans: List[Dict]) -> Dict:
        """
        নতুন প্ল্যানের সাথে বিদ্যমান প্ল্যানের সামঞ্জস্যতা বিশ্লেষণ করে।

        Args:
            new_plan: নতুন প্ল্যান
            existing_plans: বিদ্যমান প্ল্যানের তালিকা

        Returns:
            Dict: সামঞ্জস্যতা বিশ্লেষণ রিপোর্ট
        """
        if not existing_plans:
            return {
                "compatible": True,
                "score": 1.0,
                "message": "এটি প্রথম প্ল্যান, তাই কোন বিরোধ নেই",
                "conflicts": [],
                "recommendations": ["এই প্ল্যানটি সফলভাবে বাস্তবায়ন করা যেতে পারে"]
            }

        # নতুন প্ল্যান থেকে কীওয়ার্ড এক্সট্র্যাক্ট করা
        new_plan_keywords = self._extract_keywords(new_plan)

        # বিদ্যমান প্ল্যানগুলোর সাথে তুলনা করা
        compatibility_scores = []
        conflicts = []
        recommendations = []

        for existing_plan in existing_plans:
            existing_plan_content = existing_plan.get('content', '')
            existing_plan_keywords = self._extract_keywords(existing_plan_content)

            # সামঞ্জস্যতা স্কোর ক্যালকুলেট করা
            score = self._calculate_compatibility_score(new_plan_keywords, existing_plan_keywords)
            compatibility_scores.append(score)

            # বিরোধ চেক করা
            conflict = self._check_conflict(new_plan, existing_plan_content)
            if conflict:
                conflicts.append({
                    "existing_plan_id": existing_plan.get('id', ''),
                    "existing_plan_content": existing_plan_content,
                    "conflict_type": conflict['type'],
                    "conflict_description": conflict['description']
                })

        # গড় সামঞ্জস্যতা স্কোর ক্যালকুলেট করা
        avg_score = sum(compatibility_scores) / len(compatibility_scores) if compatibility_scores else 1.0

        # সামঞ্জস্যতা নির্ধারণ করা
        is_compatible = avg_score >= self.compatibility_threshold

        # সাজেশন তৈরি করা
        if not is_compatible:
            recommendations.append("নতুন প্ল্যানটি বিদ্যমান প্ল্যানগুলোর সাথে সামঞ্জস্যপূর্ণ নয়")
            recommendations.append("বিদ্যমান প্ল্যানগুলো পুনর্বিবেচনা করুন অথবা নতুন প্ল্যানটি সংশোধন করুন")
        else:
            recommendations.append("নতুন প্ল্যানটি বিদ্যমান প্ল্যানগুলোর সাথে সামঞ্জস্যপূর্ণ")
            recommendations.append("নতুন প্ল্যানটি সফলভাবে বাস্তবায়ন করা যেতে পারে")

        # ফলাফল রিটার্ন করা
        return {
            "compatible": is_compatible,
            "score": avg_score,
            "message": "নতুন প্ল্যানটি বিদ্যমান প্ল্যানগুলোর সাথে " + 
                      ("সামঞ্জস্যপূর্ণ" if is_compatible else "অসামঞ্জস্যপূর্ণ"),
            "conflicts": conflicts,
            "recommendations": recommendations
        }

    def predict_future_state(self, new_plan: str, existing_plans: List[Dict]) -> Dict:
        """
        নতুন প্ল্যান অনুযায়ী কাজ করলে সিস্টেমের পরবর্তী অবস্থান প্রেডিক্ট করে।

        Args:
            new_plan: নতুন প্ল্যান
            existing_plans: বিদ্যমান প্ল্যানের তালিকা

        Returns:
            Dict: প্রেডিক্টেড ফিউচার স্টেট
        """
        # নতুন প্ল্যান থেকে কীওয়ার্ড এক্সট্র্যাক্ট করা
        new_plan_keywords = self._extract_keywords(new_plan)

        # টাইমলাইন এক্সট্র্যাক্ট করা
        timeline = self._extract_timeline(new_plan)

        # অ্যাকশন আইটেম এক্সট্র্যাক্ট করা
        action_items = self._extract_action_items(new_plan)

        # সম্ভাব্য ফলাফল প্রেডিক্ট করা
        predicted_outcomes = self._predict_outcomes(new_plan, existing_plans)

        # রিস্ক অ্যাসেসমেন্ট
        risk_assessment = self._assess_risks(new_plan, existing_plans)

        # ফলাফল রিটার্ন করা
        return {
            "timeline": timeline,
            "action_items": action_items,
            "predicted_outcomes": predicted_outcomes,
            "risk_assessment": risk_assessment,
            "implementation_suggestions": self._generate_implementation_suggestions(new_plan, existing_plans)
        }

    def _extract_keywords(self, text: str) -> Dict[str, List[str]]:
        """টেক্সট থেকে কীওয়ার্ড এক্সট্র্যাক্ট করে।"""
        text_lower = text.lower()
        keywords = {
            'actions': [],
            'timeline': []
        }

        # অ্যাকশন কীওয়ার্ড এক্সট্র্যাক্ট করা
        for action, variants in self.action_keywords.items():
            for variant in variants:
                if variant in text_lower:
                    keywords['actions'].append(action)
                    break

        # টাইমলাইন কীওয়ার্ড এক্সট্র্যাক্ট করা
        for time_period, variants in self.timeline_keywords.items():
            for variant in variants:
                if variant in text_lower:
                    keywords['timeline'].append(time_period)
                    break

        return keywords

    def _calculate_compatibility_score(self, new_plan_keywords: Dict, existing_plan_keywords: Dict) -> float:
        """দুটি প্ল্যানের কীওয়ার্ডের ভিত্তিতে সামঞ্জস্যতা স্কোর ক্যালকুলেট করে।"""
        # অ্যাকশন কীওয়ার্ডের সামঞ্জস্যতা চেক করা
        action_compatibility = 0.0
        if new_plan_keywords['actions'] and existing_plan_keywords['actions']:
            common_actions = set(new_plan_keywords['actions']) & set(existing_plan_keywords['actions'])
            all_actions = set(new_plan_keywords['actions']) | set(existing_plan_keywords['actions'])
            action_compatibility = len(common_actions) / len(all_actions) if all_actions else 1.0
        elif not new_plan_keywords['actions'] or not existing_plan_keywords['actions']:
            action_compatibility = 1.0  # কোন অ্যাকশন নেই মানে কোন বিরোধ নেই

        # টাইমলাইন কীওয়ার্ডের সামঞ্জস্যতা চেক করা
        timeline_compatibility = 0.0
        if new_plan_keywords['timeline'] and existing_plan_keywords['timeline']:
            common_timeline = set(new_plan_keywords['timeline']) & set(existing_plan_keywords['timeline'])
            all_timeline = set(new_plan_keywords['timeline']) | set(existing_plan_keywords['timeline'])
            timeline_compatibility = len(common_timeline) / len(all_timeline) if all_timeline else 1.0
        elif not new_plan_keywords['timeline'] or not existing_plan_keywords['timeline']:
            timeline_compatibility = 1.0  # কোন টাইমলাইন নেই মানে কোন বিরোধ নেই

        # গড় সামঞ্জস্যতা স্কোর ক্যালকুলেট করা
        avg_compatibility = (action_compatibility + timeline_compatibility) / 2

        return avg_compatibility

    def _check_conflict(self, new_plan: str, existing_plan: str) -> Optional[Dict]:
        """দুটি প্ল্যানের মধ্যে বিরোধ চেক করে।"""
        new_plan_lower = new_plan.lower()
        existing_plan_lower = existing_plan.lower()

        # বিরোধী অ্যাকশন পেয়ার
        conflicting_action_pairs = [
            ('তৈরি', 'মুছে ফেলা'),
            ('চালু করা', 'বন্ধ করা'),
            ('create', 'delete'),
            ('start', 'stop')
        ]

        # বিরোধ চেক করা
        for action1, action2 in conflicting_action_pairs:
            # প্রথম অ্যাকশন নতুন প্ল্যানে এবং দ্বিতীয় অ্যাকশন বিদ্যমান প্ল্যানে আছে কিনা
            if (action1 in new_plan_keywords and action2 in existing_plan_keywords):
                return {
                    'type': 'action_conflict',
                    'description': f"নতুন প্ল্যানে '{action1}' অ্যাকশন আছে কিন্তু বিদ্যমান প্ল্যানে '{action2}' অ্যাকশন আছে"
                }

            # দ্বিতীয় অ্যাকশন নতুন প্ল্যানে এবং প্রথম অ্যাকশন বিদ্যমান প্ল্যানে আছে কিনা
            if (action2 in new_plan_keywords and action1 in existing_plan_keywords):
                return {
                    'type': 'action_conflict',
                    'description': f"নতুন প্ল্যানে '{action2}' অ্যাকশন আছে কিন্তু বিদ্যমান প্ল্যানে '{action1}' অ্যাকশন আছে"
                }

        # টাইমলাইন বিরোধ চেক করা
        new_timeline = self._extract_timeline(new_plan)
        existing_timeline = self._extract_timeline(existing_plan)

        if new_timeline and existing_timeline and new_timeline != existing_timeline:
            return {
                'type': 'timeline_conflict',
                'description': f"নতুন প্ল্যানের টাইমলাইন '{new_timeline}' কিন্তু বিদ্যমান প্ল্যানের টাইমলাইন '{existing_timeline}'"
            }

        # কোন বিরোধ পাওয়া যায়নি
        return None

    def _extract_timeline(self, text: str) -> str:
        """টেক্সট থেকে টাইমলাইন এক্সট্র্যাক্ট করে।"""
        text_lower = text.lower()

        for time_period, variants in self.timeline_keywords.items():
            for variant in variants:
                if variant in text_lower:
                    return time_period

        return "নির্দিষ্ট নয়"  # টাইমলাইন নির্দিষ্ট করা নেই

    def _extract_action_items(self, text: str) -> List[str]:
        """টেক্সট থেকে অ্যাকশন আইটেম এক্সট্র্যাক্ট করে।"""
        text_lower = text.lower()
        action_items = []

        # বাক্যগুলোতে বিভক্ত করা
        sentences = re.split(r'[।.!?
]', text)

        for sentence in sentences:
            sentence = sentence.strip()
            if not sentence:
                continue

            # প্রতিটি অ্যাকশন কীওয়ার্ড চেক করা
            for action, variants in self.action_keywords.items():
                for variant in variants:
                    if variant in text_lower:
                        action_items.append(sentence)
                        break

        return action_items

    def _predict_outcomes(self, new_plan: str, existing_plans: List[Dict]) -> List[str]:
        """নতুন প্ল্যান অনুযায়ী কাজ করলে সম্ভাব্য ফলাফল প্রেডিক্ট করে।"""
        outcomes = []

        # নতুন প্ল্যান থেকে অ্যাকশন আইটেম এক্সট্র্যাক্ট করা
        action_items = self._extract_action_items(new_plan)

        # প্রতিটি অ্যাকশন আইটেমের জন্য সম্ভাব্য ফলাফল প্রেডিক্ট করা
        for action_item in action_items:
            # অ্যাকশন টাইপ নির্ধারণ করা
            action_type = None
            for action, variants in self.action_keywords.items():
                for variant in variants:
                    if variant in action_item.lower():
                        action_type = action
                        break
                if action_type:
                    break

            # অ্যাকশন টাইপ অনুযায়ী সম্ভাব্য ফলাফল প্রেডিক্ট করা
            if action_type == 'তৈরি' or action_type == 'create':
                outcomes.append(f"নতুন সম্পদ বা সেবা তৈরি হবে")
            elif action_type == 'পরিবর্তন' or action_type == 'change':
                outcomes.append(f"বিদ্যমান সম্পদ বা সেবা পরিবর্তন হবে")
            elif action_type == 'মুছে ফেলা' or action_type == 'delete':
                outcomes.append(f"বিদ্যমান সম্পদ বা সেবা মুছে ফেলা হবে")
            elif action_type == 'যোগ করা' or action_type == 'add':
                outcomes.append(f"নতুন ফিচার বা ক্ষমতা যোগ করা হবে")
            elif action_type == 'আপডেট' or action_type == 'update':
                outcomes.append(f"বিদ্যমান ফিচার বা ক্ষমতা আপডেট করা হবে")
            elif action_type == 'বন্ধ করা' or action_type == 'stop':
                outcomes.append(f"বিদ্যমান প্রক্রিয়া বন্ধ করা হবে")
            elif action_type == 'চালু করা' or action_type == 'start':
                outcomes.append(f"নতুন প্রক্রিয়া চালু করা হবে")

        return outcomes

    def _assess_risks(self, new_plan: str, existing_plans: List[Dict]) -> Dict:
        """নতুন প্ল্যানের সম্ভাব্য ঝুঁকি মূল্যায়ন করে।"""
        risks = {
            "high": [],
            "medium": [],
            "low": []
        }

        # নতুন প্ল্যানের সাথে বিদ্যমান প্ল্যানগুলোর সামঞ্জস্যতা চেক করা
        compatibility_report = self.analyze_plan_compatibility(new_plan, existing_plans)

        # যদি প্ল্যানগুলো অসামঞ্জস্যপূর্ণ হয়
        if not compatibility_report['compatible']:
            risks['high'].append("নতুন প্ল্যানটি বিদ্যমান প্ল্যানগুলোর সাথে অসামঞ্জস্যপূর্ণ")

            # বিরোধগুলো ঝুঁকি হিসেবে যোগ করা
            for conflict in compatibility_report['conflicts']:
                if conflict['conflict_type'] == 'action_conflict':
                    risks['high'].append(f"অ্যাকশন বিরোধ: {conflict['conflict_description']}")
                elif conflict['conflict_type'] == 'timeline_conflict':
                    risks['medium'].append(f"টাইমলাইন বিরোধ: {conflict['conflict_description']}")
        else:
            # সামঞ্জস্যপূর্ণ প্ল্যানের জন্য কম ঝুঁকি
            risks['low'].append("নতুন প্ল্যানটি বিদ্যমান প্ল্যানগুলোর সাথে সামঞ্জস্যপূর্ণ")

        # টাইমলাইন ঝুঁকি চেক করা
        timeline = self._extract_timeline(new_plan)
        if timeline == "নির্দিষ্ট নয়":
            risks['medium'].append("প্ল্যানে কোন নির্দিষ্ট টাইমলাইন নেই")

        # অ্যাকশন আইটেম ঝুঁকি চেক করা
        action_items = self._extract_action_items(new_plan)
        if not action_items:
            risks['medium'].append("প্ল্যানে স্পষ্ট অ্যাকশন আইটেম নেই")

        return risks

    def _generate_implementation_suggestions(self, new_plan: str, existing_plans: List[Dict]) -> List[str]:
        """নতুন প্ল্যান বাস্তবায়নের জন্য সাজেশন তৈরি করে।"""
        suggestions = []

        # নতুন প্ল্যানের সাথে বিদ্যমান প্ল্যানগুলোর সামঞ্জস্যতা চেক করা
        compatibility_report = self.analyze_plan_compatibility(new_plan, existing_plans)

        # যদি প্ল্যানগুলো অসামঞ্জস্যপূর্ণ হয়
        if not compatibility_report['compatible']:
            suggestions.append("বিদ্যমান প্ল্যানগুলো পুনর্বিবেচনা করুন এবং নতুন প্ল্যানের সাথে সামঞ্জস্যপূর্ণ করুন")
            suggestions.append("বিরোধগুলো সমাধান করার জন্য একটি মিটিং আয়োজন করুন")
            suggestions.append("প্রয়োজনে নতুন প্ল্যানটি সংশোধন করুন")
        else:
            # সামঞ্জস্যপূর্ণ প্ল্যানের জন্য সাজেশন
            suggestions.append("নতুন প্ল্যানটি বাস্তবায়নের জন্য একটি টাইমলাইন তৈরি করুন")
            suggestions.append("প্রতিটি অ্যাকশন আইটেমের জন্য দায়িত্ব নির্ধারণ করুন")
            suggestions.append("অগ্রগতি ট্র্যাক করার জন্য নিয়মিত মিটিং আয়োজন করুন")

        # টাইমলাইন সংক্রান্ত সাজেশন
        timeline = self._extract_timeline(new_plan)
        if timeline == "নির্দিষ্ট নয়":
            suggestions.append("প্ল্যানের জন্য একটি নির্দিষ্ট টাইমলাইন নির্ধারণ করুন")

        # অ্যাকশন আইটেম সংক্রান্ত সাজেশন
        action_items = self._extract_action_items(new_plan)
        if not action_items:
            suggestions.append("প্ল্যানে স্পষ্ট অ্যাকশন আইটেম যোগ করুন")

        return suggestions
