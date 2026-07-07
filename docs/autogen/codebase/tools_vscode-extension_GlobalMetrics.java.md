# 📄 ফাইল: tools/vscode-extension/GlobalMetrics.java

**প্রকার:** .java  
**সাইজ:** 388 বাইট  
**আপডেট:** 2026-07-07T07:10:31.781823

---

## কোড

```java
package com.supremeai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalMetrics {
    private long patternsLearned;
    private long codeEdits;
    private long errorsReported;
    private long feedbackGiven;
    private long totalUsersActive;
}

```