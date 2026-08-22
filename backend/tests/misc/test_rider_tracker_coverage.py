"""
Coverage tests for services/rider_tracker.py.
Target: 100% line coverage.

রাইডার ট্র্যাকিং মডিউলের সকল ফাংশন ও শাখা কভার করা হয়েছে।
"""

from datetime import UTC, datetime
from unittest.mock import AsyncMock, MagicMock, patch

import pytest


class TestRiderStatusEnum:
    """Tests for RiderStatus enum."""

    def test_rider_status_values(self):
        """RiderStatus should have correct enum values."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import RiderStatus

            assert RiderStatus.AVAILABLE.value == "available"
            assert RiderStatus.ASSIGNED.value == "assigned"
            assert RiderStatus.PICKING_UP.value == "picking_up"
            assert RiderStatus.IN_TRANSIT.value == "in_transit"
            assert RiderStatus.DELIVERED.value == "delivered"
            assert RiderStatus.UNAVAILABLE.value == "unavailable"
            assert RiderStatus.OFFLINE.value == "offline"


class TestLocationDataclass:
    """Tests for Location dataclass."""

    def test_location_creation(self):
        """Location should be creatable with lat, lng, timestamp."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import Location

            now = datetime.now(UTC)
            loc = Location(latitude=23.8, longitude=90.4, timestamp=now)
            assert loc.latitude == 23.8
            assert loc.longitude == 90.4
            assert loc.timestamp == now


class TestRiderDataclass:
    """Tests for Rider dataclass."""

    def test_rider_creation(self):
        """Rider should be creatable with all fields."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import Location, Rider, RiderStatus

            now = datetime.now(UTC)
            loc = Location(latitude=23.8, longitude=90.4, timestamp=now)
            rider = Rider(
                rider_id="rider1",
                name="Test",
                phone="12345",
                vehicle_type="motorcycle",
                status=RiderStatus.AVAILABLE,
                current_location=loc,
                active_order=None,
            )
            assert rider.rider_id == "rider1"


class TestOrderDataclass:
    """Tests for Order dataclass."""

    def test_order_creation(self):
        """Order should be creatable with all fields."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import Location, Order

            now = datetime.now(UTC)
            pickup = Location(latitude=23.8, longitude=90.4, timestamp=now)
            dropoff = Location(latitude=23.9, longitude=90.5, timestamp=now)
            order = Order(
                order_id="order1",
                customer_id="cust1",
                pickup_location=pickup,
                dropoff_location=dropoff,
                assigned_rider=None,
                status="pending",
                created_at=now,
            )
            assert order.status == "pending"


