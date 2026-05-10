import OpenAI from "openai";

export class LLMProvider {
  private client: OpenAI | null = null;
  private provider: "openrouter" | "google" = "openrouter";

  constructor() {
    if (process.env.OPENROUTER_API_KEY) {
      this.provider = "openrouter";
      this.client = new OpenAI({
        apiKey: process.env.OPENROUTER_API_KEY,
        baseURL: "https://openrouter.ai/api/v1",
      });
    } else if (process.env.GOOGLE_GENERATIVE_AI_API_KEY) {
      this.provider = "google";
      // Google AI Studio integration would go here
      throw new Error("Google AI Studio not yet implemented");
    } else {
      throw new Error(
        "No LLM provider configured. Set OPENROUTER_API_KEY or GOOGLE_GENERATIVE_AI_API_KEY"
      );
    }
  }

  async generatePrompt(
    context: string,
    focus?: string
  ): Promise<string> {
    const systemPrompt = `You are an expert software architect and technical writer. Given a GitHub repository's metadata, README, and file tree, generate a concise (120-200 words) natural-language prompt that someone could paste into an AI coding assistant to recreate the project from scratch.

IMPORTANT GUIDELINES:
- Focus on capturing the INTENT, ARCHITECTURE, and PURPOSE, not implementation details
- Mention the tech stack, key features, and overall structure
- Include any important patterns, conventions, or design decisions
- Write in clear, instructional language
- The prompt should be immediately usable in tools like Claude, ChatGPT, or Cursor${focus ? `\n\nSPECIAL FOCUS: ${focus}` : ""}`;

    const userPrompt = `Here is the repository context:\n\n${context}\n\nGenerate the prompt:`;

    if (this.provider === "openrouter") {
      if (!this.client) {
        throw new Error("OpenRouter client not initialized");
      }

      const response = await this.client.chat.completions.create({
        model:
          process.env.OPENROUTER_MODEL || "google/gemini-2.5-pro",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userPrompt },
        ],
        max_tokens: 500,
        temperature: 0.7,
      });

      return response.choices[0].message.content || "";
    } else {
      throw new Error(`Provider ${this.provider} not implemented`);
    }
  }
}
