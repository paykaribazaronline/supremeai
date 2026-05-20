
import json
import os
from typing import Dict, List, Optional, Any
from datetime import datetime
from chat_classifier import ChatType

class DatabaseManager:
    def __init__(self, db_path: str = None):
        """
        ডাটাবেজ ম্যানেজার ইনিশিয়ালাইজ করে।

        Args:
            db_path: ডাটাবেজ ফাইলের পাথ (ডিফল্ট: বর্তমান ডিরেক্টরির data.json)
        """
        if db_path is None:
            # বর্তমান ডিরেক্টরির পাথ পেতে
            current_dir = os.path.dirname(os.path.abspath(__file__))
            db_path = os.path.join(current_dir, "data.json")

        self.db_path = db_path
        self._initialize_database()

    def _initialize_database(self):
        """ডাটাবেজ ফাইল ইনিশিয়ালাইজ করে বা লোড করে।"""
        if not os.path.exists(self.db_path):
            # নতুন ডাটাবেজ তৈরি করা
            self.data = {
                "rules": [],
                "plans": [],
                "commands": [],
                "chats": [],
                "confirmations": []
            }
            self._save_database()
        else:
            # বিদ্যমান ডাটাবেজ লোড করা
            self._load_database()

    def _load_database(self):
        """ডাটাবেজ ফাইল থেকে ডাটা লোড করে।"""
        try:
            with open(self.db_path, 'r', encoding='utf-8') as f:
                self.data = json.load(f)
        except (json.JSONDecodeError, FileNotFoundError):
            # ফাইল ক্ষতিগ্রস্ত হলে নতুন ডাটাবেজ তৈরি করা
            self.data = {
                "rules": [],
                "plans": [],
                "commands": [],
                "chats": [],
                "confirmations": []
            }
            self._save_database()

    def _save_database(self):
        """ডাটাবেজ ফাইলে ডাটা সেভ করে।"""
        with open(self.db_path, 'w', encoding='utf-8') as f:
            json.dump(self.data, f, ensure_ascii=False, indent=2)

    def save_chat(self, user_id: str, message: str, is_admin: bool = False) -> str:
        """
        একটি চ্যাট মেসেজ সেভ করে।

        Args:
            user_id: ইউজার আইডি
            message: চ্যাট মেসেজ
            is_admin: ইউজার কি এডমিন কিনা (ডিফল্ট: False)

        Returns:
            str: চ্যাট আইডি
        """
        chat_id = f"chat_{datetime.now().strftime('%Y%m%d%H%M%S%f')}"

        chat_entry = {
            "id": chat_id,
            "user_id": user_id,
            "message": message,
            "is_admin": is_admin,
            "timestamp": datetime.now().isoformat()
        }

        self.data["chats"].append(chat_entry)
        self._save_database()

        return chat_id

    def save_rule(self, chat_id: str, content: str, confidence: float, user_id: str) -> str:
        """
        একটি রুল সেভ করে।

        Args:
            chat_id: চ্যাট আইডি
            content: রুল কন্টেন্ট
            confidence: কনফিডেন্স স্কোর
            user_id: ইউজার আইডি

        Returns:
            str: রুল আইডি
        """
        rule_id = f"rule_{datetime.now().strftime('%Y%m%d%H%M%S%f')}"

        rule_entry = {
            "id": rule_id,
            "chat_id": chat_id,
            "content": content,
            "confidence": confidence,
            "created_by": user_id,
            "created_at": datetime.now().isoformat(),
            "active": True
        }

        self.data["rules"].append(rule_entry)
        self._save_database()

        return rule_id

    def save_plan(self, chat_id: str, content: str, confidence: float, user_id: str) -> str:
        """
        একটি প্ল্যান সেভ করে।

        Args:
            chat_id: চ্যাট আইডি
            content: প্ল্যান কন্টেন্ট
            confidence: কনফিডেন্স স্কোর
            user_id: ইউজার আইডি

        Returns:
            str: প্ল্যান আইডি
        """
        plan_id = f"plan_{datetime.now().strftime('%Y%m%d%H%M%S%f')}"

        plan_entry = {
            "id": plan_id,
            "chat_id": chat_id,
            "content": content,
            "confidence": confidence,
            "created_by": user_id,
            "created_at": datetime.now().isoformat(),
            "active": True
        }

        self.data["plans"].append(plan_entry)
        self._save_database()

        return plan_id

    def save_command(self, chat_id: str, content: str, confidence: float, user_id: str) -> str:
        """
        একটি কমান্ড সেভ করে।

        Args:
            chat_id: চ্যাট আইডি
            content: কমান্ড কন্টেন্ট
            confidence: কনফিডেন্স স্কোর
            user_id: ইউজার আইডি

        Returns:
            str: কমান্ড আইডি
        """
        command_id = f"cmd_{datetime.now().strftime('%Y%m%d%H%M%S%f')}"

        command_entry = {
            "id": command_id,
            "chat_id": chat_id,
            "content": content,
            "confidence": confidence,
            "created_by": user_id,
            "created_at": datetime.now().isoformat(),
            "active": True
        }

        self.data["commands"].append(command_entry)
        self._save_database()

        return command_id

    def save_confirmation(self, chat_id: str, item_type: str, item_id: str, 
                         confirmed: bool, user_id: str) -> str:
        """
        একটি কনফার্মেশন সেভ করে।

        Args:
            chat_id: চ্যাট আইডি
            item_type: আইটেম টাইপ (rule, plan, command)
            item_id: আইটেম আইডি
            confirmed: কনফার্ম করা হয়েছে কিনা
            user_id: ইউজার আইডি

        Returns:
            str: কনফার্মেশন আইডি
        """
        confirmation_id = f"conf_{datetime.now().strftime('%Y%m%d%H%M%S%f')}"

        confirmation_entry = {
            "id": confirmation_id,
            "chat_id": chat_id,
            "item_type": item_type,
            "item_id": item_id,
            "confirmed": confirmed,
            "confirmed_by": user_id,
            "confirmed_at": datetime.now().isoformat()
        }

        self.data["confirmations"].append(confirmation_entry)
        self._save_database()

        return confirmation_id

    def get_all_rules(self, active_only: bool = True) -> List[Dict[str, Any]]:
        """
        সকল রুল রিটার্ন করে।

        Args:
            active_only: শুধুমাত্র সক্রিয় রুল রিটার্ন করবে কিনা (ডিফল্ট: True)

        Returns:
            List[Dict[str, Any]]: রুলের তালিকা
        """
        if active_only:
            return [rule for rule in self.data["rules"] if rule.get("active", True)]
        return self.data["rules"]

    def get_all_plans(self, active_only: bool = True) -> List[Dict[str, Any]]:
        """
        সকল প্ল্যান রিটার্ন করে।

        Args:
            active_only: শুধুমাত্র সক্রিয় প্ল্যান রিটার্ন করবে কিনা (ডিফল্ট: True)

        Returns:
            List[Dict[str, Any]]: প্ল্যানের তালিকা
        """
        if active_only:
            return [plan for plan in self.data["plans"] if plan.get("active", True)]
        return self.data["plans"]

    def get_all_commands(self, active_only: bool = True) -> List[Dict[str, Any]]:
        """
        সকল কমান্ড রিটার্ন করে।

        Args:
            active_only: শুধুমাত্র সক্রিয় কমান্ড রিটার্ন করবে কিনা (ডিফল্ট: True)

        Returns:
            List[Dict[str, Any]]: কমান্ডের তালিকা
        """
        if active_only:
            return [cmd for cmd in self.data["commands"] if cmd.get("active", True)]
        return self.data["commands"]

    def get_chat_history(self, user_id: str = None, limit: int = 100) -> List[Dict[str, Any]]:
        """
        চ্যাট হিস্ট্রি রিটার্ন করে।

        Args:
            user_id: ইউজার আইডি (যদি দেওয়া হয়, শুধুমাত্র ওই ইউজারের চ্যাট রিটার্ন করবে)
            limit: সর্বোচ্চ কতটি চ্যাট রিটার্ন করবে (ডিফল্ট: 100)

        Returns:
            List[Dict[str, Any]]: চ্যাটের তালিকা
        """
        chats = self.data["chats"]

        if user_id:
            chats = [chat for chat in chats if chat["user_id"] == user_id]

        # টাইমস্ট্যাম্প অনুযায়ী সর্ট করা (নতুন থেকে পুরনো)
        chats = sorted(chats, key=lambda x: x["timestamp"], reverse=True)

        return chats[:limit]

    def get_item_by_id(self, item_type: str, item_id: str) -> Optional[Dict[str, Any]]:
        """
        আইডি দিয়ে একটি আইটেম খুঁজে বের করে।

        Args:
            item_type: আইটেম টাইপ (rule, plan, command)
            item_id: আইটেম আইডি

        Returns:
            Optional[Dict[str, Any]]: আইটেম যদি পাওয়া যায়, অন্যথায় None
        """
        if item_type == "rule":
            items = self.data["rules"]
        elif item_type == "plan":
            items = self.data["plans"]
        elif item_type == "command":
            items = self.data["commands"]
        else:
            return None

        for item in items:
            if item["id"] == item_id:
                return item

        return None

    def update_item_status(self, item_type: str, item_id: str, active: bool) -> bool:
        """
        একটি আইটেমের স্ট্যাটাস আপডেট করে।

        Args:
            item_type: আইটেম টাইপ (rule, plan, command)
            item_id: আইটেম আইডি
            active: সক্রিয় কিনা

        Returns:
            bool: আপডেট সফল হলে True, অন্যথায় False
        """
        if item_type == "rule":
            items = self.data["rules"]
        elif item_type == "plan":
            items = self.data["plans"]
        elif item_type == "command":
            items = self.data["commands"]
        else:
            return False

        for item in items:
            if item["id"] == item_id:
                item["active"] = active
                item["updated_at"] = datetime.now().isoformat()
                self._save_database()
                return True

        return False

    def get_confirmation_history(self, item_id: str = None, chat_id: str = None) -> List[Dict[str, Any]]:
        """
        কনফার্মেশন হিস্ট্রি রিটার্ন করে।

        Args:
            item_id: আইটেম আইডি (যদি দেওয়া হয়, শুধুমাত্র ওই আইটেমের কনফার্মেশন রিটার্ন করবে)
            chat_id: চ্যাট আইডি (যদি দেওয়া হয়, শুধুমাত্র ওই চ্যাটের কনফার্মেশন রিটার্ন করবে)

        Returns:
            List[Dict[str, Any]]: কনফার্মেশনের তালিকা
        """
        confirmations = self.data["confirmations"]

        if item_id:
            confirmations = [conf for conf in confirmations if conf["item_id"] == item_id]

        if chat_id:
            confirmations = [conf for conf in confirmations if conf["chat_id"] == chat_id]

        # টাইমস্ট্যাম্প অনুযায়ী সর্ট করা (নতুন থেকে পুরনো)
        confirmations = sorted(confirmations, key=lambda x: x["confirmed_at"], reverse=True)

        return confirmations
