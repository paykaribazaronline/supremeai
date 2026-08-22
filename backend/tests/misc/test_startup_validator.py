import pytest

from core.startup_validator import StartupValidator


@pytest.mark.anyio
async def test_startup_validator_fails_when_app_name_empty(monkeypatch):
    from core.config import settings

    monkeypatch.setattr(settings, "app_name", "", raising=False)

    with pytest.raises(ValueError):
        await StartupValidator.validate()

    st = StartupValidator.last_status()
    assert st["success"] is False


@pytest.mark.anyio
async def test_startup_validator_passes(monkeypatch):
    from core.config import settings

    monkeypatch.setattr(settings, "app_name", "SupremeAI", raising=False)
    await StartupValidator.validate()

    st = StartupValidator.last_status()
    assert st["success"] is True
