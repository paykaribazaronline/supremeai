# 📋 Commit bf9cc13ab5a0a4a5f8fe375c4bac491edb09a660

## Commit Stats
```
commit bf9cc13ab5a0a4a5f8fe375c4bac491edb09a660
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 04:10:17 2026 +0600

    test(studio-client): fix vitest errors by mocking useStore correctly

 apps/studio-client/src/App.test.tsx | 2 ++
 1 file changed, 2 insertions(+)

```

## Diff Detail
```diff
commit bf9cc13ab5a0a4a5f8fe375c4bac491edb09a660
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 04:10:17 2026 +0600

    test(studio-client): fix vitest errors by mocking useStore correctly

diff --git a/apps/studio-client/src/App.test.tsx b/apps/studio-client/src/App.test.tsx
index 2ea10c8d7..8adf40dde 100644
--- a/apps/studio-client/src/App.test.tsx
+++ b/apps/studio-client/src/App.test.tsx
@@ -45,6 +45,8 @@ const storeState = {
   forgeFeedback: null,
   forgeSuccessCode: null,
   forgeNewSkill: mockForgeNewSkill,
+  isConfigLoaded: true,
+  setConfig: vi.fn(),
 };
 
 vi.mock('./store/useStore', () => ({

```
