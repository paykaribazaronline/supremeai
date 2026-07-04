# 📋 Commit e00bd7fbeda5ee0cf66421a9896d69f96285dcac

## Commit Stats
```
commit e00bd7fbeda5ee0cf66421a9896d69f96285dcac
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 04:30:59 2026 +0600

    fix(frontend): resolve ESLint unused variable and React refs rendering rule violations

 apps/studio-client/src/components/LiveSujonBackground.tsx     | 2 +-
 apps/studio-client/src/components/dashboard/FileTreePanel.tsx | 8 ++++----
 2 files changed, 5 insertions(+), 5 deletions(-)

```

## Diff Detail
```diff
commit e00bd7fbeda5ee0cf66421a9896d69f96285dcac
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 04:30:59 2026 +0600

    fix(frontend): resolve ESLint unused variable and React refs rendering rule violations

diff --git a/apps/studio-client/src/components/LiveSujonBackground.tsx b/apps/studio-client/src/components/LiveSujonBackground.tsx
index 3e84486b7..891095e48 100644
--- a/apps/studio-client/src/components/LiveSujonBackground.tsx
+++ b/apps/studio-client/src/components/LiveSujonBackground.tsx
@@ -263,7 +263,7 @@ export function LiveSujonBackground({ state: forcedState }: LiveSujonBackgroundP
         window.addEventListener('resize', resizeCanvas);
         resizeCanvas();
 
-        let lastStateId = -1;
+
 
         const render = (now: number) => {
             if (!isVisible || !glRef.current) return;
diff --git a/apps/studio-client/src/components/dashboard/FileTreePanel.tsx b/apps/studio-client/src/components/dashboard/FileTreePanel.tsx
index d8cbbbbb2..59b9ef37b 100644
--- a/apps/studio-client/src/components/dashboard/FileTreePanel.tsx
+++ b/apps/studio-client/src/components/dashboard/FileTreePanel.tsx
@@ -8,7 +8,7 @@ export const FileTreePanel: React.FC = () => {
   // By using useRef<Map>, we avoid triggering React renders for every single patch.
   // We only force a re-render when we specifically want to update the tree view (e.g. via a throttled update).
   const treeRef = useRef<Map<string, FileNode>>(new Map());
-  const [renderTick, setRenderTick] = useState(0);
+  const [treeMap, setTreeMap] = useState<Map<string, FileNode>>(new Map());
   const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set(['/']));
 
   useEffect(() => {
@@ -16,7 +16,7 @@ export const FileTreePanel: React.FC = () => {
     // Here we simulate an initial root.
     if (!treeRef.current.has('/')) {
       treeRef.current.set('/', { name: 'workspace', path: '/', type: 'directory', status: 'unchanged' });
-      setRenderTick(t => t + 1);
+      setTreeMap(new Map(treeRef.current));
     }
   }, [fileTreeData]);
 
@@ -53,11 +53,11 @@ export const FileTreePanel: React.FC = () => {
   };
 
   const renderNode = (path: string, depth: number = 0) => {
-    const node = treeRef.current.get(path);
+    const node = treeMap.get(path);
     if (!node) return null;
 
     const isExpanded = expandedFolders.has(path);
-    const children = Array.from(treeRef.current.values()).filter(n => {
+    const children = Array.from(treeMap.values()).filter(n => {
       if (n.path === path) return false;
       const parentPath = n.path.substring(0, n.path.lastIndexOf('/')) || '/';
       return parentPath === path;

```
