# 📄 ফাইল: skills/dynamic/text_summarizer.py

**প্রকার:** .py  
**সাইজ:** 500 বাইট  
**আপডেট:** 2026-07-07T17:46:01.257423

---

## কোড

```py
def run(text: str, max_sentences: int = 3):
    """Summarizes text by extracting the first few sentences."""
    if not text:
        return ""
    # Simple sentence splitter
    sentences = [s.strip() for s in text.replace("\n", " ").split(".") if s.strip()]
    summary = ". ".join(sentences[:max_sentences])
    if len(sentences) > max_sentences:
        summary += "."
    return {
        "original_length": len(text),
        "summary": summary,
        "sentences_count": len(sentences)
    }

```