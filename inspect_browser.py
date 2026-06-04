from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=False)
    page = browser.new_page(viewport={'width': 1280, 'height': 800})

    console_errors = []
    page_errors = []

    page.on("console", lambda msg: console_errors.append(f"[{msg.type}] {msg.text}") if msg.type in ("error", "warning") else None)
    page.on("pageerror", lambda exc: page_errors.append(str(exc)))

    page.goto('https://supremeai-a.web.app')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(3000)

    page.screenshot(path='C:/tmp/page_discovery.png', full_page=True)
    print("Screenshot saved to C:/tmp/page_discovery.png")

    print("\nConsole errors/warnings:")
    for e in console_errors[:20]:
        print(e)

    print("\nPage errors:")
    for e in page_errors[:20]:
        print(e)

    print("\nURL:", page.url)
    print("Title:", page.title())

    page.wait_for_timeout(60000)
    browser.close()
