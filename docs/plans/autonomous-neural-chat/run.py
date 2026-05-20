import os
import sys
import json

# Add current directory to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Load configuration
def load_config():
    config_path = os.path.join(os.path.dirname(__file__), 'config.json')
    if os.path.exists(config_path):
        with open(config_path, 'r') as f:
            return json.load(f)
    return {
        "server": {
            "host": "localhost",
            "port": 5000,
            "debug": False
        }
    }

config = load_config()
server_config = config.get('server', {})
host = server_config.get('host', 'localhost')
port = server_config.get('port', 5000)
debug = server_config.get('debug', False)

print(f"Starting server on {host}:{port}")
print(f"Health check available at: http://{host}:{port}/health")
print(f"API status available at: http://{host}:{port}/api/status")
print()

# Import and run the app
from app import app, socketio

socketio.run(app, host=host, port=port, debug=debug)
