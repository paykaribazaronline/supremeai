# 📄 ফাইল: backend/tests/core/test_enum_guard.py

**প্রকার:** .py  
**সাইজ:** 3,068 বাইট  
**আপডেট:** 2026-07-09T10:27:17.509496

---

## কোড

```py
import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from core.enum_guard import EnumMismatchError, guard_enum, run_enum_guards


class TestGuardEnumSuccess:
    @pytest.mark.anyio
    async def test_guard_enum_matching_labels(self):
        mock_conn = AsyncMock()
        mock_result = MagicMock()
        mock_result.all.return_value = [("active",), ("pending",)]
        mock_conn.execute = AsyncMock(return_value=mock_result)

        mock_engine = MagicMock()
        mock_ctx = AsyncMock()
        mock_ctx.__aenter__.return_value = mock_conn
        mock_ctx.__aexit__.return_value = False
        mock_engine.connect.return_value = mock_ctx

        with patch("core.enum_guard.engine", mock_engine):
            from enum import Enum

            class TestEnum(Enum):
                ACTIVE = "active"
                PENDING = "pending"

            await guard_enum("test_enum", TestEnum)

    @pytest.mark.anyio
    async def test_guard_enum_db_not_found(self):
        mock_conn = AsyncMock()
        mock_result = MagicMock()
        mock_result.all.return_value = []
        mock_conn.execute = AsyncMock(return_value=mock_result)

        mock_engine = MagicMock()
        mock_ctx = AsyncMock()
        mock_ctx.__aenter__.return_value = mock_conn
        mock_ctx.__aexit__.return_value = False
        mock_engine.connect.return_value = mock_ctx

        with patch("core.enum_guard.engine", mock_engine):
            from enum import Enum

            class TestEnum(Enum):
                ACTIVE = "active"

            await guard_enum("test_enum", TestEnum)

    @pytest.mark.anyio
    async def test_guard_enum_db_connection_error(self):
        mock_engine = MagicMock()
        mock_engine.connect.side_effect = Exception("DB connection failed")

        with patch("core.enum_guard.engine", mock_engine):
            from enum import Enum

            class TestEnum(Enum):
                ACTIVE = "active"

            await guard_enum("test_enum", TestEnum)

    @pytest.mark.anyio
    async def test_guard_enum_mismatch_raises(self):
        mock_conn = AsyncMock()
        mock_result = MagicMock()
        mock_result.all.return_value = [("active",), ("archived",)]
        mock_conn.execute = AsyncMock(return_value=mock_result)

        mock_engine = MagicMock()
        mock_ctx = AsyncMock()
        mock_ctx.__aenter__.return_value = mock_conn
        mock_ctx.__aexit__.return_value = False
        mock_engine.connect.return_value = mock_ctx

        with patch("core.enum_guard.engine", mock_engine):
            from enum import Enum

            class TestEnum(Enum):
                ACTIVE = "active"
                PENDING = "pending"

            with pytest.raises(EnumMismatchError):
                await guard_enum("test_enum", TestEnum)


class TestRunEnumGuards:
    @pytest.mark.anyio
    async def test_run_enum_guards(self):
        with patch("core.enum_guard.guard_enum", new_callable=AsyncMock) as mock_guard:
            await run_enum_guards()
            assert mock_guard.call_count == 6

```