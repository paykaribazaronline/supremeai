import json
import pytest
from unittest.mock import AsyncMock, patch
from tools.collaborative_editor import CollaborativeEditor

# বাংলা মন্তব্য: লোকাল টেস্টিংয়ের জন্য Redis ক্লায়েন্ট এবং ফাংশনালিটিগুলো মক করা হচ্ছে।
@pytest.fixture
def mock_redis():
    with patch("tools.collaborative_editor.redis.from_url") as mock_from_url:
        mock_redis_instance = AsyncMock()
        
        # Pub/Sub মক
        mock_pubsub = AsyncMock()
        mock_redis_instance.pubsub.return_value = mock_pubsub
        
        # hgetall (স্টেট ফেচ) মক
        mock_redis_instance.hgetall.return_value = {}
        
        mock_from_url.return_value = mock_redis_instance
        yield mock_redis_instance

@pytest.mark.anyio
async def test_connect_and_disconnect_client(mock_redis):
    # বাংলা মন্তব্য: ক্লায়েন্টের কানেকশন এবং ডিসকানেক্ট যাচাই করা হচ্ছে।
    editor = CollaborativeEditor()
    ws_mock = AsyncMock()
    
    await editor.connect_client("session1", "client1", ws_mock)
    assert "session1" in editor.local_sessions
    assert "client1" in editor.local_sessions["session1"]
    
    # কানেকশনের পর sync_state পাঠানো হয়েছে কি না তা চেক করা
    ws_mock.send_text.assert_called_once()
    
    await editor.disconnect_client("session1", "client1")
    assert "session1" not in editor.local_sessions

@pytest.mark.anyio
async def test_broadcast_delta_position_shifting(mock_redis):
    # বাংলা মন্তব্য: এডিটের ফলে কার্সরের শিফটিং লজিক এবং Redis-এ স্টেট পারসিস্টেন্স চেক করা হচ্ছে।
    editor = CollaborativeEditor()
    
    # মক ইনিশিয়াল স্টেট
    mock_redis.hgetall.return_value = {
        "document_state": "Hello World",
        "ai_cursor": json.dumps({"position": 6, "status": "idle"})
    }
    
    # ৭ নং পজিশনে "Awesome " টেক্সট ইনসার্ট করছি
    delta = {"insert": "Awesome ", "position": 6}
    await editor.broadcast_delta("session1", delta, sender_id="client2")
    
    # Redis State Update চেক করা হচ্ছে (hset কল হয়েছে কি না)
    mock_redis.hset.assert_called_once()
    args, kwargs = mock_redis.hset.call_args
    
    assert args[0] == "supremeai:state:session1"
    updates = kwargs["mapping"]
    
    assert updates["document_state"] == "Hello Awesome World"
    updated_cursor = json.loads(updates["ai_cursor"])
    assert updated_cursor["position"] == 14  # 6 + len("Awesome ")
    
    # Redis Publish চেক করা হচ্ছে
    mock_redis.publish.assert_called_once()
