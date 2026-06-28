import traceback
try:
    from core.app import app
    routes = [r.path for r in app.routes if hasattr(r, 'path')]
    print('Total routes:', len(routes))
    for path in routes:
        if 'voice' in path or 'tts' in path:
            print('Relevant route:', path)
except Exception as e:
    traceback.print_exc()
