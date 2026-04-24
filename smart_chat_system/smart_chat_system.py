
import json
from typing import Dict, List, Optional, Tuple, Any
from datetime import datetime
from chat_classifier import ChatClassifier, ChatType
from database_manager import DatabaseManager

class SmartChatSystem:
    def __init__(self, db_path: str = None):
        """
        স্মার্ট চ্যাট সিস্টেম ইনিশিয়ালাইজ করে।

        Args:
            db_path: ডাটাবেজ ফাইলের পাথ (ডিফল্ট: বর্তমান ডিরেক্টরির data.json)
        """
        self.classifier = ChatClassifier()
        self.db_manager = DatabaseManager(db_path)

        # পেন্ডিং কনফার্মেশন ট্র্যাক করার জন্য ডিকশনারি
        self.pending_confirmations = {}

    def process_message(self, user_id: str, message: str, is_admin: bool = False) -> Dict[str, Any]:
        """
        একটি চ্যাট মেসেজ প্রসেস করে এবং ফলাফল রিটার্ন করে।

        Args:
            user_id: ইউজার আইডি
            message: চ্যাট মেসেজ
            is_admin: ইউজার কি এডমিন কিনা (ডিফল্ট: False)

        Returns:
            Dict[str, Any]: প্রসেস করা ফলাফল
        """
        # চ্যাট ডাটাবেজে সেভ করা
        chat_id = self.db_manager.save_chat(user_id, message, is_admin)

        # মেসেজ ক্লাসিফাই করা
        chat_type, confidence, reason = self.classifier.classify(message)

        # ফলাফল তৈরি করা
        result = {
            "chat_id": chat_id,
            "message": message,
            "chat_type": chat_type.value,
            "confidence": confidence,
            "reason": reason,
            "needs_confirmation": False,
            "item_id": None,
            "item_type": None
        }

        # যদি মেসেজটি রুল, প্ল্যান বা কমান্ড হয়
        if chat_type != ChatType.NORMAL:
            # কন্টেন্ট এক্সট্র্যাক্ট করা
            content_data = self.classifier.extract_content(message, chat_type)
            content = content_data.get("content", message)

            # আইটেম সেভ করা (কিন্তু কনফার্মেশনের জন্য পেন্ডিং রাখা)
            item_id = self._save_pending_item(chat_type, chat_id, content, confidence, user_id)

            # ফলাফল আপডেট করা
            result["needs_confirmation"] = True
            result["item_id"] = item_id
            result["item_type"] = chat_type.value
            result["content"] = content

            # পেন্ডিং কনফার্মেশন ট্র্যাক করা
            self.pending_confirmations[item_id] = {
                "chat_id": chat_id,
                "chat_type": chat_type.value,
                "content": content,
                "confidence": confidence,
                "user_id": user_id,
                "is_admin": is_admin
            }

        return result

    def _save_pending_item(self, chat_type: ChatType, chat_id: str, content: str, 
                          confidence: float, user_id: str) -> str:
        """
        একটি আইটেম সেভ করে যা কনফার্মেশনের জন্য পেন্ডিং আছে।

        Args:
            chat_type: চ্যাট টাইপ
            chat_id: চ্যাট আইডি
            content: আইটেম কন্টেন্ট
            confidence: কনফিডেন্স স্কোর
            user_id: ইউজার আইডি

        Returns:
            str: আইটেম আইডি
        """
        if chat_type == ChatType.RULE:
            return self.db_manager.save_rule(chat_id, content, confidence, user_id)
        elif chat_type == ChatType.PLAN:
            return self.db_manager.save_plan(chat_id, content, confidence, user_id)
        elif chat_type == ChatType.COMMAND:
            return self.db_manager.save_command(chat_id, content, confidence, user_id)
        else:
            return None

    def confirm_item(self, item_id: str, confirmed: bool, user_id: str) -> Dict[str, Any]:
        """
        একটি আইটেম কনফার্ম বা প্রত্যাখ্যান করে।

        Args:
            item_id: আইটেম আইডি
            confirmed: কনফার্ম করা হয়েছে কিনা
            user_id: ইউজার আইডি

        Returns:
            Dict[str, Any]: কনফার্মেশন ফলাফল
        """
        # পেন্ডিং কনফার্মেশন চেক করা
        if item_id not in self.pending_confirmations:
            return {
                "success": False,
                "message": "আইটেমটি পেন্ডিং কনফার্মেশনে পাওয়া যায়নি",
                "item_id": item_id
            }

        # পেন্ডিং কনফার্মেশন ডাটা পাওয়া
        pending_data = self.pending_confirmations[item_id]
        chat_id = pending_data["chat_id"]
        item_type = pending_data["chat_type"]

        # কনফার্মেশন সেভ করা
        confirmation_id = self.db_manager.save_confirmation(
            chat_id, item_type, item_id, confirmed, user_id
        )

        # আইটেম স্ট্যাটাস আপডেট করা
        if confirmed:
            self.db_manager.update_item_status(item_type, item_id, True)
            status_message = f"{item_type} সফলভাবে কনফার্ম করা হয়েছে"
        else:
            self.db_manager.update_item_status(item_type, item_id, False)
            status_message = f"{item_type} প্রত্যাখ্যান করা হয়েছে"

        # পেন্ডিং কনফার্মেশন থেকে সরানো
        del self.pending_confirmations[item_id]

        return {
            "success": True,
            "message": status_message,
            "item_id": item_id,
            "item_type": item_type,
            "confirmed": confirmed,
            "confirmation_id": confirmation_id
        }

    def get_pending_confirmations(self, user_id: str = None) -> List[Dict[str, Any]]:
        """
        পেন্ডিং কনফার্মেশনের তালিকা রিটার্ন করে।

        Args:
            user_id: ইউজার আইডি (যদি দেওয়া হয়, শুধুমাত্র ওই ইউজারের পেন্ডিং কনফার্মেশন রিটার্ন করবে)

        Returns:
            List[Dict[str, Any]]: পেন্ডিং কনফার্মেশনের তালিকা
        """
        pending_items = []

        for item_id, data in self.pending_confirmations.items():
            if user_id and data["user_id"] != user_id:
                continue

            pending_items.append({
                "item_id": item_id,
                "chat_id": data["chat_id"],
                "item_type": data["chat_type"],
                "content": data["content"],
                "confidence": data["confidence"],
                "user_id": data["user_id"],
                "is_admin": data["is_admin"]
            })

        return pending_items

    def get_rules(self, active_only: bool = True) -> List[Dict[str, Any]]:
        """
        সকল রুল রিটার্ন করে।

        Args:
            active_only: শুধুমাত্র সক্রিয় রুল রিটার্ন করবে কিনা (ডিফল্ট: True)

        Returns:
            List[Dict[str, Any]]: রুলের তালিকা
        """
        return self.db_manager.get_all_rules(active_only)

    def get_plans(self, active_only: bool = True) -> List[Dict[str, Any]]:
        """
        সকল প্ল্যান রিটার্ন করে।

        Args:
            active_only: শুধুমাত্র সক্রিয় প্ল্যান রিটার্ন করবে কিনা (ডিফল্ট: True)

        Returns:
            List[Dict[str, Any]]: প্ল্যানের তালিকা
        """
        return self.db_manager.get_all_plans(active_only)

    def get_commands(self, active_only: bool = True) -> List[Dict[str, Any]]:
        """
        সকল কমান্ড রিটার্ন করে।

        Args:
            active_only: শুধুমাত্র সক্রিয় কমান্ড রিটার্ন করবে কিনা (ডিফল্ট: True)

        Returns:
            List[Dict[str, Any]]: কমান্ডের তালিকা
        """
        return self.db_manager.get_all_commands(active_only)

    def get_chat_history(self, user_id: str = None, limit: int = 100) -> List[Dict[str, Any]]:
        """
        চ্যাট হিস্ট্রি রিটার্ন করে।

        Args:
            user_id: ইউজার আইডি (যদি দেওয়া হয়, শুধুমাত্র ওই ইউজারের চ্যাট রিটার্ন করবে)
            limit: সর্বোচ্চ কতটি চ্যাট রিটার্ন করবে (ডিফল্ট: 100)

        Returns:
            List[Dict[str, Any]]: চ্যাটের তালিকা
        """
        return self.db_manager.get_chat_history(user_id, limit)

    def get_item_by_id(self, item_type: str, item_id: str) -> Optional[Dict[str, Any]]:
        """
        আইডি দিয়ে একটি আইটেম খুঁজে বের করে।

        Args:
            item_type: আইটেম টাইপ (rule, plan, command)
            item_id: আইটেম আইডি

        Returns:
            Optional[Dict[str, Any]]: আইটেম যদি পাওয়া যায়, অন্যথায় None
        """
        return self.db_manager.get_item_by_id(item_type, item_id)

    def get_confirmation_history(self, item_id: str = None, chat_id: str = None) -> List[Dict[str, Any]]:
        """
        কনফার্মেশন হিস্ট্রি রিটার্ন করে।

        Args:
            item_id: আইটেম আইডি (যদি দেওয়া হয়, শুধুমাত্র ওই আইটেমের কনফার্মেশন রিটার্ন করবে)
            chat_id: চ্যাট আইডি (যদি দেওয়া হয়, শুধুমাত্র ওই চ্যাটের কনফার্মেশন রিটার্ন করবে)

        Returns:
            List[Dict[str, Any]]: কনফার্মেশনের তালিকা
        """
        return self.db_manager.get_confirmation_history(item_id, chat_id)
