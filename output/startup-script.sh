#!/bin/bash
# SupremeAI Local Models Setup Script

echo "Starting SupremeAI local models setup..."

# Update system
sudo apt update
sudo apt upgrade -y

# Install required packages
sudo apt install -y curl wget git python3 python3-pip

# Install Ollama
echo "Installing Ollama..."
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama service
sudo systemctl enable ollama
sudo systemctl start ollama

# Wait for Ollama to start
sleep 10

# Pull models
echo "Pulling models..."
ollama pull qwen2.5-coder:7b
ollama pull llama3.1:8b
ollama pull nomic-embed-text

# Create startup service
sudo tee /etc/systemd/system/ollama-startup.service > /dev/null <<EOF
[Unit]
Description=Ollama Startup Service
After=network.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/ollama serve
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl enable ollama-startup

echo "SupremeAI local models setup complete!"
echo "VM is ready for use."