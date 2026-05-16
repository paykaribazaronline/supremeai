#!/bin/sh

# Generate config.toml from environment variables
cat <<EOF > /config.toml
[db]
data-source = "${DB_DATA_SOURCE}"

[jwt]
secret = "${JWT_SECRET}"

[tg]
app-id = ${TG_APP_ID}
app-hash = "${TG_APP_HASH}"

[server]
port = 8080
EOF

echo "🚀 Starting Teldrive with generated config.toml..."
exec /teldrive run --config /config.toml
