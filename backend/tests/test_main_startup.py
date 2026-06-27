import importlib
import sys
from types import SimpleNamespace
from unittest.mock import MagicMock


def _import_main_with_stubs():
    stub_modules = {
        "core.app": SimpleNamespace(app=MagicMock()),
        "core.config": SimpleNamespace(
            settings=SimpleNamespace(
                env="local",
                port=8000,
                host="127.0.0.1",
                validate_config=lambda: None,
            )
        ),
        "core.logging_config": SimpleNamespace(setup_logging=lambda: None),
        "database": SimpleNamespace(db=MagicMock()),
    }

    original_modules = {name: sys.modules.get(name) for name in stub_modules}
    sys.modules.update(stub_modules)
    try:
        if "backend.main" in sys.modules:
            del sys.modules["backend.main"]
        main = importlib.import_module("backend.main")
    finally:
        for name, module in original_modules.items():
            if module is None:
                sys.modules.pop(name, None)
            else:
                sys.modules[name] = module
    return main


def test_bootstrap_supabase_schema_if_configured_calls_bootstrap(monkeypatch):
    main = _import_main_with_stubs()
    monkeypatch.setenv("SUPABASE_DATABASE_URL_POOLER", "postgresql://pooler_user:pooler_pass@localhost:6543/postgres")
    fake_supabase_db = MagicMock()
    main.supabase_db = fake_supabase_db

    main.bootstrap_supabase_schema_if_configured()

    fake_supabase_db.bootstrap_schema.assert_called_once()


def test_bootstrap_supabase_schema_if_not_configured_does_nothing(monkeypatch):
    main = _import_main_with_stubs()
    monkeypatch.delenv("SUPABASE_DATABASE_URL", raising=False)
    monkeypatch.delenv("SUPABASE_DATABASE_URL_POOLER", raising=False)
    fake_supabase_db = MagicMock()
    main.supabase_db = fake_supabase_db

    main.bootstrap_supabase_schema_if_configured()

    fake_supabase_db.bootstrap_schema.assert_not_called()
