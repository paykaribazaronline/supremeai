"""
Multi-Language Code Generator for Reverse Engineering
Generates connectors in 6 languages: Python, TypeScript, Java, Swift, C#, Go
"""

import re
from datetime import datetime
from typing import List


class ConnectorGenerator:
    """
    Generates client SDK/connector code for reverse-engineered APIs.
    Supports: Python, TypeScript, Java, Swift, C#, Go.
    """

    LANGUAGE_EXTENSIONS = {
        "python": "py",
        "typescript": "ts",
        "java": "java",
        "swift": "swift",
        "csharp": "cs",
        "go": "go"
    }

    AUTH_TEMPLATES = {
        "none": {
            "header": lambda base_url: "",
            "init": "",
            "auth_call": ""
        },
        "Session-based": {
            "header": "",
            "init": "self.session = requests.Session()\n        self.authenticated = False",
            "auth_call": "self._login()"
        },
        "JWT Bearer": {
            "header": "Authorization: Bearer {token}",
            "init": "self.token = credentials.get('token') if credentials else None",
            "auth_call": "self.session.headers.update({'Authorization': f'Bearer {self.token}'})"
        },
        "OAuth2": {
            "header": "Authorization: Bearer {access_token}",
            "init": "self.access_token = None",
            "auth_call": "# OAuth2 flow handled separately\n        self.access_token = credentials.get('access_token')"
        }
    }

    def __init__(self, platform_name: str, base_url: str, auth_type: str, endpoints: list, language: str = "python"):
        self.platform_name = platform_name
        self.base_url = base_url.rstrip('/')
        self.auth_type = auth_type if auth_type in self.AUTH_TEMPLATES else "none"
        self.endpoints = endpoints or []
        self.language = language.lower()
        self.class_name = self._to_pascal_case(platform_name)

    def _to_pascal_case(self, s: str) -> str:
        s = re.sub(r'[^a-zA-Z0-9_]', '_', s)
        return ''.join(word.capitalize() for word in s.split('_') if word)

    def _indent(self, lines: List[str], spaces: int = 4) -> str:
        indent_str = ' ' * spaces
        return '\n'.join(indent_str + line if line.strip() else line for line in lines)

    # ─────────────────────────────────────────────────────────────────────────────
    # Python Generator
    # ─────────────────────────────────────────────────────────────────────────────
    def _generate_python(self) -> str:
        lines = []
        # Header
        lines.append(f"# Auto-generated connector for {self.platform_name}")
        lines.append(f"# Generated: {datetime.now().isoformat()}")
        lines.append(f"# Auth: {self.auth_type}")
        lines.append('"""Auto-generated API client."""')
        lines.append("")
        lines.append("import requests")
        lines.append("from typing import Dict, Any, Optional")
        lines.append("")

        # Class
        lines.append(f"class {self.class_name}Connector:")
        lines.append(f'    """Client for {self.platform_name} API"""')
        lines.append("")

        # __init__
        lines.append("    def __init__(self, credentials: Optional[Dict[str, str]] = None):")
        lines.append(f'        self.base_url = "{self.base_url}"')
        lines.append("        self.session = requests.Session()")
        lines.append("        self.credentials = credentials or {}")
        lines.append("")

        # authenticate
        lines.append("    def authenticate(self) -> bool:")
        lines.append('        """Perform authentication based on detected auth type"""')
        if self.auth_type == "Session-based":
            lines.append("        login_data = {")
            lines.append('            "email": self.credentials.get("email"),')
            lines.append('            "password": self.credentials.get("password")')
            lines.append("        }")
            lines.append(f'        resp = self.session.post(f"{{self.base_url}}/api/login", json=login_data)')
            lines.append("        return resp.status_code == 200")
        elif self.auth_type == "JWT Bearer":
            lines.append('        token = self.credentials.get("token")')
            lines.append('        if not token:')
            lines.append('            raise ValueError("JWT token required in credentials")')
            lines.append('        self.session.headers.update({"Authorization": f"Bearer {token}"}) ')
            lines.append("        return True")
        else:
            lines.append("        # No authentication required or custom")
            lines.append("        return True")
        lines.append("")

        # API methods per endpoint
        for ep in self.endpoints:
            method_name = self._to_method_name(ep)
            http_method = "POST"  # default; could infer
            lines.append(f"    def {method_name}(self, **kwargs) -> Dict[str, Any]:")
            lines.append(f'        """Call {ep} endpoint"""')
            lines.append(f'        url = f"{{self.base_url}}{ep}"')
            lines.append(f'        resp = self.session.{http_method.lower()}(url, json=kwargs)')
            lines.append("        resp.raise_for_status()")
            lines.append("        return resp.json()")
            lines.append("")

        lines.append("    def health(self) -> bool:")
        lines.append('        """Check if service is reachable"""')
        lines.append('        try:')
        lines.append('            resp = self.session.get(f"{self.base_url}/health")')
        lines.append('            return resp.status_code == 200')
        lines.append('        except Exception:')
        lines.append('            return False')

        return "\n".join(lines)

    # ─────────────────────────────────────────────────────────────────────────────
    # TypeScript Generator
    # ─────────────────────────────────────────────────────────────────────────────
    def _generate_typescript(self) -> str:
        lines = []
        lines.append(f"// Auto-generated connector for {self.platform_name}")
        lines.append(f"// Generated: {datetime.now().isoformat()}")
        lines.append("")
        lines.append("export interface ApiConfig {")
        lines.append('  baseUrl: string;')
        lines.append('  authToken?: string;')
        lines.append('  timeoutMs?: number;')
        lines.append("}")
        lines.append("")
        class_name = f"{self.class_name}Client"
        lines.append(f"export class {class_name} {{")
        lines.append('  private baseUrl: string;')
        lines.append('  private authToken?: string;')
        lines.append('  private timeout: number;')
        lines.append("")
        lines.append("  constructor(config: ApiConfig) {")
        lines.append('    this.baseUrl = config.baseUrl;')
        lines.append('    this.authToken = config.authToken;')
        lines.append('    this.timeout = config.timeoutMs || 30000;')
        lines.append("  }")
        lines.append("")
        # authenticate method placeholder
        lines.append("  async authenticate(): Promise<boolean> {")
        lines.append('    // Implement auth based on your provider')
        lines.append('    return true;')
        lines.append("  }")
        lines.append("")
        # API methods
        for ep in self.endpoints[:3]:  # limit sample
            method_name = self._to_method_name(ep)
            lines.append(f"  async {method_name}(params: any): Promise<any> {{")
            lines.append(f'    const response = await fetch(`${{this.baseUrl}}{ep}`, {{')
            lines.append('      method: "POST",')
            lines.append('      headers: {')
            lines.append('        "Content-Type": "application/json",')
            if self.auth_type == "JWT Bearer":
                lines.append('        "Authorization": `Bearer ${this.authToken}`')
            lines.append('      },')
            lines.append('      body: JSON.stringify(params)')
            lines.append('    });')
            lines.append('    if (!response.ok) {')
            lines.append('      throw new Error(`API error: ${{response.status}}`);')
            lines.append('    }')
            lines.append('    return response.json();')
        lines.append("  }")
        lines.append("}")
        return "\n".join(lines)

    # ─────────────────────────────────────────────────────────────────────────────
    # Java Generator
    # ─────────────────────────────────────────────────────────────────────────────
    def _generate_java(self) -> str:
        lines = []
        pkg = f"com.{self.platform_name.lower()}.client"
        class_name = f"{self.class_name}Client"
        lines.append(f"package {pkg};")
        lines.append("")
        lines.append("import okhttp3.*;")
        lines.append("import java.io.IOException;")
        lines.append("import java.util.Map;")
        lines.append("import com.google.gson.Gson;")
        lines.append("")
        lines.append(f"public class {class_name} {{")
        lines.append('  private final OkHttpClient client = new OkHttpClient();')
        lines.append(f'  private final String baseUrl = "{self.base_url}";')
        lines.append('  private String token;')
        lines.append("")
        lines.append("  public void setToken(String token) { this.token = token; }")
        lines.append("")
        lines.append("  public boolean authenticate(String email, String password) throws IOException {")
        if self.auth_type == "Session-based":
            lines.append('    // Session-based: maintain cookies automatically via OkHttp')
            lines.append('    okhttp3.Request request = new okhttp3.Request.Builder()')
            lines.append(f'        .url(baseUrl + "/api/login")')
            lines.append('        .post(new FormBody.Builder()')
            lines.append('            .add("email", email)')
            lines.append('            .add("password", password)')
            lines.append('            .build())')
            lines.append('        .build();')
            lines.append('    try (Response response = client.newCall(request).execute()) {')
            lines.append('      return response.isSuccessful();')
            lines.append('    }')
        else:
            lines.append('    // JWT or other token-based')
            lines.append('    // TODO: implement')
            lines.append('    return true;')
        lines.append("  }")
        lines.append("")
        # Sample method
        if self.endpoints:
            ep = self.endpoints[0]
            method_name = self._to_method_name(ep)
            lines.append(f"  public String {method_name}(Map<String, String> params) throws IOException {{")
            lines.append('    okhttp3.Request request = new okhttp3.Request.Builder()')
            lines.append(f'        .url(baseUrl + "{ep}")')
            lines.append('        .post(new FormBody.Builder()')
            lines.append('            // add params')
            lines.append('            .build())')
            lines.append('        .build();')
            lines.append('    try (Response response = client.newCall(request).execute()) {')
            lines.append('      return response.body().string();')
            lines.append('    }')
            lines.append("  }")
        lines.append("}")
        return "\n".join(lines)

    # ─────────────────────────────────────────────────────────────────────────────
    # Swift Generator
    # ─────────────────────────────────────────────────────────────────────────────
    def _generate_swift(self) -> str:
        lines = []
        class_name = f"{self.class_name}Client"
        lines.append("// Auto-generated Swift client")
        lines.append("import Foundation")
        lines.append("")
        lines.append(f"class {class_name} {{")
        lines.append(f'  let baseURL = URL(string: "{self.base_url}")!')
        lines.append('  var session: URLSession')
        lines.append('  var authToken: String?')
        lines.append("")
        lines.append("  init() {")
        lines.append('    let config = URLSessionConfiguration.default')
        lines.append('    self.session = URLSession(configuration: config)')
        lines.append("  }")
        lines.append("")
        lines.append("  func authenticate(email: String, password: String) async throws -> Bool {")
        if self.auth_type == "Session-based":
            lines.append('    var request = URLRequest(url: baseURL.appendingPathComponent("/api/login"))')
            lines.append('    request.httpMethod = "POST"')
            lines.append('    request.setValue("application/json", forHTTPHeaderField: "Content-Type")')
            lines.append('    let payload = ["email": email, "password": password]')
            lines.append('    request.httpBody = try? JSONSerialization.data(withJSONObject: payload)')
            lines.append('    let (data, response) = try await session.data(for: request)')
            lines.append('    guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {')
            lines.append('      return false')
            lines.append('    }')
            lines.append('    // Store cookies if needed')
            lines.append('    return true')
        else:
            lines.append('    // Implement token-based auth')
            lines.append('    return true')
        lines.append("  }")
        lines.append("")
        if self.endpoints:
            ep = self.endpoints[0]
            method_name = self._to_method_name(ep)
            lines.append(f"  func {method_name}(params: [String: Any]) async throws -> [String: Any] {{")
            lines.append('    var request = URLRequest(url: baseURL.appendingPathComponent("' + ep + '"))')
            lines.append('    request.httpMethod = "POST"')
            lines.append('    request.setValue("application/json", forHTTPHeaderField: "Content-Type")')
            lines.append('    if let token = authToken {')
            lines.append('      request.setValue("Bearer \\(token)", forHTTPHeaderField: "Authorization")')
            lines.append('    }')
            lines.append('    request.httpBody = try? JSONSerialization.data(withJSONObject: params)')
            lines.append('    let (data, response) = try await session.data(for: request)')
            lines.append('    guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {')
            lines.append('      throw NSError(domain: "APIError", code: 0)')
            lines.append('    }')
            lines.append('    guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {')
            lines.append('      throw NSError(domain: "ParseError", code: 0)')
            lines.append('    }')
            lines.append('    return json')
        lines.append("  }")
        lines.append("}")
        return "\n".join(lines)

    # ─────────────────────────────────────────────────────────────────────────────
    # C# Generator
    # ─────────────────────────────────────────────────────────────────────────────
    def _generate_csharp(self) -> str:
        lines = []
        lines.append("// Auto-generated C# client")
        lines.append("using System;")
        lines.append("using System.Net.Http;")
        lines.append("using System.Net.Http.Json;")
        lines.append("using System.Text;")
        lines.append("using System.Text.Json;")
        lines.append("using System.Threading.Tasks;")
        lines.append("")
        class_name = f"{self.class_name}Client"
        lines.append(f"public class {class_name} {{")
        lines.append(f'  private readonly HttpClient _client;')
        lines.append(f'  private readonly string _baseUrl = "{self.base_url}";')
        lines.append("")
        lines.append("  public " + class_name + "() {")
        lines.append('    _client = new HttpClient();')
        lines.append('    _client.BaseAddress = new Uri(_baseUrl);')
        lines.append("  }")
        lines.append("")
        lines.append("  public async Task<bool> AuthenticateAsync(string email, string password) {")
        if self.auth_type == "Session-based":
            lines.append('    var content = new FormUrlEncodedContent(new[] {')
            lines.append('      new KeyValuePair<string, string>("email", email),')
            lines.append('      new KeyValuePair<string, string>("password", password)')
            lines.append('    });')
            lines.append('    var response = await _client.PostAsync("/api/login", content);')
            lines.append('    return response.IsSuccessStatusCode;')
        else:
            lines.append('    // TODO: token-based')
            lines.append('    return true;')
        lines.append("  }")
        lines.append("")
        if self.endpoints:
            ep = self.endpoints[0]
            method_name = self._to_method_name(ep)
            lines.append(f"  public async Task<JsonElement> {method_name}Async(object payload) {{")
            lines.append('    var response = await _client.PostAsJsonAsync("' + ep + '", payload);')
            lines.append('    response.EnsureSuccessStatusCode();')
            lines.append('    var stream = await response.Content.ReadAsStreamAsync();')
            lines.append('    return await JsonSerializer.DeserializeAsync<JsonElement>(stream);')
            lines.append("  }")
        lines.append("}")
        return "\n".join(lines)

    # ─────────────────────────────────────────────────────────────────────────────
    # Go Generator
    # ─────────────────────────────────────────────────────────────────────────────
    def _generate_go(self) -> str:
        lines = []
        lines.append("// Auto-generated Go client")
        lines.append("package main")
        lines.append("")
        lines.append("import (")
        lines.append('  "net/http"')
        lines.append('  "net/url"')
        lines.append('  "encoding/json"')
        lines.append('  "fmt"')
        lines.append(")")
        lines.append("")
        struct_name = f"{self.class_name}Client"
        lines.append(f"type {struct_name} struct {{")
        lines.append('  BaseURL *url.URL')
        lines.append('  Client  *http.Client')
        lines.append('  Token   string')
        lines.append("}")
        lines.append("")
        lines.append(f"func New{struct_name}() *{struct_name} {{")
        lines.append(f'  base, _ := url.Parse("{self.base_url}")')
        lines.append(f'  return &{struct_name}{{')
        lines.append('    BaseURL: base,')
        lines.append('    Client:  &http.Client{},')
        lines.append('  }')
        lines.append("}")
        lines.append("")
        lines.append("func (c *" + struct_name + ") Authenticate(email, password string) error {")
        if self.auth_type == "Session-based":
            lines.append('  // TODO: implement session cookie handling')
            lines.append('  return nil')
        else:
            lines.append('  // Set token if provided')
            lines.append('  return nil')
        lines.append("}")
        lines.append("")
        if self.endpoints:
            ep = self.endpoints[0]
            method_name = self._to_method_name(ep)
            lines.append(f"func (c *{struct_name}) {method_name}(params map[string]string) (map[string]interface{}, error) {{")
            lines.append('  // Build request')
            lines.append('  // ...')
            lines.append('  return nil, nil')
        lines.append("}")
        return "\n".join(lines)

    # ─────────────────────────────────────────────────────────────────────────────
    # Public API
    # ─────────────────────────────────────────────────────────────────────────────

    def generate(self) -> str:
        lang = self.language
        if lang == "python":
            return self._generate_python()
        elif lang == "typescript":
            return self._generate_typescript()
        elif lang == "java":
            return self._generate_java()
        elif lang == "swift":
            return self._generate_swift()
        elif lang == "csharp":
            return self._generate_csharp()
        elif lang == "go":
            return self._generate_go()
        else:
            # Default to python
            return self._generate_python()

    def _to_method_name(self, endpoint: str) -> str:
        # Convert /api/users -> getUsers or call_api generic
        clean = endpoint.strip('/').replace('/', '_').replace('-', '_')
        clean = re.sub(r'[^a-zA-Z0-9_]', '', clean)
        if not clean:
            return "callApi"
        return clean.lower() + "_api" if not clean.isalnum() else clean
