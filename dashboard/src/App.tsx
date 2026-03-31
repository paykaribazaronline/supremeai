import React, { useEffect, useMemo, useState } from "react";

type HealthState = "checking" | "up" | "down";

type Metric = {
  label: string;
  value: string;
  tone?: "good" | "warn";
};

const defaultMetrics: Metric[] = [
  { label: "Backend", value: "Checking", tone: "warn" },
  { label: "Dashboard", value: "Build Ready", tone: "good" },
  { label: "Admin App", value: "Flutter Web", tone: "good" },
  { label: "Hosting", value: "Firebase Unified", tone: "good" },
];

const workstreams = [
  "Secure every protected API with one auth path",
  "Keep dashboard and admin app deployable from CI",
  "Route Firebase Functions to real backend URLs",
  "Expose project health without leaking secrets",
];

const App: React.FC = () => {
  const [health, setHealth] = useState<HealthState>("checking");
  const [checkedAt, setCheckedAt] = useState<string>("Not checked yet");

  useEffect(() => {
    let cancelled = false;

    const loadHealth = async () => {
      try {
        const response = await fetch("/actuator/health", {
          headers: { Accept: "application/json" },
        });

        if (!cancelled) {
          setHealth(response.ok ? "up" : "down");
          setCheckedAt(new Date().toLocaleString());
        }
      } catch (error) {
        if (!cancelled) {
          setHealth("down");
          setCheckedAt(new Date().toLocaleString());
        }
      }
    };

    loadHealth();
    const interval = window.setInterval(loadHealth, 30000);

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, []);

  const metrics = useMemo<Metric[]>(() => {
    const backendMetric: Metric =
      health === "up"
        ? { label: "Backend", value: "Healthy", tone: "good" }
        : health === "down"
          ? { label: "Backend", value: "Unavailable", tone: "warn" }
          : { label: "Backend", value: "Checking", tone: "warn" };

    return [backendMetric, ...defaultMetrics.slice(1)];
  }, [health]);

  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">SupremeAI Control Surface</p>
        <h1>One place to verify the system before we ship it.</h1>
        <p className="lede">
          This dashboard is now a clean Vite entry point for the project. It keeps
          the frontend build green while we phase richer admin modules back in on a
          stable foundation.
        </p>

        <div className="status-row">
          <span className={`pill pill-${health}`}>Backend {health}</span>
          <span className="muted">Last check: {checkedAt}</span>
        </div>
      </section>

      <section className="grid">
        {metrics.map((metric) => (
          <article className="card" key={metric.label}>
            <span className="card-label">{metric.label}</span>
            <strong className={`card-value tone-${metric.tone ?? "good"}`}>
              {metric.value}
            </strong>
          </article>
        ))}
      </section>

      <section className="panel">
        <div>
          <p className="eyebrow">Release Focus</p>
          <h2>Current workstreams</h2>
        </div>
        <ul className="workstreams">
          {workstreams.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
    </main>
  );
};

export default App;
