"""
SupremeAI Temporal Abstraction System
=====================================

Implements temporal abstraction for AI systems to:
- Understand and reason about time-based patterns
- Learn temporal hierarchies and relationships
- Predict future events based on temporal sequences
- Abstract away fine-grained temporal details into meaningful chunks
- Model long-term dependencies and causal relationships

Temporal abstraction is crucial for complex reasoning and planning.

Bengali:
সময়ের বিমূর্ততা সিস্টেম
সময়ভিত্তিক প্যাটার্ন বুঝতে এবং যুক্তি দেখানোর জন্য এআই সিস্টেম:
- সময়ের ক্রম এবং সম্পর্কগুলো শিখুন
- সময়ের ক্রম অনুসারে ভবিষ্যতের ঘটনা পূর্বাভাস দিন
- বিস্তারিত সময়ের তথ্যকে অর্থপূর্ণ অংশে বিমূর্ত করুন
- দীর্ঘমেয়াদী নির্ভরতা এবং কারণ-ক্রিয়া মডেল করুন
"""

import time
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any

import numpy as np


class TemporalGranularity(Enum):
    """Different levels of temporal granularity."""

    MILLISECOND = "millisecond"
    SECOND = "second"
    MINUTE = "minute"
    HOUR = "hour"
    DAY = "day"
    WEEK = "week"
    MONTH = "month"
    QUARTER = "quarter"
    YEAR = "year"
    CUSTOM = "custom"


class TemporalPatternType(Enum):
    """Types of temporal patterns."""

    PERIODIC = "periodic"
    TREND = "trend"
    SEASONAL = "seasonal"
    CYCLICAL = "cyclical"
    ANOMALOUS = "anomalous"
    TRANSIENT = "transient"


@dataclass
class TemporalEvent:
    """Represents a temporal event with timing and metadata."""

    timestamp: float  # Unix timestamp
    event_type: str
    event_data: dict[str, Any]
    duration: float | None = None  # Duration in seconds
    confidence: float = 1.0  # Confidence in the event
    priority: float = 0.5  # Priority level (0.0 to 1.0)

    def __post_init__(self):
        if self.duration and self.duration < 0:
            raise ValueError("Duration must be non-negative")


@dataclass
class TemporalPattern:
    """Represents an identified temporal pattern."""

    pattern_type: TemporalPatternType
    start_time: float
    end_time: float
    period: float | None = None  # For periodic patterns
    frequency: float | None = None  # Events per time unit
    strength: float = 0.0  # Strength of the pattern (0.0 to 1.0)
    description: str = ""
    confidence: float = 0.7


@dataclass
class TemporalAbstractionConfig:
    """Configuration for temporal abstraction system."""

    # Time window parameters
    short_term_window: int = 3600  # 1 hour in seconds
    medium_term_window: int = 86400  # 1 day in seconds
    long_term_window: int = 604800  # 1 week in seconds

    # Pattern detection parameters
    min_pattern_duration: int = 300  # 5 minutes minimum
    pattern_confidence_threshold: float = 0.6
    anomaly_threshold: float = 2.0  # Standard deviations for anomaly detection

    # Abstraction parameters
    abstraction_levels: int = 5  # Number of abstraction levels
    temporal_resolution: TemporalGranularity = TemporalGranularity.HOUR
    max_events_to_track: int = 10000

    # Prediction parameters
    prediction_horizon: int = 3600  # Predict next hour by default
    prediction_confidence_threshold: float = 0.7


