import os

test_files = [
    'test_api.py', 
    'test_e2e.py', 
    'test_context_and_actions.py', 
    'test_task_endpoints.py'
]

for filename in test_files:
    filepath = os.path.join(r'c:\Users\n\supremeai\supremeai_2.0\backend\tests', filename)
    if not os.path.exists(filepath): continue
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = content.replace('import core.app as app_mod', 'import core.services as services_mod')
    content = content.replace('app_mod.intent_parser', 'services_mod.intent_parser')
    content = content.replace('app_mod.model_router', 'services_mod.model_router')
    content = content.replace('app_mod.admin_god', 'services_mod.admin_god')
    
    content = content.replace("patch('core.app.model_router", "patch('core.services.model_router")
    content = content.replace("patch('core.app.admin_god", "patch('core.services.admin_god")
    content = content.replace('patch("core.app.model_router', 'patch("core.services.model_router')
    content = content.replace('patch("core.app.admin_god', 'patch("core.services.admin_god')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Updated {filename}')
