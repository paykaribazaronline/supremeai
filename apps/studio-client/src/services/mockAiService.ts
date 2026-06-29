// Mock AI Orchestrator Service for SupremeAI 2.0 (Offline/Local Fallback Mode)
// বাংলা মন্তব্য: এটি ব্যাকএন্ড অফলাইন থাকলে ক্লায়েন্ট-সাইডে Aethel AI এর বুদ্ধিমান আচরণ ও রেসপন্স সিমুলেট করে।

export interface MockAIResponse {
  text: string;
  isSystem?: boolean;
}

const ORCHESTRATOR_RESPONSES = [
  "All core clusters (Node 47, Node 12, Swarm-Alpha) are currently running at nominal capacity.",
  "Warning: Minor latency detected in Cloud Orchestrator gateway. Automatically balancing traffic.",
  "Security status: Threat level low. Central ORC firewall active.",
  "Aethel cognitive pipeline is fully synced. Waiting for orchestrator directives.",
];

export const mockAiService = {
  generateResponse: (userInput: string): MockAIResponse => {
    const input = userInput.toLowerCase().trim();

    // 1. Greet Intents
    if (input.match(/^(hello|hi|hey|halo|greetings)/)) {
      return {
        text: `Greetings, Operator. I am Aethel, your SupremeAI Core Orchestrator. How can I assist you with the cluster orchestration today?`
      };
    }

    // 2. Status / Diagnostics
    if (input.includes('status') || input.includes('diagnose') || input.includes('health')) {
      const cpu = Math.floor(Math.random() * 25) + 30; // 30-55%
      const mem = Math.floor(Math.random() * 15) + 60; // 60-75%
      return {
        text: `[System Status: NOMINAL]\n- CPU Load: ${cpu}%\n- Memory Usage: ${mem}%\n- Active Clusters: 4/4\n- All telemetry streams synced. Firewall status: SECURE.`
      };
    }

    // 3. Deployment / Node command
    if (input.includes('deploy') || input.includes('node') || input.includes('run')) {
      const nodeId = input.match(/\d+/) ? `Node ${input.match(/\d+/)?.[0]}` : "Node 47 (Analytics)";
      return {
        text: `Initiating diagnostic probe on ${nodeId}...\nTelemetry reports nominal load. Automatic failover active. CI/CD pipeline triggered successfully.`
      };
    }

    // 4. Help
    if (input === 'help' || input === 'command' || input === '?') {
      return {
        text: `Aethel Command Registry:\n- "status" / "health": Check cluster telemetry\n- "deploy [node_id]": Trigger node check\n- "optimize": Optimize cluster nodes\n- "theme [name]": Switch visual dimension`
      };
    }

    // 5. Optimization
    if (input.includes('optimize') || input.includes('fix')) {
      return {
        text: `Optimization sequence initiated. Re-allocating workloads from Cloud Orchestrator to Java Background Worker... Workload balanced successfully.`
      };
    }

    // 6. Fallback (Intelligent random response)
    const randomReply = ORCHESTRATOR_RESPONSES[Math.floor(Math.random() * ORCHESTRATOR_RESPONSES.length)];
    return {
      text: `Processing: "${userInput}"...\n${randomReply}`
    };
  }
};