class TemporalMemory:
    """
    Maintains temporal memory with different time scales.
    """

    def __init__(self, config: TemporalAbstractionConfig):
        self.config = config
        self.events: list[TemporalEvent] = []
        self.patterns: list[TemporalPattern] = []
        self.short_term_memory: list[TemporalEvent] = []
        self.medium_term_memory: list[TemporalEvent] = []
        self.long_term_memory: list[TemporalEvent] = []

        # Index for faster access
        self.event_index: dict[str, list[int]] = {}

        # Time tracking
        self.last_cleanup = time.time()

    def add_event(self, event: TemporalEvent):
        """Add an event to temporal memory."""
        # Add to main event list
        self.events.append(event)

        # Add to appropriate time window
        current_time = time.time()

        if current_time - event.timestamp < self.config.short_term_window:
            self.short_term_memory.append(event)
        elif current_time - event.timestamp < self.config.medium_term_window:
            self.medium_term_memory.append(event)
        else:
            self.long_term_memory.append(event)

        # Update index
        event_type = event.event_type
        if event_type not in self.event_index:
            self.event_index[event_type] = []
        self.event_index[event_type].append(len(self.events) - 1)

        # Periodic cleanup to maintain memory limits
        self._cleanup_old_events()

    def _cleanup_old_events(self):
        """Clean up old events to maintain memory limits."""
        current_time = time.time()

        # Only run cleanup periodically
        if current_time - self.last_cleanup < 3600:  # Once per hour
            return

        # Remove events beyond long term window
        cutoff_time = current_time - self.config.long_term_window
        self.events = [e for e in self.events if e.timestamp >= cutoff_time]

        # Rebuild index
        self.event_index = {}
        for i, event in enumerate(self.events):
            if event.event_type not in self.event_index:
                self.event_index[event.event_type] = []
            self.event_index[event.event_type].append(i)

        self.last_cleanup = current_time

    def get_events_in_range(
        self, start_time: float, end_time: float, event_type: str | None = None
    ) -> list[TemporalEvent]:
        """Get events within a specific time range."""
        filtered_events = [e for e in self.events if start_time <= e.timestamp <= end_time]

        if event_type:
            filtered_events = [e for e in filtered_events if e.event_type == event_type]

        return filtered_events

    def get_recent_events(self, seconds: int, event_type: str | None = None) -> list[TemporalEvent]:
        """Get events from the last N seconds."""
        current_time = time.time()
        return self.get_events_in_range(current_time - seconds, current_time, event_type)


