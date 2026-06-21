import os
import uvicorn
from core.app import app
from core.logging_config import setup_logging
from config import settings

# Initialize logging
setup_logging()

if __name__ == "__main__":
    port = int(os.environ.get("PORT", settings.port))
    is_local = settings.env == "local"
    if is_local:
        uvicorn.run("main:app", host=settings.host, port=port, reload=True)
    else:
        uvicorn.run(app, host=settings.host, port=port, reload=False)

