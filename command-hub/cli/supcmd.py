#!/usr/bin/env python3
"""
SupremeAI Command CLI Tool
Lightweight command-line interface for executing commands
"""

import sys
import json
import requests
import argparse
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional, Any
from tenacity import retry, stop_after_attempt, wait_exponential


class CommandCLI:
    """Command-line interface for SupremeAI commands"""
    
    def __init__(self, base_url: str = "http://localhost:8080", config_file: Optional[str] = None):
        self.base_url = base_url
        self.api_url = f"{base_url}/api/commands"
        self.config_file = config_file or Path.home() / ".supcmd" / "config.json"
        self.auth_token = self._load_auth_token()
    
    def _load_auth_token(self) -> str:
        """Load stored auth token from config"""
        try:
            if self.config_file.exists():
                with open(self.config_file) as f:
                    config = json.load(f)
                    return config.get("token", "")
        except:
            pass
        return ""
    
    def _save_auth_token(self, token: str):
        """Save auth token to config"""
        self.config_file.parent.mkdir(parents=True, exist_ok=True)
        with open(self.config_file, "w") as f:
            json.dump({"token": token}, f)
    
    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
    def _post_command(self, url: str, payload: dict, headers: dict):
        import requests
        return requests.post(url, json=payload, headers=headers, timeout=30)

    def execute_command(self, name: str, params: Dict[str, Any]) -> bool:
        """Execute a command via API"""
        try:
            print(f"📤 Executing: {name}")
            
            headers = {}
            if self.auth_token:
                headers["Authorization"] = f"Bearer {self.auth_token}"
            
            response = self._post_command(
                f"{self.api_url}/execute",
                payload={"name": name, "parameters": params},
                headers=headers
            )
            
            if response.status_code == 200:
                data = response.json()
                print("✅ Command succeeded")
                if data.get("data"):
                    self._print_json(data["data"])
                return True
                
            elif response.status_code == 202:
                data = response.json()
                print("⏳ Command queued (async)")
                print(f"   Job ID: {data.get('data', {}).get('jobId', 'unknown')}")
                return True
                
            elif response.status_code == 403:
                print("❌ Permission denied - authenticate with 'supcmd login'")
                return False
                
            else:
                print(f"❌ Command failed: {response.status_code}")
                print(response.text)
                return False
                
        except requests.exceptions.ConnectionError:
            print(f"❌ Cannot connect to {self.base_url}")
            print("   Make sure the API server is running")
            return False
        except Exception as e:
            print(f"❌ Error: {e}")
            return False
    
    def list_commands(self, category: Optional[str] = None, cmd_type: Optional[str] = None) -> bool:
        """List all available commands"""
        try:
            params = {}
            if category:
                params["category"] = category
            if cmd_type:
                params["type"] = cmd_type
            
            response = requests.get(
                f"{self.api_url}/list",
                params=params,
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                commands = data.get("commands", [])
                
                if not commands:
                    print("No commands found")
                    return True
                
                # Group by category
                by_category = {}
                for cmd in commands:
                    cat = cmd["category"]
                    if cat not in by_category:
                        by_category[cat] = []
                    by_category[cat].append(cmd)
                
                # Print by category
                for category in sorted(by_category.keys()):
                    print(f"\n📂 {category}")
                    for cmd in by_category[category]:
                        print(f"  • {cmd['name']:<20} {cmd['description']}")
                        print(f"    Type: {cmd['type']}")
                
                return True
            else:
                print(f"❌ Failed to list commands: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error: {e}")
            return False
    
    def get_command_info(self, name: str) -> bool:
        """Get detailed info about a command"""
        try:
            response = requests.get(
                f"{self.api_url}/{name}",
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                cmd = data["command"]
                
                print(f"\n📋 Command: {cmd['name']}")
                print(f"   Description: {cmd['description']}")
                print(f"   Category: {cmd['category']}")
                print(f"   Type: {cmd['type']} (sync/async)")
                print(f"   Permissions: {', '.join(cmd['permissions'])}")
                
                if "parameters" in data and data["parameters"]:
                    print(f"\n   Parameters:")
                    self._print_json(data["parameters"])
                
                return True
            elif response.status_code == 404:
                print(f"❌ Command not found: {name}")
                return False
            else:
                print(f"❌ Error: {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ Error: {e}")
            return False
    
    def login(self, token: str) -> bool:
        """Authenticate with API server"""
        try:
            # Verify token works by calling a protected endpoint
            headers = {"Authorization": f"Bearer {token}"}
            response = requests.get(
                f"{self.api_url}/list",
                headers=headers,
                timeout=10
            )
            
            if response.status_code == 200:
                self._save_auth_token(token)
                print("✅ Authenticated successfully")
                return True
            else:
                print("❌ Authentication failed - invalid token")
                return False
                
        except Exception as e:
            print(f"❌ Error: {e}")
            return False
    
    def health_check(self) -> bool:
        """Check API server health"""
        try:
            response = requests.get(
                f"{self.api_url}/health",
                timeout=5
            )
            
            if response.status_code == 200:
                print("✅ API server is healthy")
                return True
            else:
                print(f"❌ API server returned: {response.status_code}")
                return False
                
        except requests.exceptions.ConnectionError:
            print(f"❌ Cannot connect to API at {self.base_url}")
            return False
        except Exception as e:
            print(f"❌ Health check failed: {e}")
            return False
    
    def _print_json(self, obj: Any, indent: int = 2):
        """Pretty-print JSON object"""
        print(json.dumps(obj, indent=indent, default=str))


def main():
    parser = argparse.ArgumentParser(
        description="SupremeAI Command CLI - Execute system commands remotely",
        prog="supcmd"
    )
    
    parser.add_argument(
        "--url",
        default="http://localhost:8080",
        help="API server URL (default: http://localhost:8080)"
    )
    
    parser.add_argument(
        "--token",
        help="API authentication token"
    )
    
    subparsers = parser.add_subparsers(title="commands", dest="command")
    
    # Execute command
    exec_parser = subparsers.add_parser("exec", help="Execute a command")
    exec_parser.add_argument("name", help="Command name (e.g., health-check)")
    exec_parser.add_argument(
        "-p", "--param",
        action="append",
        nargs=2,
        metavar=("KEY", "VALUE"),
        help="Command parameter (can be used multiple times)"
    )
    
    # List commands
    list_parser = subparsers.add_parser("list", help="List available commands")
    list_parser.add_argument(
        "-c", "--category",
        help="Filter by category"
    )
    list_parser.add_argument(
        "-t", "--type",
        help="Filter by type (SYNC/ASYNC)"
    )
    
    # Show command info
    info_parser = subparsers.add_parser("info", help="Show command details")
    info_parser.add_argument("name", help="Command name")
    
    # Login
    login_parser = subparsers.add_parser("login", help="Authenticate with API")
    login_parser.add_argument("token", help="API authentication token")
    
    # Health
    health_parser = subparsers.add_parser("health", help="Check API health")
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        return 1
    
    cli = CommandCLI(base_url=args.url)
    
    # Override token if provided
    if args.token:
        cli.auth_token = args.token
    
    if args.command == "exec":
        params = {}
        if args.param:
            for key, value in args.param:
                params[key] = value
        success = cli.execute_command(args.name, params)
        return 0 if success else 1
    
    elif args.command == "list":
        success = cli.list_commands(category=args.category, cmd_type=args.type)
        return 0 if success else 1
    
    elif args.command == "info":
        success = cli.get_command_info(args.name)
        return 0 if success else 1
    
    elif args.command == "login":
        success = cli.login(args.token)
        return 0 if success else 1
    
    elif args.command == "health":
        success = cli.health_check()
        return 0 if success else 1
    
    return 0


if __name__ == "__main__":
    sys.exit(main())

