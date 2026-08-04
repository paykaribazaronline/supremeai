import os
import re

from core.error_bus import with_error_bus


@with_error_bus("main")
def main():
    backend_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    count = 0

    # Regex to find ErrorEvent(..., severity="...") and inject structured_context
    # It's tricky because of multi-line. We will just use ast or simple regex.
    # Actually, if we just do: `ErrorEvent(` ... `)` replacement.

    for root, _, files in os.walk(backend_dir):
        for file in files:
            if file.endswith(".py"):
                filepath = os.path.join(root, file)
                with open(filepath, encoding="utf-8") as f:
                    content = f.read()

                # Check if ErrorEvent is in the file
                if "ErrorEvent(" not in content or "class ErrorEvent" in content:
                    continue

                new_content = re.sub(
                    r'(severity=[\'"][^\'"]+[\'"])(?!\s*,\s*structured_context)',
                    r'\1, structured_context=ErrorContext(module="auto_fixed")',
                    content,
                )

                if new_content != content:
                    # check if ErrorContext is imported, if not add it
                    if "ErrorContext" not in new_content:
                        # find where ErrorEvent is imported
                        new_content = re.sub(
                            r"from core\.messaging\.event_bus import (.*?)ErrorEvent(.*?)",
                            r"from core.messaging.event_bus import \1ErrorEvent, ErrorContext\2",
                            new_content,
                        )
                        if "ErrorContext" not in new_content:
                            new_content = "from core.messaging.event_bus import ErrorContext\n" + new_content
                    with open(filepath, "w", encoding="utf-8") as f:
                        f.write(new_content)
                    count += 1
                    print(f"Fixed {filepath}")

    print(f"Total files fixed: {count}")


if __name__ == "__main__":
    main()
