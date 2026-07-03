# 📄 ফাইল: apps/desktop/src-tauri/.cargo/config.toml

**প্রকার:** .toml  
**সাইজ:** 222 বাইট  
**আপডেট:** 2026-07-03T13:04:52.045077

---

## কোড

```toml
[target.x86_64-pc-windows-msvc]
rustflags = ["-C", "link-arg=/DEBUG:FASTLINK"]

[profile.dev]
split-debuginfo = "unpacked"
debug = "line-tables-only"

[profile.test]
split-debuginfo = "unpacked"
debug = "line-tables-only"

```