class TemporalPatternDetector:
    """
    Detects temporal patterns in event sequences.
    """

    def __init__(self, config: TemporalAbstractionConfig):
        self.config = config
        self.temporal_memory = TemporalMemory(config)

    def detect_periodic_patterns(self, events: list[TemporalEvent]) -> list[TemporalPattern]:
        """Detect periodic patterns in events."""
        if len(events) < 3:
            return []

        # Sort events by timestamp
        sorted_events = sorted(events, key=lambda e: e.timestamp)

        patterns = []

        # Group events by type
        event_groups: dict[str, list[TemporalEvent]] = {}
        for event in sorted_events:
            if event.event_type not in event_groups:
                event_groups[event.event_type] = []
            event_groups[event.event_type].append(event)

        # Analyze each event type for periodicity
        for event_type, event_list in event_groups.items():
            if len(event_list) < 3:
                continue

            # Calculate inter-event intervals
            intervals = []
            for i in range(1, len(event_list)):
                interval = event_list[i].timestamp - event_list[i - 1].timestamp
                if interval > 0:  # Only positive intervals
                    intervals.append(interval)

            if len(intervals) < 2:
                continue

            # Find dominant period using histogram
            interval_array = np.array(intervals)
            hist, bin_edges = np.histogram(interval_array, bins=20)

            # Find the most common interval range
            dominant_bin_idx = np.argmax(hist)
            period_estimate = (bin_edges[dominant_bin_idx] + bin_edges[dominant_bin_idx + 1]) / 2

            # Verify periodicity
            if len([i for i in intervals if abs(i - period_estimate) < period_estimate * 0.2]) >= len(intervals) * 0.6:
                # Calculate pattern strength (how regular the intervals are)
                std_dev = np.std(intervals)
                mean_interval = np.mean(intervals)
                regularity = 1.0 - min(1.0, std_dev / (mean_interval + 1e-8))

                pattern = TemporalPattern(
                    pattern_type=TemporalPatternType.PERIODIC,
                    start_time=event_list[0].timestamp,
                    end_time=event_list[-1].timestamp,
                    period=period_estimate,
                    frequency=1.0 / period_estimate if period_estimate > 0 else 0.0,
                    strength=regularity,
                    description=f"Periodic {event_type} events every ~{period_estimate:.0f} seconds",
                    confidence=min(1.0, regularity + 0.2),  # Boost confidence slightly
                )

                patterns.append(pattern)

        return patterns

    def detect_trends(self, events: list[TemporalEvent]) -> list[TemporalPattern]:
        """Detect trends in event occurrence rates."""
        if len(events) < 5:
            return []

        # Group events by time windows
        sorted_events = sorted(events, key=lambda e: e.timestamp)

        # Create sliding windows
        window_size = self.config.short_term_window
        windows = []

        start_time = sorted_events[0].timestamp
        end_time = sorted_events[-1].timestamp

        current_time = start_time
        while current_time < end_time:
            window_events = [e for e in sorted_events if current_time <= e.timestamp < current_time + window_size]

            windows.append(
                {
                    "start_time": current_time,
                    "end_time": current_time + window_size,
                    "count": len(window_events),
                    "density": len(window_events) / window_size,
                }
            )

            current_time += window_size // 4  # 75% overlap

        if len(windows) < 3:
            return []

        # Calculate trend using linear regression
        x_values = np.array([w["start_time"] for w in windows])
        y_values = np.array([w["density"] for w in windows])

        # Perform linear regression
        coeffs = np.polyfit(x_values, y_values, 1)
        slope = coeffs[0]
        intercept = coeffs[1]

        # Calculate R-squared
        y_pred = slope * x_values + intercept
        ss_res = np.sum((y_values - y_pred) ** 2)
        ss_tot = np.sum((y_values - np.mean(y_values)) ** 2)
        r_squared = 1 - (ss_res / ss_tot) if ss_tot != 0 else 0

        # Determine trend type
        if abs(slope) > 1e-6 and r_squared > 0.3:  # Significant trend with good fit
            trend_type = "increasing" if slope > 0 else "decreasing"
            min(1.0, abs(slope) * 1000)  # Normalize strength

            pattern = TemporalPattern(
                pattern_type=TemporalPatternType.TREND,
                start_time=windows[0]["start_time"],
                end_time=windows[-1]["end_time"],
                strength=min(1.0, r_squared),
                description=f"{trend_type} trend in event density (R²={r_squared:.2f})",
                confidence=min(1.0, r_squared),
            )

            return [pattern]

        return []

    def detect_seasonal_patterns(self, events: list[TemporalEvent]) -> list[TemporalPattern]:
        """Detect seasonal patterns (daily, weekly, etc.)."""
        if len(events) < 10:
            return []

        sorted_events = sorted(events, key=lambda e: e.timestamp)

        # Convert timestamps to different time units
        timestamps = np.array([e.timestamp for e in sorted_events])

        # Check for daily patterns (24-hour cycles)
        daily_patterns = self._check_cyclic_pattern(timestamps, 24 * 3600)

        # Check for weekly patterns (7-day cycles)
        weekly_patterns = self._check_cyclic_pattern(timestamps, 7 * 24 * 3600)

        # Check for monthly patterns (30-day cycles)
        monthly_patterns = self._check_cyclic_pattern(timestamps, 30 * 24 * 3600)

        patterns = []

        for cycle_type, cycle_time, strength in [
            ("daily", 24 * 3600, daily_patterns),
            ("weekly", 7 * 24 * 3600, weekly_patterns),
            ("monthly", 30 * 24 * 3600, monthly_patterns),
        ]:
            if strength > 0.4:  # Threshold for significance
                pattern = TemporalPattern(
                    pattern_type=TemporalPatternType.SEASONAL,
                    start_time=sorted_events[0].timestamp,
                    end_time=sorted_events[-1].timestamp,
                    period=cycle_time,
                    strength=strength,
                    description=f"Seasonal {cycle_type} pattern (strength: {strength:.2f})",
                    confidence=strength,
                )
                patterns.append(pattern)

        return patterns

    def _check_cyclic_pattern(self, timestamps: np.ndarray, cycle_period: float) -> float:
        """Check if there's a cyclic pattern with the given period."""
        if len(timestamps) < 5:
            return 0.0

        # Normalize timestamps to the cycle period
        normalized_times = timestamps % cycle_period

        # Create histogram to see clustering
        hist, _ = np.histogram(normalized_times, bins=20)

        # Calculate entropy of the distribution
        # Low entropy indicates strong clustering (periodic pattern)
        hist_normalized = hist / np.sum(hist)
        hist_normalized = hist_normalized[hist_normalized > 0]  # Remove zeros
        entropy = -np.sum(hist_normalized * np.log(hist_normalized + 1e-10))

        # Convert entropy to pattern strength (lower entropy = stronger pattern)
        max_entropy = np.log(len(hist))  # Maximum possible entropy
        pattern_strength = 1.0 - (entropy / max_entropy) if max_entropy > 0 else 0.0

        return pattern_strength

    def detect_anomalies(self, events: list[TemporalEvent]) -> list[TemporalPattern]:
        """Detect anomalous temporal patterns."""
        if len(events) < 5:
            return []

        sorted_events = sorted(events, key=lambda e: e.timestamp)

        # Calculate inter-event intervals
        intervals = []
        for i in range(1, len(sorted_events)):
            interval = sorted_events[i].timestamp - sorted_events[i - 1].timestamp
            intervals.append(interval)

        if len(intervals) < 2:
            return []

        # Calculate statistics
        mean_interval = np.mean(intervals)
        std_interval = np.std(intervals)

        anomalies = []

        # Identify anomalous intervals
        for i, interval in enumerate(intervals):
            z_score = abs(interval - mean_interval) / (std_interval + 1e-8)

            if z_score > self.config.anomaly_threshold:
                anomaly_time = sorted_events[i + 1].timestamp
                anomaly = TemporalPattern(
                    pattern_type=TemporalPatternType.ANOMALOUS,
                    start_time=anomaly_time,
                    end_time=anomaly_time,
                    strength=z_score / self.config.anomaly_threshold,
                    description=f"Temporal anomaly at {datetime.fromtimestamp(anomaly_time)} (z-score: {z_score:.2f})",
                    confidence=min(1.0, z_score / 5.0),  # Cap confidence
                )
                anomalies.append(anomaly)

        return anomalies

    def identify_temporal_patterns(self, events: list[TemporalEvent]) -> list[TemporalPattern]:
        """Identify all types of temporal patterns in events."""
        patterns = []

        # Detect different types of patterns
        patterns.extend(self.detect_periodic_patterns(events))
        patterns.extend(self.detect_trends(events))
        patterns.extend(self.detect_seasonal_patterns(events))
        patterns.extend(self.detect_anomalies(events))

        # Filter by confidence threshold
        return [p for p in patterns if p.confidence >= self.config.pattern_confidence_threshold]