class TestLocationTracker:
    """Tests for LocationTracker."""

    @pytest.mark.asyncio
    async def test_update_location(self):
        """update_location should store and return Location."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import LocationTracker

            with patch("services.rider_tracker.get_cache") as mock_gc:
                mock_cache = AsyncMock()
                mock_gc.return_value = mock_cache
                tracker = LocationTracker()
                loc = await tracker.update_location("rider1", 23.8, 90.4)
                assert loc.latitude == 23.8
                assert loc.longitude == 90.4

    @pytest.mark.asyncio
    async def test_get_location_found(self):
        """get_location should return Location when data exists."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import LocationTracker

            with patch("services.rider_tracker.get_cache") as mock_gc:
                mock_cache = AsyncMock()
                mock_cache.get.return_value = {
                    "latitude": 23.8,
                    "longitude": 90.4,
                    "timestamp": datetime.now(UTC).isoformat(),
                }
                mock_gc.return_value = mock_cache
                tracker = LocationTracker()
                loc = await tracker.get_location("rider1")
                assert loc is not None
                assert loc.latitude == 23.8

    @pytest.mark.asyncio
    async def test_get_location_not_found(self):
        """get_location should return None when no data."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import LocationTracker

            with patch("services.rider_tracker.get_cache") as mock_gc:
                mock_cache = AsyncMock()
                mock_cache.get.return_value = None
                mock_gc.return_value = mock_cache
                tracker = LocationTracker()
                loc = await tracker.get_location("nonexistent")
                assert loc is None


class TestRouteOptimizer:
    """Tests for RouteOptimizer."""

    def test_haversine_distance(self):
        """haversine_distance should calculate correctly."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import Location, RouteOptimizer

            now = datetime.now(UTC)
            loc1 = Location(latitude=23.8, longitude=90.4, timestamp=now)
            loc2 = Location(latitude=23.6, longitude=90.5, timestamp=now)
            distance = RouteOptimizer.haversine_distance(loc1, loc2)
            assert distance > 0
            assert distance < 50

    def test_haversine_same_point(self):
        """haversine_distance should return 0 for same point."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import Location, RouteOptimizer

            now = datetime.now(UTC)
            loc = Location(latitude=23.8, longitude=90.4, timestamp=now)
            distance = RouteOptimizer.haversine_distance(loc, loc)
            assert distance == 0.0

    def test_estimate_eta(self):
        """estimate_eta should return minutes."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import RouteOptimizer

            eta = RouteOptimizer.estimate_eta(30.0, avg_speed_kmh=30.0)
            assert eta == 60

    @pytest.mark.asyncio
    async def test_find_nearest_rider(self):
        """find_nearest_rider should return the closest available rider."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import (
                Location,
                Rider,
                RiderStatus,
                RouteOptimizer,
            )

            now = datetime.now(UTC)
            loc_target = Location(latitude=23.8, longitude=90.4, timestamp=now)
            rider1 = Rider(
                rider_id="r1",
                name="R1",
                phone="1",
                vehicle_type="bike",
                status=RiderStatus.AVAILABLE,
                current_location=Location(23.81, 90.41, now),
                active_order=None,
            )
            rider2 = Rider(
                rider_id="r2",
                name="R2",
                phone="2",
                vehicle_type="bike",
                status=RiderStatus.AVAILABLE,
                current_location=Location(24.0, 91.0, now),
                active_order=None,
            )

            optimizer = RouteOptimizer.__new__(RouteOptimizer)
            # find_nearest_rider একটি async মেথড, তাই await করা প্রয়োজন
            result = await optimizer.find_nearest_rider({"r1": rider1, "r2": rider2}, loc_target)
            assert result is not None
            assert result.rider_id == "r1"

    @pytest.mark.asyncio
    async def test_find_nearest_rider_none_available(self):
        """find_nearest_rider should return None when no riders available."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import (
                Location,
                Rider,
                RiderStatus,
                RouteOptimizer,
            )

            now = datetime.now(UTC)
            loc_target = Location(23.8, 90.4, now)
            rider = Rider(
                rider_id="r1",
                name="R1",
                phone="1",
                vehicle_type="bike",
                status=RiderStatus.OFFLINE,
                current_location=Location(23.81, 90.41, now),
                active_order=None,
            )

            optimizer = RouteOptimizer.__new__(RouteOptimizer)
            # find_nearest_rider একটি async মেথড, তাই await করা প্রয়োজন
            result = await optimizer.find_nearest_rider({"r1": rider}, loc_target)
            assert result is None


class TestRiderTracker:
    """Tests for RiderTracker."""

    def test_init(self):
        """RiderTracker should initialize with sub-components."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import RiderTracker

            with patch("services.rider_tracker.get_cache") as mock_gc:
                mock_gc.return_value = MagicMock()
                tracker = RiderTracker()
                assert tracker.location is not None

    def test_track_event(self):
        """track_event should store event for user."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import RiderTracker

            with patch("services.rider_tracker.get_cache") as mock_gc:
                mock_gc.return_value = MagicMock()
                tracker = RiderTracker()
                tracker.track_event("user1", "order_assigned", {"order_id": "123"})
                events = tracker.get_user_events("user1")
                assert len(events) == 1
                assert events[0]["type"] == "order_assigned"

    def test_aggregate_metrics(self):
        """aggregate_metrics should return metrics dict."""
        with patch.dict("sys.modules", {"core.cache": MagicMock()}):
            from services.rider_tracker import RiderTracker

            with patch("services.rider_tracker.get_cache") as mock_gc:
                mock_gc.return_value = MagicMock()
                tracker = RiderTracker()
                metrics = tracker.aggregate_metrics()
                assert isinstance(metrics, dict)
