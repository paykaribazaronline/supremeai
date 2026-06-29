import sys

import pytest

sys.path.append("../..")
from p2p.secure_tunnel import SecureTunnel


class TestSecureTunnel:
    @pytest.mark.asyncio
    async def test_create(self):
        tunnel = SecureTunnel()
        result = await tunnel.create("peer_a", "peer_b")
        assert result["status"] == "created"
        assert result["peer_a"] == "peer_a"
        assert result["peer_b"] == "peer_b"

    @pytest.mark.asyncio
    async def test_terminate(self):
        tunnel = SecureTunnel()
        await tunnel.terminate("tunnel_1")
        # Should not raise
