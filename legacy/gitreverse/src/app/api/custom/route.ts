import { NextRequest, NextResponse } from "next/server";
import crypto from "crypto";

interface CustomRequestBody {
  url: string;
  focus: string;
}

// Cache for custom requests by focus string hash
const focusCache = new Map<string, { result: string; timestamp: number }>();
const CACHE_TTL = 60 * 60 * 1000; // 1 hour

function getFocusHash(focus: string): string {
  return crypto.createHash("md5").update(focus).digest("hex");
}

export async function POST(request: NextRequest) {
  try {
    const body: CustomRequestBody = await request.json();
    const { url, focus } = body;

    if (!url || !focus) {
      return NextResponse.json(
        { error: "URL and focus are required" },
        { status: 400 }
      );
    }

    // Check MD5 cache
    const focusHash = getFocusHash(focus);
    const cached = focusCache.get(focusHash);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      return NextResponse.json({ result: cached.result });
    }

    const serviceUrl =
      process.env.CUSTOM_REVERSE_SERVICE_URL || "http://localhost:3001";

    // Create AbortController for 15-minute timeout
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15 * 60 * 1000);

    try {
      const response = await fetch(`${serviceUrl}/api/custom-reverse`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url, focus }),
        signal: controller.signal,
      });

      clearTimeout(timeout);

      if (!response.ok) {
        if (response.status === 503) {
          return NextResponse.json(
            {
              error: "Custom reverse service unavailable",
              message:
                "The custom reverse service is not running. Please start it and try again.",
            },
            { status: 503 }
          );
        }
        throw new Error(`Custom service error: ${response.statusText}`);
      }

      // Check if response is SSE stream
      const contentType = response.headers.get("content-type");
      if (contentType?.includes("text/event-stream")) {
        // Return SSE stream directly
        return new Response(response.body, {
          headers: {
            "Content-Type": "text/event-stream",
            "Cache-Control": "no-cache",
            Connection: "keep-alive",
          },
        });
      }

      const data = await response.json();

      // Cache result
      focusCache.set(focusHash, {
        result: data.result,
        timestamp: Date.now(),
      });

      return NextResponse.json(data);
    } catch (error: any) {
      clearTimeout(timeout);

      if (error.name === "AbortError") {
        return NextResponse.json(
          { error: "Request timed out after 15 minutes" },
          { status: 504 }
        );
      }

      // Service not running
      if (error.code === "ECONNREFUSED") {
        return NextResponse.json(
          {
            error: "Custom reverse service unavailable",
            message:
              "The custom reverse service is not running. Please start it at " +
              serviceUrl,
          },
          { status: 503 }
        );
      }

      throw error;
    }
  } catch (error: any) {
    console.error("Custom reverse error:", error);
    return NextResponse.json(
      { error: error.message || "Internal server error" },
      { status: 500 }
    );
  }
}