class TemporalPredictor:
    """
    Predicts future events based on temporal patterns.
    """

    def __init__(self, config: TemporalAbstractionConfig):
        self.config = config
        self.pattern_detector = TemporalPatternDetector(config)

    def predict_next_event(self, events: list[TemporalEvent], event_type: str) -> tuple[float | None, float]:
        """
        Predict when the next event of a specific type will occur.

        Returns:
            (predicted_timestamp, confidence)
        """
        # Filter events of the specified type
        filtered_events = [e for e in events if e.event_type == event_type]

        if len(filtered_events) < 2:
            return None, 0.1  # Low confidence if insufficient data

        # Detect patterns in these events
        patterns = self.pattern_detector.identify_temporal_patterns(filtered_events)

        # Prioritize periodic patterns for prediction
        periodic_patterns = [p for p in patterns if p.pattern_type == TemporalPatternType.PERIODIC]

        if periodic_patterns:
            # Use the strongest periodic pattern
            strongest_pattern = max(periodic_patterns, key=lambda p: p.strength)

            if strongest_pattern.period:
                last_event = max(filtered_events, key=lambda e: e.timestamp)
                predicted_time = last_event.timestamp + strongest_pattern.period
                confidence = strongest_pattern.confidence

                return predicted_time, confidence

        # If no strong periodic pattern, use trend analysis
        trend_patterns = [p for p in patterns if p.pattern_type == TemporalPatternType.TREND]

        if trend_patterns:
            trend = trend_patterns[0]  # Use the first trend
            # Simple extrapolation based on trend
            last_event = max(filtered_events, key=lambda e: e.timestamp)
            # This is a simplified prediction - a full implementation would be more sophisticated
            predicted_time = last_event.timestamp + self.config.prediction_horizon
            confidence = min(0.8, trend.confidence)  # Cap trend confidence

            return predicted_time, confidence

        # If no patterns, use simple average interval
        sorted_events = sorted(filtered_events, key=lambda e: e.timestamp)
        intervals = [sorted_events[i].timestamp - sorted_events[i - 1].timestamp for i in range(1, len(sorted_events))]

        if intervals:
            avg_interval = np.mean(intervals)
            last_time = sorted_events[-1].timestamp
            predicted_time = last_time + avg_interval
            confidence = 0.3  # Low confidence for simple average

            return predicted_time, confidence

        return None, 0.1  # Very low confidence

    def predict_event_sequence(
        self, events: list[TemporalEvent], horizon_seconds: int
    ) -> list[tuple[float, str, float]]:
        """
        Predict a sequence of events over the specified time horizon.

        Returns:
            List of (timestamp, event_type, confidence) tuples
        """
        predictions = []

        # Get unique event types
        event_types = list(set(e.event_type for e in events))

        current_time = time.time()
        end_time = current_time + horizon_seconds

        # For each event type, predict next occurrence
        for event_type in event_types:
            predicted_time, confidence = self.predict_next_event(events, event_type)

            if predicted_time and current_time < predicted_time <= end_time:
                predictions.append((predicted_time, event_type, confidence))

        # Sort predictions by time
        predictions.sort(key=lambda x: x[0])

        return predictions


