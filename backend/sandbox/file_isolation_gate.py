# backend/sandbox/file_isolation_gate.py
import logging
import shutil
import uuid
from pathlib import Path
from typing import Any

# বাংলা মন্তব্য: রেন্ডার ডকার লেআউটের সাথে সামঞ্জস্যপূর্ণ রাখতে backend. ইম্পোর্ট রুট সরিয়ে দেওয়া হয়েছে
from sandbox.docker_sandbox import \
    DockerSandbox  # আপনার এক্সিস্টিং স্যান্ডবক্স ইঞ্জিন

logger = logging.getLogger("supremeai.sandbox.file_gate")

# আইসোলেটেড স্টেজিং এরিয়ার রুট পাথ ডিফাইন
SECURE_STAGING_DIR = Path("/tmp/supremeai_isolated_stage").resolve()


class FileIsolationGate:
    def __init__(self):
        self.sandbox = DockerSandbox()
        # নিশ্চিত করা যে স্টেজিং রুট ডিরেক্টরি এক্সিস্ট করে
        SECURE_STAGING_DIR.mkdir(parents=True, exist_ok=True)

    def execute_file_parsing_safely(
        self, raw_file_bytes: bytes, file_extension: str
    ) -> dict[str, Any]:
        """
        আপলোড করা ফাইলকে একটি র্যান্ডম আইসোলেটেড ডিরেক্টরিতে সাময়িক স্টোর করে
        ডকার স্যান্ডবক্সে এক্সিকিউট করায় এবং কাজ শেষে মেমোরি ও ডিস্ক সম্পূর্ণ ক্লিন করে।
        """
        # ১. ইউনিক ট্রানজেকশন আইডি এবং সেফ পাথ জেনারেশন (Path Traversal Protection)
        transaction_id = str(uuid.uuid4())
        session_dir = (SECURE_STAGING_DIR / transaction_id).resolve()

        try:
            session_dir.mkdir(parents=True, exist_ok=False)

            # ডিফেন্সিভ চেক: সেশন ডিরেক্টরিটি রুট স্টেজিং ডিরেক্টরির ভেতরেই আছে কিনা
            if not session_dir.is_relative_to(SECURE_STAGING_DIR):
                raise PermissionError("Suspicious path escape pattern blocked.")

            target_file_path = session_dir / f"input_target.{file_extension.strip('.')}"

            # ২. ফাইল ডিস্কে রাইট করা (আইসোলেটেড স্টেজিংয়ে)
            target_file_path.write_bytes(raw_file_bytes)
            logger.info(
                f"🔒 Isolated staging context locked for Transaction: {transaction_id}"
            )

            # ৩. ডকার স্যান্ডবক্সে রান করানোর জন্য মক কমান্ড/স্ক্রিপ্ট প্রিপারেশন
            # (বাস্তব ক্ষেত্রে এখানে কন্টেইনারের ভেতর একটি মিনিমাল পাইথন পার্সার রান হবে)
            sandbox_payload = {
                "script": "import os; print(f'File Size Processed inside Container: {os.path.getsize(\"/sandbox/target\")} bytes')",
                "bind_mount_source": str(target_file_path),
                "bind_mount_target": "/sandbox/target",
            }

            # ৪. ডকার কন্টেইনার স্পিন-আপ (এক্সিস্টিং স্যান্ডবক্স ইঞ্জিন ব্যবহার করে)
            logger.info(
                "🐋 Spawning isolated container runtime for payload execution..."
            )
            # আপনার docker_sandbox ইঞ্জিনের ইন্টারফেস অনুযায়ী এই কলটি এক্সিকিউট হবে
            sandbox_res = self.sandbox.run_safe_container(sandbox_payload)

            return {
                "success": True,
                "transaction_id": transaction_id,
                "sandbox_output": sandbox_res,
            }

        except Exception as e:
            logger.error(
                f"❌ Critical failure in Sandbox File Isolation Gate: {str(e)}"
            )
            return {
                "success": False,
                "error": f"Transaction isolation barrier failure: {str(e)}",
            }

        finally:
            # 🧹 ৫. কঠোর ক্লিনআপ গ্যারান্টি (Zero-Trace Policy)
            # কাজ সফল হোক বা ব্যর্থ, ট্রানজেকশন ফোল্ডার ডিস্ক থেকে সম্পূর্ণ মুছে ফেলা হবে
            if session_dir.exists():
                shutil.rmtree(session_dir)
                logger.info(
                    f"🧹 Vacuum cleanup complete for transaction workspace: {transaction_id}"
                )
