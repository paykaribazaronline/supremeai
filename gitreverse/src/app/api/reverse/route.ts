import { NextRequest, NextResponse } from "next/server";
import { GitHubAPI } from "@/lib/github";
import { LLMProvider } from "@/lib/llm";
import { getSupabase } from "@/lib/supabase";
import { InFlightCache } from "@/lib/cache";

// In-flight request map to prevent duplicates
const inFlightCache = new InFlightCache();

interface ReverseRequestBody {
  url: string;
  focus?: string;
}

interface GitHubRepoInfo {
  owner: string;
  repo: string;
  full_name: string;
}

function parseGitHubURL(url: string): GitHubRepoInfo | null {
  // Handle various GitHub URL formats
  const patterns = [
    /https?:\/\/github\.com\/([^/]+)\/([^/]+?)(?:\.git)?(?:\/.*)?$/,
    /^([^/]+)\/([^/]+)$/,  // owner/repo format
  ];

  for (const pattern of patterns) {
    const match = url.match(pattern);
    if (match) {
      const owner = match[1];
      const repo = match[2].replace(/\.git$/, "");
      return { owner, repo, full_name: `${owner}/${repo}` };
    }
  }

  return null;
}

export async function POST(request: NextRequest) {
  try {
    const body: ReverseRequestBody = await request.json();
    const { url, focus } = body;

    if (!url) {
      return NextResponse.json(
        { error: "URL is required" },
        { status: 400 }
      );
    }

    // Parse GitHub URL
    const repoInfo = parseGitHubURL(url);
    if (!repoInfo) {
      return NextResponse.json(
        { error: "Invalid GitHub URL or owner/repo format" },
        { status: 400 }
      );
    }

    // Check in-flight
    const cacheKey = `${repoInfo.full_name}:${focus || ""}`;
    const inFlight = inFlightCache.get(cacheKey);
    if (inFlight) {
      return inFlight;
    }

    // Create new request
    const promise = (async () => {
      try {
        // 1. Fetch repo metadata and README
        const github = new GitHubAPI(process.env.GITHUB_TOKEN);
        
        let metadata, readme, tree;
        try {
          [metadata, readme, tree] = await Promise.all([
            github.getRepo(repoInfo.owner, repoInfo.repo),
            github.getReadme(repoInfo.owner, repoInfo.repo).catch(() => null),
            github.getTree(repoInfo.owner, repoInfo.repo, "main").catch(async (err) => {
              // Retry with master if main fails
              if (err.status === 404) {
                return github.getTree(repoInfo.owner, repoInfo.repo, "master");
              }
              throw err;
            }),
          ]);
        } catch (apiError: any) {
          if (apiError.status === 404) {
            return NextResponse.json(
              { error: "Repository not found" },
              { status: 404 }
            );
          }
          throw apiError;
        }

        // 2. Build context for LLM
        const context = buildContext({
          metadata,
          readme: readme ? readme.slice(0, 8000) : null,
          tree: tree ? tree.filter((item: any) => item.type === "blob").slice(0, 100) : [],
        });

        // 3. Call LLM
        const llm = new LLMProvider();
        let prompt: string;
        try {
          prompt = await llm.generatePrompt(context, focus);
        } catch (llmError: any) {
          // Check for rate limit errors
          const errorMsg = llmError.message || "";
          if (
            errorMsg.includes("rate limit") ||
            errorMsg.includes("credits") ||
            errorMsg.includes("429")
          ) {
            return NextResponse.json(
              {
                error: "LLM rate limit exceeded",
                message: "Please check our library for existing prompts instead.",
              },
              { status: 429 }
            );
          }
          throw llmError;
        }

        // 4. Cache in Supabase if available
        const supabase = getSupabase();
        if (supabase) {
          try {
            await supabase.from("quick_reverse_cache").upsert({
              repo_full_name: repoInfo.full_name,
              prompt,
              metadata: {
                stars: metadata.stargazers_count,
                language: metadata.language,
                description: metadata.description,
              },
              created_at: new Date().toISOString(),
            });
          } catch (cacheError) {
            console.error("Failed to cache in Supabase:", cacheError);
          }
        }

        return NextResponse.json({ prompt });
      } catch (error: any) {
        console.error("Reverse error:", error);
        return NextResponse.json(
          { error: error.message || "Internal server error" },
          { status: 500 }
        );
      } finally {
        // Clean up in-flight
        inFlightCache.delete(cacheKey);
      }
    })();

    inFlightCache.set(cacheKey, promise);
    return promise;
  } catch (error: any) {
    console.error("Request error:", error);
    return NextResponse.json(
      { error: "Invalid request" },
      { status: 400 }
    );
  }
}

function buildContext(data: any) {
  const parts = [
    `Repository: ${data.metadata.full_name}`,
    `Stars: ${data.metadata.stargazers_count || 0}`,
    `Language: ${data.metadata.language || "Unknown"}`,
    `Description: ${data.metadata.description || "No description"}`,
  ];

  if (data.metadata.topics && data.metadata.topics.length > 0) {
    parts.push(`Topics: ${data.metadata.topics.join(", ")}`);
  }

  if (data.readme) {
    parts.push(`\nREADME:\n${data.readme.slice(0, 8000)}`);
  }

  if (data.tree && data.tree.length > 0) {
    parts.push(`\nFile Tree (depth 1):`);
    parts.push(
      data.tree
        .slice(0, 100)
        .map((item: any) => `- ${item.path}`)
        .join("\n")
    );
  }

  return parts.join("\n");
}
