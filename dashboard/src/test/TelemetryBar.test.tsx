import { render, screen } from "@testing-library/react";
import React from "react";
import { describe, test, expect } from "vitest";

import TelemetryBar from "../components/TelemetryBar";

describe("TelemetryBar Component", () => {
  test("renders system status, uptime, latency, active nodes, and CPU load correctly", () => {
    const props = {
      uptime: "02h 15m 30s",
      latency: 45,
      activeNodes: 8,
      load: 62,
    };

    render(<TelemetryBar {...props} />);

    // Verify system operational status
    expect(screen.getByText("System: Operational")).toBeInTheDocument();

    // Verify uptime is rendered correctly
    expect(screen.getByText("02h 15m 30s")).toBeInTheDocument();

    // Verify latency is rendered correctly
    expect(screen.getByText("45ms")).toBeInTheDocument();

    // Verify active nodes count is rendered correctly
    expect(screen.getByText("8")).toBeInTheDocument();

    // Verify CPU load percentage is rendered correctly
    expect(screen.getByText("62%")).toBeInTheDocument();
  });
});
