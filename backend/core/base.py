from abc import ABC
from abc import abstractmethod
from typing import Any


class BaseSkill(ABC):
    """
    বাংলা মন্তব্য: সকল স্কিলের জন্য অ্যাবস্ট্র্যাক্ট বেস ক্লাস (The Contract)।
    প্রতিটি স্কিলকে অবশ্যই একটি async `execute` মেথড ইমপ্লিমেন্ট করতে হবে।
    """

    @abstractmethod
    async def execute(self, *args: Any, **kwargs: Any) -> Any:
        """
        বাংলা মন্তব্য: এই মেথডটি স্কিলের মূল লজিক ধারণ করবে।
        """
        pass

    @property
    def name(self) -> str:
        return self.__class__.__name__
