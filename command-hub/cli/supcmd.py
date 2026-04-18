#!/usr/bin/env python3
import requests
import json
import os
import argparse

BASE_URL = os.environ.get("SUPREMEAI_API_URL", "http://localhost:8080/api")
TOKEN_FILE = os.path.expanduser("~/.supremeai_token")

def save_token(token):
    with open(TOKEN_FILE, "w") as f:
        f.write(token)

def load_token():
    if not os.path.exists(TOKEN_FILE):
        return None
    with open(TOKEN_FILE, "r") as f:
        return f.read().strip()

def login(args):
    # In a real app, this would be a proper OAuth flow.
    # For now, we're just saving a Firebase ID token.
    print("Login is handled via the frontend. Please use a valid Firebase ID token.")
    print("You can save a token manually for CLI use:")
    token = input("Enter your Firebase ID Token: ")
    save_token(token)
    print("Token saved.")

def list_commands(args):
    headers = {"Authorization": f"Bearer {load_token()}"}
    response = requests.get(f"{BASE_URL}/commands/list", headers=headers)
    if response.status_code == 200:
        print(json.dumps(response.json(), indent=2))
    else:
        print(f"Error: {response.status_code}")
        print(response.text)

def execute_command(args):
    headers = {
        "Authorization": f"Bearer {load_token()}",
        "Content-Type": "application/json"
    }
    
    params = {}
    if args.params:
        for p in args.params:
            key, value = p.split('=', 1)
            # crude type inference
            if value.lower() in ['true', 'false']:
                params[key] = bool(value)
            elif value.isdigit():
                params[key] = int(value)
            else:
                try:
                    params[key] = float(value)
                except ValueError:
                    params[key] = value

    payload = {
        "name": args.command_name,
        "parameters": params
    }

    response = requests.post(f"{BASE_URL}/commands/execute", headers=headers, json=payload)
    
    if response.status_code == 200:
        print(json.dumps(response.json(), indent=2))
    else:
        print(f"Error: {response.status_code}")
        print(response.text)

def main():
    parser = argparse.ArgumentParser(description="SupremeAI CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # Login
    login_parser = subparsers.add_parser("login", help="Authenticate the CLI")
    login_parser.set_defaults(func=login)

    # List
    list_parser = subparsers.add_parser("list", help="List available commands")
    list_parser.set_defaults(func=list_commands)

    # Execute
    exec_parser = subparsers.add_parser("exec", help="Execute a command")
    exec_parser.add_argument("command_name", help="The name of the command to execute")
    exec_parser.add_argument("-p", "--param", dest="params", action="append", help="Parameter for the command in key=value format")
    exec_parser.set_defaults(func=execute_command)

    args = parser.parse_args()
    args.func(args)

if __name__ == "__main__":
    main()
