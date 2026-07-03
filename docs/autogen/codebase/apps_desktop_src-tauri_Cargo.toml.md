# 📄 ফাইল: apps/desktop/src-tauri/Cargo.toml

**প্রকার:** .toml  
**সাইজ:** 699 বাইট  
**আপডেট:** 2026-07-03T21:37:07.755925

---

## কোড

```toml
[package]
name = "supremeai-desktop"
version = "0.1.0"
edition = "2021"
license = "MIT OR Apache-2.0"
repository = "https://github.com/supremeai/supremeai_2.0"
homepage = "https://supremeai.dev"
description = "SupremeAI 2.0 Desktop Application"
authors = ["SupremeAI Team"]

[features]
custom-protocol = ["tauri/custom-protocol"]

[build-dependencies]
tauri-build = { version = "=1.5.4", features = [] }

[dependencies]
tauri = { version = "=1.5.4", features = [ "window-maximize", "window-start-dragging", "window-unminimize", "window-unmaximize", "window-hide", "window-show", "window-minimize", "window-close", "notification", "global-shortcut"] }
serde_json = "1"
num_cpus = "1"
ntapi = "0.4.3"

```