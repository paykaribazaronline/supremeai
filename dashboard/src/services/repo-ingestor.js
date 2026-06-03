/**
 * Repo Ingestor Service
 * Fetches repository data from GitHub API directly
 * Supports: public repos (no token), private repos (PAT), GitHub App (JWT)
 */

class RepoIngestor {
  constructor() {
    this.baseUrl = 'https://api.github.com';
    this.cache = new Map();
  }

  /**
   * Fetch repository metadata
   * @param {string} owner - Repository owner
   * @param {string} repo - Repository name
   * @param {string|null} token - GitHub token (optional)
   * @returns {Promise<Object>} Repository metadata
   */
  async fetchRepoMetadata(owner, repo, token = null) {
    const cacheKey = `meta:${owner}/${repo}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const headers = this._getHeaders(token);
    const response = await fetch(
      `${this.baseUrl}/repos/${owner}/${repo}`,
      { headers }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch repo metadata: ${response.statusText}`);
    }

    const data = await response.json();
    const metadata = {
      full_name: data.full_name,
      name: data.name,
      owner: data.owner.login,
      description: data.description,
      language: data.language,
      stars: data.stargazers_count,
      forks: data.forks_count,
      topics: data.topics || [],
      default_branch: data.default_branch,
      updated_at: data.updated_at,
      created_at: data.created_at,
      size: data.size,
      isPrivate: data.private,
      html_url: data.html_url,
      license: data.license ? data.license.key : null,
    };

    this.cache.set(cacheKey, metadata);
    return metadata;
  }

  /**
   * Fetch file tree from repository
   * @param {string} owner - Repository owner
   * @param {string} repo - Repository name
   * @param {string} path - Path to fetch (default: root)
   * @param {number} depth - Max depth to traverse (default: 2)
   * @param {string|null} token - GitHub token (optional)
   * @returns {Promise<Array>} File tree
   */
  async fetchFileTree(owner, repo, path = '', depth = 2, token = null) {
    const cacheKey = `tree:${owner}/${repo}:${path}:${depth}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const headers = this._getHeaders(token);
    
    try {
      // Try to get recursive tree
      const treeResponse = await fetch(
        `${this.baseUrl}/repos/${owner}/${repo}/git/trees/${path || 'HEAD'}?recursive=1`,
        { headers }
      );

      if (treeResponse.ok) {
        const treeData = await treeResponse.json();
        const files = this._filterTreeByDepth(treeData.tree || [], depth);
        this.cache.set(cacheKey, files);
        return files;
      }
    } catch (error) {
      console.warn('Failed to fetch git tree, falling back to contents API:', error);
    }

    // Fallback: use contents API
    const files = await this._fetchContentsRecursive(owner, repo, path, depth, token);
    this.cache.set(cacheKey, files);
    return files;
  }

  /**
   * Fetch file content
   * @param {string} owner - Repository owner
   * @param {string} repo - Repository name
   * @param {string} path - File path
   * @param {string|null} token - GitHub token (optional)
   * @returns {Promise<Object>} File content and metadata
   */
  async fetchFileContent(owner, repo, path, token = null) {
    const cacheKey = `content:${owner}/${repo}:${path}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const headers = this._getHeaders(token);
    const response = await fetch(
      `${this.baseUrl}/repos/${owner}/${repo}/contents/${path}`,
      { headers }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch file content: ${response.statusText}`);
    }

    const data = await response.json();
    const content = atob(data.content); // Decode base64

    const fileData = {
      path: data.path,
      name: data.name,
      size: data.size,
      content: content,
      encoding: data.encoding,
      sha: data.sha,
      language: this._detectLanguage(data.name, content),
    };

    this.cache.set(cacheKey, fileData);
    return fileData;
  }

  /**
   * Fetch README file
   * @param {string} owner - Repository owner
   * @param {string} repo - Repository name
   * @param {string|null} token - GitHub token (optional)
   * @returns {Promise<Object|null>} README content or null
   */
  async fetchREADME(owner, repo, token = null) {
    const cacheKey = `readme:${owner}/${repo}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const headers = this._getHeaders(token);
    
    try {
      const response = await fetch(
        `${this.baseUrl}/repos/${owner}/${repo}/readme`,
        { headers }
      );

      if (!response.ok) {
        return null;
      }

      const data = await response.json();
      const content = atob(data.content);
      
      // Truncate to 8000 chars
      const truncated = content.length > 8000 
        ? content.substring(0, 8000) + '...'
        : content;

      const readmeData = {
        content: truncated,
        format: this._detectReadmeFormat(data.name),
        path: data.path,
      };

      this.cache.set(cacheKey, readmeData);
      return readmeData;
    } catch (error) {
      console.warn('Failed to fetch README:', error);
      return null;
    }
  }

  /**
   * Fetch repository languages
   * @param {string} owner - Repository owner
   * @param {string} repo - Repository name
   * @param {string|null} token - GitHub token (optional)
   * @returns {Promise<Object>} Language breakdown
   */
  async fetchLanguages(owner, repo, token = null) {
    const cacheKey = `langs:${owner}/${repo}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const headers = this._getHeaders(token);
    const response = await fetch(
      `${this.baseUrl}/repos/${owner}/${repo}/languages`,
      { headers }
    );

    if (!response.ok) {
      return {};
    }

    const languages = await response.json();
    this.cache.set(cacheKey, languages);
    return languages;
  }

  /**
   * Fetch repository contributors
   * @param {string} owner - Repository owner
   * @param {string} repo - Repository name
   * @param {string|null} token - GitHub token (optional)
   * @returns {Promise<Array>} Contributors list
   */
  async fetchContributors(owner, repo, token = null) {
    const cacheKey = `contributors:${owner}/${repo}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const headers = this._getHeaders(token);
    const response = await fetch(
      `${this.baseUrl}/repos/${owner}/${repo}/contributors?per_page=10`,
      { headers }
    );

    if (!response.ok) {
      return [];
    }

    const contributors = await response.json();
    const simplified = contributors.map(c => ({
      login: c.login,
      id: c.id,
      avatar_url: c.avatar_url,
      contributions: c.contributions,
    }));

    this.cache.set(cacheKey, simplified);
    return simplified;
  }

  /**
   * Fetch repository issues (for context)
   * @param {string} owner - Repository owner
   * @param {string} repo - Repository name
   * @param {number} count - Number of issues to fetch
   * @param {string|null} token - GitHub token (optional)
   * @returns {Promise<Array>} Recent issues
   */
  async fetchRecentIssues(owner, repo, count = 5, token = null) {
    const cacheKey = `issues:${owner}/${repo}:${count}`;
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const headers = this._getHeaders(token);
    const response = await fetch(
      `${this.baseUrl}/repos/${owner}/${repo}/issues?state=open&per_page=${count}`,
      { headers }
    );

    if (!response.ok) {
      return [];
    }

    const issues = await response.json();
    const simplified = issues.map(i => ({
      number: i.number,
      title: i.title,
      body: i.body ? i.body.substring(0, 500) : '',
      created_at: i.created_at,
      labels: i.labels.map(l => l.name),
    }));

    this.cache.set(cacheKey, simplified);
    return simplified;
  }

  /**
   * Fetch file tree recursively using contents API
   * @private
   */
  async _fetchContentsRecursive(owner, repo, path, depth, token) {
    if (depth < 0) return [];

    const headers = this._getHeaders(token);
    const response = await fetch(
      `${this.baseUrl}/repos/${owner}/${repo}/contents/${path}`,
      { headers }
    );

    if (!response.ok) return [];

    const items = await response.json();
    if (!Array.isArray(items)) return [];

    const files = [];
    for (const item of items) {
      if (item.type === 'file') {
        files.push({
          path: item.path,
          name: item.name,
          type: 'blob',
          size: item.size,
          language: this._detectLanguage(item.name),
        });
      } else if (item.type === 'dir' && depth > 0) {
        const subFiles = await this._fetchContentsRecursive(
          owner, repo, item.path, depth - 1, token
        );
        files.push(...subFiles);
      }
    }

    return files;
  }

  /**
   * Filter tree by depth and exclude unwanted files
   * @private
   */
  _filterTreeByDepth(tree, maxDepth) {
    return tree.filter(item => {
      if (item.type !== 'blob') return false;
      
      const depth = (item.path.match(/\//g) || []).length;
      if (depth > maxDepth) return false;

      // Exclude common unwanted patterns
      const unwanted = [
        'node_modules/',
        '.git/',
        'dist/',
        'build/',
        'coverage/',
        '.next/',
        '.nuxt/',
        'vendor/',
        'target/',
        '*.min.js',
        '*.min.css',
        '*.map',
      ];

      return !unwanted.some(pattern => {
        if (pattern.startsWith('*')) {
          return item.path.endsWith(pattern.substring(1));
        }
        return item.path.includes(pattern);
      });
    });
  }

  /**
   * Detect language from filename and content
   * @private
   */
  _detectLanguage(filename, content = '') {
    const ext = filename.split('.').pop().toLowerCase();
    
    const langMap = {
      js: 'javascript',
      jsx: 'javascript',
      ts: 'typescript',
      tsx: 'typescript',
      py: 'python',
      go: 'go',
      rs: 'rust',
      java: 'java',
      rb: 'ruby',
      c: 'c',
      cpp: 'cpp',
      cc: 'cpp',
      cxx: 'cpp',
      h: 'c',
      hpp: 'cpp',
      cs: 'csharp',
      php: 'php',
      swift: 'swift',
      kt: 'kotlin',
      scala: 'scala',
      sh: 'shell',
      bash: 'shell',
      zsh: 'shell',
      md: 'markdown',
      json: 'json',
      yml: 'yaml',
      yaml: 'yaml',
      xml: 'xml',
      html: 'html',
      css: 'css',
      scss: 'scss',
      sass: 'sass',
      vue: 'vue',
      svelte: 'svelte',
    };

    return langMap[ext] || 'unknown';
  }

  /**
   * Detect README format
   * @private
   */
  _detectReadmeFormat(filename) {
    const ext = filename.split('.').pop().toLowerCase();
    if (ext === 'md') return 'markdown';
    if (ext === 'rst') return 'restructuredtext';
    if (ext === 'txt') return 'plaintext';
    return 'markdown';
  }

  /**
   * Get request headers
   * @private
   */
  _getHeaders(token) {
    const headers = {
      'Accept': 'application/vnd.github.v3+json',
      'User-Agent': 'SupremeAI-CodeFlow/1.0',
    };

    if (token) {
      headers['Authorization'] = `token ${token}`;
    }

    return headers;
  }

  /**
   * Clear cache
   */
  clearCache() {
    this.cache.clear();
  }

  /**
   * Get cache size
   */
  getCacheSize() {
    return this.cache.size;
  }
}

export default RepoIngestor;