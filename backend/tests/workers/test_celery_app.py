import sys


sys.path.append("../..")
from workers.celery_app import app


def test_celery_app_exposed():
    assert app is not None
