# 📄 ফাইল: backend/tests/core/test_enum_guard.py

**প্রকার:** .py  
**সাইজ:** 1,817 বাইট  
**আপডেট:** 2026-07-07T16:04:55.535453

---

## কোড

```py
import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from core.enum_guard import EnumMismatchError, guard_enum, run_enum_guards

@pytest.mark.anyio
async def test_guard_enum_db_not_found():
    # Mock the database connection and result with empty list
    mock_conn = AsyncMock()
    mock_result = MagicMock()
    mock_result.all.return_value = []  # DB has no such enum
    mock_conn.execute = AsyncMock(return_value=mock_result)
    
    mock_engine = AsyncMock()
    mock_engine.connect.return_value.__aenter__.return_value = mock_conn
    
    with patch('core.enum_guard.engine', mock_engine):
        from enum import Enum
        class TestEnum(Enum):
            ACTIVE = 'active'
        
        # Should log a warning and return without raising
        await guard_enum('test_enum', TestEnum)
        # No assertion needed, just ensure no exception

@pytest.mark.anyio
async def test_guard_enum_db_connection_error():
    # Mock the database connection to raise an exception
    mock_engine = AsyncMock()
    # Make the connect method raise an exception
    mock_engine.connect.side_effect = Exception("DB connection failed")
    
    with patch('core.enum_guard.engine', mock_engine):
        from enum import Enum
        class TestEnum(Enum):
            ACTIVE = 'active'
        
        # Should log a warning and return without raising
        await guard_enum('test_enum', TestEnum)
        # No assertion needed

@pytest.mark.anyio
async def test_run_enum_guards():
    # Mock each guard_enum call to avoid actual DB calls
    with patch('core.enum_guard.guard_enum', new_callable=AsyncMock) as mock_guard:
        await run_enum_guards()
        # Ensure guard_enum was called for each enum
        assert mock_guard.call_count == 6  # Because there are 6 enums in run_enum_guards

```