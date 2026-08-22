import hashlib
import hmac
import pytest
from unittest.mock import AsyncMock
from tools.social.telegram_bot import TelegramBotHandler
from tools.social.telegram_security import security_guard


@pytest.fixture
def bot_handler():
    handler = TelegramBotHandler()
    handler.bot_token = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
    handler.send_message = AsyncMock(return_value=True)
    handler.answer_callback_query = AsyncMock(return_value=True)
    return handler


def test_telegram_commands_dictionary():
    handler = TelegramBotHandler()
    assert "/start" in handler.COMMANDS
    assert "/app" in handler.COMMANDS
    assert "/quick" in handler.COMMANDS
    assert "/telemetry" in handler.COMMANDS
    assert "/help" in handler.COMMANDS

    assert "38ms" in handler.COMMANDS["/telemetry"]
    assert "Mini App" in handler.COMMANDS["/app"]


def test_telegram_sync_handle_message():
    handler = TelegramBotHandler()
    assert "Mini App" in handler.handle_message("/app")
    assert "Quick Actions" in handler.handle_message("/quick")
    assert "38ms" in handler.handle_message("/telemetry")


@pytest.mark.asyncio
async def test_handle_update_telemetry_command(bot_handler):
    update = {
        "message": {
            "chat": {"id": 12345},
            "from": {"id": 12345, "username": "testuser"},
            "text": "/telemetry",
        }
    }
    await bot_handler.handle_update(update)
    bot_handler.send_message.assert_called_once()
    args, kwargs = bot_handler.send_message.call_args
    assert "Live Swarm Telemetry" in args[1]
    assert "38ms" in args[1]


@pytest.mark.asyncio
async def test_handle_update_app_command(bot_handler):
    update = {
        "message": {
            "chat": {"id": 12345},
            "from": {"id": 12345, "username": "testuser"},
            "text": "/app",
        }
    }
    await bot_handler.handle_update(update)
    bot_handler.send_message.assert_called_once()
    args, kwargs = bot_handler.send_message.call_args
    assert "Mini App" in args[1]
    assert "reply_markup" in kwargs
    assert "web_app" in str(kwargs["reply_markup"])


@pytest.mark.asyncio
async def test_handle_update_quick_actions_callback(bot_handler):
    update = {
        "callback_query": {
            "id": "cb-123",
            "data": "quick_self_healer",
            "message": {"chat": {"id": 12345}},
        }
    }
    await bot_handler.handle_update(update)
    bot_handler.answer_callback_query.assert_called_once_with("cb-123")
    bot_handler.send_message.assert_called_once()
    args, _ = bot_handler.send_message.call_args
    assert "Self-Healer Autonomous Diagnosis" in args[1]


@pytest.mark.asyncio
async def test_handle_update_kb_search(bot_handler):
    update = {
        "message": {
            "chat": {"id": 12345},
            "from": {"id": 12345, "username": "testuser"},
            "text": "/kb browser",
        }
    }
    await bot_handler.handle_update(update)
    bot_handler.send_message.assert_called_once()
    args, _ = bot_handler.send_message.call_args
    assert "Autonomous Browser Suite" in args[1]


def test_telegram_security_webapp_validation():
    bot_token = "secret_bot_token_123"
    auth_date = "1700000000"
    user_json = '{"id":12345,"first_name":"Niloy"}'

    # Construct check string
    data_dict = {
        "auth_date": auth_date,
        "user": user_json,
    }
    check_string = f"auth_date={auth_date}\nuser={user_json}"
    secret_key = hmac.new(b"WebAppData", bot_token.encode(), hashlib.sha256).digest()
    valid_hash = hmac.new(secret_key, check_string.encode(), hashlib.sha256).hexdigest()

    init_data = f"auth_date={auth_date}&user={user_json}&hash={valid_hash}"

    valid, parsed = security_guard.validate_webapp_init_data(init_data, bot_token)
    assert valid is True
    assert parsed["auth_date"] == auth_date

    # Invalid hash test
    invalid_init_data = f"auth_date={auth_date}&user={user_json}&hash=invalid_hash"
    invalid_res, _ = security_guard.validate_webapp_init_data(invalid_init_data, bot_token)
    assert invalid_res is False
