#!/usr/bin/env python3
import requests
import json
import os
import argparse

BASE_URL = os.environ.get("SUPREMEAI_API_URL", "https://supremeai-a.web.app/api")
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

def system_learning_improve(args):
    """Trigger system learning improvement cycle."""
    headers = {
        "Authorization": f"Bearer {load_token()}",
        "Content-Type": "application/json"
    }
    
    response = requests.post(f"{BASE_URL}/api/system-learning/improve", headers=headers)
    
    if response.status_code == 200:
        data = response.json()
        print("✅ System learning improvement cycle completed!")
        print(f"\n📊 Analysis Results:")
        print(f"   • Total learnings analyzed: {data.get('summary', {}).get('totalLearningsAnalyzed', 0)}")
        print(f"   • Improvements identified: {data.get('summary', {}).get('improvementsIdentified', 0)}")
        print(f"   • Optimizations applied: {data.get('summary', {}).get('optimizationsApplied', 0)}")
        print(f"   • Recommendations generated: {data.get('summary', {}).get('recommendationsGenerated', 0)}")
        
        recommendations = data.get('recommendations', [])
        if recommendations:
            print(f"\n💡 Recommendations:")
            for i, rec in enumerate(recommendations[:5], 1):  # Show top 5
                print(f"   {i}. {rec}")
        
        optimization = data.get('optimization', {})
        if optimization:
            print(f"\n🔧 Optimizations:")
            for action in optimization.get('actions', [])[:3]:  # Show top 3
                print(f"   • {action}")
    else:
        print(f"❌ Error: {response.status_code}")
        print(response.text)

def system_learning_status(args):
    """View system learning status and statistics."""
    headers = {"Authorization": f"Bearer {load_token()}"}
    response = requests.get(f"{BASE_URL}/api/system-learning/stats", headers=headers)
    
    if response.status_code == 200:
        stats = response.json()
        print("📈 System Learning Status:\n")
        print(f"Total learnings: {stats.get('total', 0)}")
        print(f"Success rate: {stats.get('successRate', 0)*100:.1f}%")
        print(f"Average quality: {stats.get('averageQuality', 0):.2f}")
        
        by_type = stats.get('byType', {})
        if by_type:
            print("\n📚 Learning Types:")
            for ltype, count in by_type.items():
                print(f"   • {ltype}: {count}")
    else:
        print(f"❌ Error: {response.status_code}")
        print(response.text)

def system_learning_improve(args):
    """Trigger system learning improvement cycle."""
    headers = {
        "Authorization": f"Bearer {load_token()}",
        "Content-Type": "application/json"
    }
    
    response = requests.post(f"{BASE_URL}/api/system-learning/improve", headers=headers)
    
    if response.status_code == 200:
        data = response.json()
        print("✅ System learning improvement cycle completed!")
        print(f"\n📊 Analysis Results:")
        print(f"   • Total learnings analyzed: {data.get('summary', {}).get('totalLearningsAnalyzed', 0)}")
        print(f"   • Improvements identified: {data.get('summary', {}).get('improvementsIdentified', 0)}")
        print(f"   • Optimizations applied: {data.get('summary', {}).get('optimizationsApplied', 0)}")
        print(f"   • Recommendations generated: {data.get('summary', {}).get('recommendationsGenerated', 0)}")
        
        recommendations = data.get('recommendations', [])
        if recommendations:
            print(f"\n💡 Recommendations:")
            for i, rec in enumerate(recommendations[:5], 1):  # Show top 5
                print(f"   {i}. {rec}")
        
        optimization = data.get('optimization', {})
        if optimization:
            print(f"\n🔧 Optimizations:")
            for action in optimization.get('actions', [])[:3]:  # Show top 3
                print(f"   • {action}")
    else:
        print(f"❌ Error: {response.status_code}")
        print(response.text)

def system_learning_status(args):
    """View system learning status and statistics."""
    headers = {"Authorization": f"Bearer {load_token()}"}
    response = requests.get(f"{BASE_URL}/api/system-learning/stats", headers=headers)
    
    if response.status_code == 200:
        stats = response.json()
        print("📈 System Learning Status:\n")
        print(f"Total learnings: {stats.get('total', 0)}")
        print(f"Success rate: {stats.get('successRate', 0)*100:.1f}%")
        print(f"Average quality: {stats.get('averageQuality', 0):.2f}")
        
        by_type = stats.get('byType', {})
        if by_type:
            print("\n📚 Learning Types:")
            for ltype, count in by_type.items():
                print(f"   • {ltype}: {count}")
    else:
        print(f"❌ Error: {response.status_code}")
        print(response.text)

def main():
    parser = argparse.ArgumentParser(description="SupremeAI CLI - System Learning & Improvement Tool")
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

    # System Learning Commands
    system_parser = subparsers.add_parser("system", help="System management commands")
    system_subparsers = system_parser.add_subparsers(dest="system_command", required=True)
    
    # System learning improve
    improve_parser = system_subparsers.add_parser("learning improve", 
        help="Improve system learning from collected data")
    improve_parser.set_defaults(func=system_learning_improve)
    
    # System learning status
    status_parser = system_subparsers.add_parser("learning status",
        help="View system learning status and statistics")
    status_parser.set_defaults(func=system_learning_status)

    args = parser.parse_args()
    args.func(args)

if __name__ == "__main__":
    main()
