"""
SupremeAI 2.0 — Celery Worker entrypoint.
বাংলা মন্তব্য: এই ফাইলটি Celery রানার দ্বারা অ্যাক্সেস করা হয়। এটি core.task_queue থেকে celery_app ইম্পোর্ট করে এবং এক্সপোজ করে।
"""

from core.task_queue import celery_app


# Expose the app for the Celery command-line interface
app = celery_app
