import asyncio
import random
from typing import Any

from loguru import logger

try:
    from playwright.async_api import ElementHandle, Page
except ImportError:
    # বাংলা মন্তব্য: মেইন ব্যাকএন্ড কন্টেইনারে playwright না থাকলে fallback setup
    Page = Any
    ElementHandle = Any


class HumanBehaviorSimulators:
    """
    মানুষের আচরণ সিমুলেট করার জন্য হেল্পার ক্লাস।
    এটি বট-ডিটেকশন বাইপাস করতে সাহায্য করে।
    """

    @staticmethod
    def _generate_bezier_points(start: tuple, end: tuple, steps: int = 20) -> list:
        """মানুষের হাতের সামান্য কাঁপুনি সিমুলেট করার জন্য Bezier পাথ পয়েন্ট জেনারেট করে।"""
        x1, y1 = start
        x2, y2 = end

        # র্যান্ডম কন্ট্রোল পয়েন্ট নিয়ে ন্যাচারাল কার্ভ তৈরি করা হচ্ছে
        control1_x = x1 + (x2 - x1) * random.uniform(0.1, 0.4)
        control1_y = y1 + (y2 - y1) * random.uniform(0.1, 0.3)
        control2_x = x1 + (x2 - x1) * random.uniform(0.6, 0.9)
        control2_y = y1 + (y2 - y1) * random.uniform(0.7, 0.9)

        points = []
        for i in range(steps):
            t = i / float(steps - 1)
            # Cubic Bezier ফর্মুলা
            x = (
                (1 - t) ** 3 * x1
                + 3 * (1 - t) ** 2 * t * control1_x
                + 3 * (1 - t) * t**2 * control2_x
                + t**3 * x2
            )
            y = (
                (1 - t) ** 3 * y1
                + 3 * (1 - t) ** 2 * t * control1_y
                + 3 * (1 - t) * t**2 * control2_y
                + t**3 * y2
            )
            points.append((x, y))
        return points

    @classmethod
    async def natural_mouse_move_and_click(cls, page: Page, selector: str):
        """মাউস কার্সারকে Bezier কার্ভ দিয়ে মুভ করিয়ে র্যান্ডম অফসেট ক্লিক করবে।"""
        try:
            element = await page.wait_for_selector(
                selector, state="visible", timeout=10000
            )
            box = await element.bounding_box()
            if not box:
                raise ValueError(f"Element {selector} has no layout bounding box.")

            # এলিমেন্টের সেন্টারে সামান্য র্যান্ডম অফসেট নিয়ে ক্লিক কোঅর্ডিনেট নির্ধারণ
            target_x = box["x"] + box["width"] / 2 + random.uniform(-5, 5)
            target_y = box["y"] + box["height"] / 2 + random.uniform(-5, 5)

            # এন্ট্রি ভেক্টর সিমুলেট করার জন্য র্যান্ডম শুরু পয়েন্ট নেওয়া হলো
            start_x = random.uniform(0, 100)
            start_y = random.uniform(0, 100)

            path = cls._generate_bezier_points(
                (start_x, start_y), (target_x, target_y), steps=random.randint(15, 30)
            )

            for x, y in path:
                await page.mouse.move(x, y)
                await asyncio.sleep(random.uniform(0.005, 0.015))  # মাইক্রো ডিলে

            await asyncio.sleep(random.uniform(0.1, 0.25))  # ক্লিকের আগে সামান্য থামা
            await page.mouse.click(target_x, target_y)
            logger.debug(f"Simulated natural human click on selector: {selector}")
        except Exception as e:  # noqa: BLE001
            logger.error(f"Human-like click failed on {selector}: {str(e)}")
            raise

    @classmethod
    async def natural_type(cls, page: Page, selector: str, text: str):
        """Gaussian ডিস্ট্রিবিউশন ডিলে ব্যবহার করে কিবোর্ড টাইপিং সিমুলেট করবে।"""
        try:
            element = await page.wait_for_selector(
                selector, state="visible", timeout=10000
            )
            await element.focus()
            await asyncio.sleep(random.uniform(0.15, 0.3))

            for char in text:
                await page.keyboard.type(char)
                # Gaussian ডিস্ট্রিবিউশন: Mean=100ms, StdDev=30ms
                delay = random.gauss(0.10, 0.03)
                # বাস্তবসম্মত বাউন্ডারি লিমিট (50ms থেকে 250ms)
                delay = max(0.05, min(delay, 0.25))
                await asyncio.sleep(delay)

            logger.debug(f"Simulated natural typing into selector: {selector}")
        except Exception as e:  # noqa: BLE001
            logger.error(f"Human-like typing failed on {selector}: {str(e)}")
            raise
