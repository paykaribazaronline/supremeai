
filepath = "tests/test_docker_sandbox.py"
with open(filepath, encoding="utf-8") as f:
    content = f.read()

# Replace patch("os.getenv", return_value="true") with patching settings
content = content.replace('patch("os.getenv", return_value="true")', 'patch("core.config.settings.allow_local_sandbox_fallback", "true")')

# And for test_execute_command_no_fallback_in_prod and no_fallback_if_disallowed:
# Wait, they also mock os.getenv to return "false". Let's check how they do it.
# Actually let's just make it simpler: tools/docker_sandbox.py should check os.getenv if we want to be consistent with other modules!
