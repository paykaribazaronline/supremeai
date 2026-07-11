# 📄 ফাইল: backend/tools/task_router.py

**প্রকার:** .py  
**সাইজ:** 2,677 বাইট  
**আপডেট:** 2026-07-11T13:36:50.180181

---

## কোড

```py
from loguru import logger
from tools.local_code_executor import LocalCodeExecutor
from tools.freebuff_client import FreebuffClient


class TaskRouter:
    """
    Cohesion Upgrade: এই ক্লাসটি বিভিন্ন টাস্ককে সঠিক এক্সিকিউটরের কাছে পাঠায়।
    এটি লোকাল কোড এক্সিকিউশন এবং এক্সটার্নাল টুল ডেলিগেশনের মধ্যে সিদ্ধান্ত নেয়।
    """

    def __init__(self):
        # ডকার ব্যবহার করে কোড চালানোর জন্য LocalCodeExecutor ইনস্ট্যান্স তৈরি করা হচ্ছে
        self.local_executor = LocalCodeExecutor(use_docker=True)
        # এক্সটার্নাল 'freebuff' টুলের জন্য FreebuffClient ইনস্ট্যান্স তৈরি করা হচ্ছে
        self.freebuff_client = FreebuffClient()
        logger.info("TaskRouter initialized with LocalCodeExecutor and FreebuffClient")

    async def route_task(self, task_type: str, payload: dict) -> dict:
        """
        টাস্কের ধরন অনুযায়ী সঠিক এক্সিকিউটরের কাছে ডেলিগেট করে।
        """
        if task_type == "execute_python_code":
            code = payload.get("code")
            if not code:
                return {"success": False, "error": "No code provided for execution."}

            logger.info(f"Routing task to LocalCodeExecutor: {code[:70]}...")
            # কোড এক্সিকিউশনের জন্য LocalCodeExecutor ব্যবহার করা হচ্ছে
            return await self.local_executor.execute_local_code(code)

        elif task_type == "delegate_to_freebuff":
            command_args = payload.get("command_args")
            if not command_args or not isinstance(command_args, list):
                return {"success": False, "error": "Invalid or no command_args provided for Freebuff delegation."}

            logger.info(f"Routing task to FreebuffClient with args: {command_args}")
            # এক্সটার্নাল টুল ডেলিগেশনের জন্য FreebuffClient ব্যবহার করা হচ্ছে
            return await self.freebuff_client.delegate_task(command_args)

        else:
            logger.warning(f"Unknown task type received: {task_type}")
            return {"success": False, "error": f"Unsupported task type: {task_type}"}

```