class TemporalAbstractionLayer:
    """
    Creates temporal abstractions by grouping fine-grained events into meaningful chunks.
    """

    def __init__(self, config: TemporalAbstractionConfig):
        self.config = config
        self.abstraction_levels = {}

    def create_abstraction(self, events: list[TemporalEvent], granularity: TemporalGranularity) -> list[dict[str, Any]]:
        """
        Create temporal abstractions at the specified granularity.

        Returns:
            List of abstracted time periods with aggregated information
        """
        if not events:
            return []

        # Sort events by timestamp
        sorted_events = sorted(events, key=lambda e: e.timestamp)

        # Group events by the specified granularity
        grouped_events = self._group_by_granularity(sorted_events, granularity)

        abstractions = []

        for time_period, period_events in grouped_events.items():
            # Calculate statistics for this period
            stats = self._calculate_period_statistics(period_events)

            abstraction = {
                "time_period": time_period,
                "event_count": len(period_events),
                "event_types": list(set(e.event_type for e in period_events)),
                "statistics": stats,
                "representative_events": self._select_representative_events(period_events),
                "complexity": self._calculate_complexity(period_events),
            }

            abstractions.append(abstraction)

        return abstractions

    def _group_by_granularity(
        self, events: list[TemporalEvent], granularity: TemporalGranularity
    ) -> dict[str, list[TemporalEvent]]:
        """Group events by the specified temporal granularity."""
        groups = {}

        for event in events:
            time_key = self._get_time_key(event.timestamp, granularity)

            if time_key not in groups:
                groups[time_key] = []

            groups[time_key].append(event)

        return groups

    def _get_time_key(self, timestamp: float, granularity: TemporalGranularity) -> str:
        """Convert timestamp to a key based on granularity."""
        dt = datetime.fromtimestamp(timestamp)

        if granularity == TemporalGranularity.SECOND:
            return f"{dt.year}-{dt.month:02d}-{dt.day:02d} {dt.hour:02d}:{dt.minute:02d}:{dt.second:02d}"
        elif granularity == TemporalGranularity.MINUTE:
            return f"{dt.year}-{dt.month:02d}-{dt.day:02d} {dt.hour:02d}:{dt.minute:02d}"
        elif granularity == TemporalGranularity.HOUR:
            return f"{dt.year}-{dt.month:02d}-{dt.day:02d} {dt.hour:02d}:00"
        elif granularity == TemporalGranularity.DAY:
            return f"{dt.year}-{dt.month:02d}-{dt.day:02d}"
        elif granularity == TemporalGranularity.WEEK:
            week_num = dt.isocalendar()[1]
            return f"{dt.year}-W{week_num:02d}"
        elif granularity == TemporalGranularity.MONTH:
            return f"{dt.year}-{dt.month:02d}"
        elif granularity == TemporalGranularity.YEAR:
            return f"{dt.year}"
        else:
            # For custom granularities, use a default approach
            return str(int(timestamp))

    def _calculate_period_statistics(self, events: list[TemporalEvent]) -> dict[str, float]:
        """Calculate statistics for a period of events."""
        if not events:
            return {}

        timestamps = [e.timestamp for e in events]
        durations = [e.duration for e in events if e.duration is not None]
        priorities = [e.priority for e in events]
        confidences = [e.confidence for e in events]

        stats = {
            "start_time": min(timestamps),
            "end_time": max(timestamps),
            "duration_span": max(timestamps) - min(timestamps),
            "avg_priority": np.mean(priorities) if priorities else 0.0,
            "avg_confidence": np.mean(confidences) if confidences else 0.0,
            "event_density": len(events) / (max(timestamps) - min(timestamps) + 1e-8),
        }

        if durations:
            stats["avg_duration"] = np.mean(durations)
            stats["total_duration"] = sum(durations)

        return stats  # type: ignore

    def _select_representative_events(self, events: list[TemporalEvent]) -> list[TemporalEvent]:
        """Select representative events from a group."""
        if not events:
            return []

        # Sort by priority and confidence
        sorted_events = sorted(events, key=lambda e: e.priority * e.confidence, reverse=True)

        # Return top 3 events or all if fewer than 3
        return sorted_events[:3]

    def _calculate_complexity(self, events: list[TemporalEvent]) -> float:
        """Calculate the temporal complexity of a group of events."""
        if len(events) < 2:
            return 0.0

        # Calculate temporal irregularity
        sorted_events = sorted(events, key=lambda e: e.timestamp)
        intervals = [sorted_events[i].timestamp - sorted_events[i - 1].timestamp for i in range(1, len(sorted_events))]

        if not intervals:
            return 0.0

        # Complexity is higher when intervals are more variable
        mean_interval = np.mean(intervals)
        std_interval = np.std(intervals)
        irregularity = std_interval / (mean_interval + 1e-8)

        # Also consider diversity of event types
        event_type_diversity = len(set(e.event_type for e in events)) / len(events)

        # Combine measures
        complexity = (irregularity * 0.6) + (event_type_diversity * 0.4)

        return min(1.0, complexity)  # type: ignore


