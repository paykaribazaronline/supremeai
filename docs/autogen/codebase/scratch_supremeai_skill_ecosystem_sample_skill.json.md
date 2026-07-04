# 📄 ফাইল: scratch/supremeai_skill_ecosystem/sample_skill.json

**প্রকার:** .json  
**সাইজ:** 419 বাইট  
**আপডেট:** 2026-07-04T10:39:00.794653

---

## কোড

```json
{
  "metadata": {
    "name": "ExampleSkill",
    "version": "v1.0",
    "description": "A sample skill for demonstration purposes",
    "author": "SupremeAI"
  },
  "inputs": [
    {"name": "input1", "type": "string", "required": true}
  ],
  "outputs": [
    {"name": "output1", "type": "string"}
  ],
  "implementation": {
    "language": "python",
    "code": "def run(input1):\n    return f'Hello {input1}'"
  }
}

```