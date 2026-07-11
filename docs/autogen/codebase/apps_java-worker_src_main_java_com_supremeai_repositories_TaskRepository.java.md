# 📄 ফাইল: apps/java-worker/src/main/java/com/supremeai/repositories/TaskRepository.java

**প্রকার:** .java  
**সাইজ:** 280 বাইট  
**আপডেট:** 2026-07-11T16:26:09.468349

---

## কোড

```java
package com.supremeai.repositories;

import com.supremeai.models.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {
}

```