class TemporalAbstractionSystem:
    """
    Main system integrating temporal memory, pattern detection, prediction, and abstraction.
    """

    def __init__(self, config: TemporalAbstractionConfig = None):
        self.config = config or TemporalAbstractionConfig()
        self.temporal_memory = TemporalMemory(self.config)
        self.pattern_detector = TemporalPatternDetector(self.config)
        self.predictor = TemporalPredictor(self.config)
        self.abstraction_layer = TemporalAbstractionLayer(self.config)

        # Maintain hierarchy of abstractions
        self.abstraction_hierarchy = {}

    def process_event(self, event: TemporalEvent):
        """Process a new temporal event."""
        self.temporal_memory.add_event(event)

        # Update patterns regularly
        if len(self.temporal_memory.events) % 10 == 0:  # Every 10 events
            self._update_patterns()

    def _update_patterns(self):
        """Update detected patterns based on current events."""
        self.pattern_detector.temporal_memory = self.temporal_memory
        # Note: We don't store patterns here as they're computed on demand

    def get_temporal_patterns(self, time_window: int | None = None) -> list[TemporalPattern]:
        """Get temporal patterns in the specified time window."""
        if time_window:
            current_time = time.time()
            events = self.temporal_memory.get_events_in_range(current_time - time_window, current_time)
        else:
            events = self.temporal_memory.events

        return self.pattern_detector.identify_temporal_patterns(events)

    def predict_future_events(self, horizon_seconds: int) -> list[dict[str, Any]]:
        """Predict events in the future."""
        predictions = self.predictor.predict_event_sequence(self.temporal_memory.events, horizon_seconds)

        return [
            {
                "predicted_time": datetime.fromtimestamp(pred[0]).isoformat() if pred[0] else None,
                "event_type": pred[1],
                "confidence": pred[2],
                "timestamp": pred[0],
            }
            for pred in predictions
        ]

    def create_temporal_abstraction(
        self, granularity: TemporalGranularity, time_window: int | None = None
    ) -> list[dict[str, Any]]:
        """Create temporal abstractions at the specified granularity."""
        if time_window:
            current_time = time.time()
            events = self.temporal_memory.get_events_in_range(current_time - time_window, current_time)
        else:
            events = self.temporal_memory.events

        return self.abstraction_layer.create_abstraction(events, granularity)

    def get_temporal_insights(self) -> dict[str, Any]:
        """Get comprehensive temporal insights."""
        events = self.temporal_memory.events
        if not events:
            return {"message": "No events recorded yet"}

        # Basic statistics
        start_time = min(e.timestamp for e in events)
        end_time = max(e.timestamp for e in events)
        total_duration = end_time - start_time

        # Event statistics
        event_types = [e.event_type for e in events]
        type_counts = {}
        for et in event_types:
            type_counts[et] = type_counts.get(et, 0) + 1

        # Calculate event rate
        event_rate = len(events) / (total_duration / 3600) if total_duration > 0 else 0  # Per hour

        # Detect patterns
        patterns = self.get_temporal_patterns()
        pattern_types = [p.pattern_type.value for p in patterns]

        insights = {
            "time_span": {
                "start": datetime.fromtimestamp(start_time).isoformat(),
                "end": datetime.fromtimestamp(end_time).isoformat(),
                "duration_hours": total_duration / 3600,
            },
            "event_statistics": {
                "total_events": len(events),
                "event_types": type_counts,
                "event_rate_per_hour": event_rate,
                "average_confidence": np.mean([e.confidence for e in events]),
                "average_priority": np.mean([e.priority for e in events]),
            },
            "detected_patterns": {
                "total_patterns": len(patterns),
                "pattern_types": list(set(pattern_types)),
                "strong_patterns": [p.description for p in patterns if p.strength > 0.7],
            },
            "predictions": self.predict_future_events(self.config.prediction_horizon),
        }

        return insights

    def get_temporal_hierarchy(self) -> dict[str, list[dict[str, Any]]]:
        """Get temporal hierarchy across different granularities."""
        hierarchy = {}

        for granularity in [TemporalGranularity.HOUR, TemporalGranularity.DAY, TemporalGranularity.WEEK]:
            abstraction = self.create_temporal_abstraction(granularity)
            hierarchy[granularity.value] = abstraction

        return hierarchy


