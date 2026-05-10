const GITHUB_API_BASE = "https://api.github.com";

export class GitHubAPI {
  private token: string | undefined;

  constructor(token?: string) {
    this.token = token;
  }

  private async request(endpoint: string, options: RequestInit = {}) {
    const headers: HeadersInit = {
      Accept: "application/vnd.github.v3+json",
      "User-Agent": "GitReverse/1.0.0",
      ...options.headers,
    };

    if (this.token) {
      (headers as Record<string, string>)["Authorization"] =
        `token ${this.token}`;
    }

    const response = await fetch(`${GITHUB_API_BASE}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      throw { status: response.status, message: response.statusText };
    }

    return response.json();
  }

  async getRepo(owner: string, repo: string) {
    return this.request(`/repos/${owner}/${repo}`);
  }

  async getReadme(owner: string, repo: string, ref?: string) {
    const params = ref ? `?ref=${ref}` : "";
    const data = await this.request(
      `/repos/${owner}/${repo}/readme${params}`
    );

    // GitHub returns content base64 encoded
    if (data.content) {
      return atob(data.content.replace(/\n/g, ""));
    }
    return null;
  }

  async getTree(owner: string, repo: string, branch: string = "main") {
    return this.request(
      `/repos/${owner}/${repo}/git/trees/${branch}?recursive=1`
    );
  }

  async getBranches(owner: string, repo: string) {
    return this.request(`/repos/${owner}/${repo}/branches`);
  }
}