# Example usage and testing
def demo_temporal_abstraction():
    """Demonstrate temporal abstraction system capabilities."""
    print("Initializing Temporal Abstraction System...")

    # Create system
    config = TemporalAbstractionConfig()
    temp_system = TemporalAbstractionSystem(config)

    # Simulate events happening over time
    base_time = time.time() - 86400  # 24 hours ago

    # Create periodic events (every 2 hours)
    for i in range(12):
        event_time = base_time + i * 7200  # Every 2 hours
        event = TemporalEvent(
            timestamp=event_time,
            event_type="system_check",
            event_data={"status": "normal"},
            duration=10,
            confidence=0.9,
            priority=0.3,
        )
        temp_system.process_event(event)

    # Create some trend events (increasing frequency)
    for i in range(5):
        event_time = base_time + 86400 + i * 1800  # Last 2.5 hours, every 30 min
        event = TemporalEvent(
            timestamp=event_time,
            event_type="user_activity",
            event_data={"user_id": f"user_{i}"},
            duration=120,
            confidence=0.8,
            priority=0.6,
        )
        temp_system.process_event(event)

    # Create an anomalous event
    anomalous_time = base_time + 43200  # Middle of the timeline
    anomalous_event = TemporalEvent(
        timestamp=anomalous_time,
        event_type="system_error",
        event_data={"error_code": "E500", "severity": "high"},
        duration=300,
        confidence=1.0,
        priority=0.9,
    )
    temp_system.process_event(anomalous_event)

    print(f"Processed {len(temp_system.temporal_memory.events)} events")

    # Get detected patterns
    print("\nDetecting temporal patterns...")
    patterns = temp_system.get_temporal_patterns()
    print(f"Detected {len(patterns)} patterns:")
    for i, pattern in enumerate(patterns):
        print(
            f"  {i+1}. {pattern.description} (strength: {pattern.strength:.2f}, confidence: {pattern.confidence:.2f})"
        )

    # Get predictions
    print("\nMaking predictions...")
    predictions = temp_system.predict_future_events(3600)  # Next hour
    print(f"Predicted {len(predictions)} events in the next hour:")
    for pred in predictions:
        print(f"  - {pred['event_type']} at {pred['predicted_time']} (confidence: {pred['confidence']:.2f})")

    # Create abstractions
    print("\nCreating temporal abstractions...")
    hourly_abstractions = temp_system.create_temporal_abstraction(TemporalGranularity.HOUR)
    print(f"Created {len(hourly_abstractions)} hourly abstractions")

    # Print first few abstractions
    for i, abst in enumerate(hourly_abstractions[:3]):
        print(f"  {i+1}. {abst['time_period']}: {abst['event_count']} events, complexity: {abst['complexity']:.2f}")

    # Get comprehensive insights
    print("\nGetting temporal insights...")
    insights = temp_system.get_temporal_insights()

    print(f"Time span: {insights['time_span']['duration_hours']:.1f} hours")
    print(f"Total events: {insights['event_statistics']['total_events']}")
    print(f"Event types: {list(insights['event_statistics']['event_types'].keys())}")
    print(f"Detected patterns: {insights['detected_patterns']['total_patterns']}")
    print(f"Strong patterns: {len(insights['detected_patterns']['strong_patterns'])}")

    # Get temporal hierarchy
    print("\nGetting temporal hierarchy...")
    hierarchy = temp_system.get_temporal_hierarchy()
    for granularity, abstractions in hierarchy.items():
        print(f"{granularity.capitalize()} level: {len(abstractions)} abstractions")


if __name__ == "__main__":
    demo_temporal_abstraction()
