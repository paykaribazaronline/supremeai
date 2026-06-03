# 🥇 SupremeAI Cinematic Design Guide
This document establishes the **Dashboard Home** as the benchmark for all UI/UX development within the SupremeAI project. All future tabs and components must adhere to these "Modern Admin" standards to ensure a cohesive, high-tech, cinematic experience.

---

## 🎨 1. Core Visual Identity

### Color Palette (Neon & Obsidian)
Always use the CSS variables defined in `index.css`:
- **Primary Accent**: `--neon-blue` (`#00f3ff`) - Used for "System Online", primary actions, and stable status.
- **Secondary Accent**: `--neon-purple` (`#bc13fe`) - Used for intelligence metrics, ML tasks, and high-tech flair.
- **Success**: `--success` (`#10b981`) - Used for "Healthy" or "Operational" states.
- **Danger**: `#ff4d4f` (Neon Red) - Used for anomalies or "Failed" states.
- **Base**: `--cyber-black` (`#020205`) with Glass overlays.

### Typography
- **Headings**: `'Outfit', sans-serif` (Bold/Extrabold, Letter spacing: -1px).
- **Technical/Data**: `'JetBrains Mono', monospace` (Used for timestamps, IDs, and logs).

---

## 🧱 2. UI Components (The "Glass" Standard)

### The Glass Card (`.glass-card`)
Every major section must be wrapped in a glass card.
```tsx
<div className="glass-card" style={{ height: '400px' }}>
  <div className="glass-card-title">
    Section Title
    <IconComponent style={{ color: 'var(--neon-blue)' }} />
  </div>
  {/* Content */}
</div>
```
*Rules:*
- Backdrop filter: `blur(12px)`.
- Border: `1px solid rgba(0, 243, 255, 0.15)`.
- Padding: `24px`.

### Buttons
- **Primary**: `.cyber-button` (Solid glow on hover).
- **Secondary/Icon**: `.glass-action-button` (Ghost border, subtle blur).
- **Danger**: `.cyber-danger-button` (Crimson glow).

### Gradients
Use the `.text-gradient` class for emphasized titles:
```tsx
<span className="text-gradient">Core Intelligence</span>
```

---

## 📊 3. Data Visualization

### Recharts Configuration
- **Gradients**: Always use `linearGradient` for Area charts (opacity 0.3 -> 0).
- **Tooltips**: Use the high-contrast dark style:
```tsx
contentStyle={{ 
  backgroundColor: 'rgba(13, 13, 18, 0.95)', 
  border: '1px solid var(--neon-blue)', 
  borderRadius: 8, 
  backdropFilter: 'blur(10px)' 
}}
```

### Tables
- Use `.cyber-table-row` for transparent backgrounds and neon hover states.
- Data columns should use `JetBrains Mono` for numbers and timestamps.

---

## 🎬 4. Motion & Feedback (`framer-motion`)

### Page Transitions
Every tab should start with a motion wrapper:
```tsx
<motion.div 
  initial={{ opacity: 0, y: 20 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ duration: 0.6 }}
>
  {/* Content */}
</motion.div>
```

### Hover Effects
Cards and buttons should use `whileHover={{ scale: 1.02 }}` for a tactile, responsive feel.

---

## 🗺️ 5. Layout Structure

### The "Command Center" Header
Every page must have a cinematic header:
1.  **System Trace**: Small text with a pulsing badge (e.g., "NEURAL LINK ACTIVE").
2.  **Hero Title**: Large bold title with a gradient.
3.  **Subtitle**: Explanatory text in `var(--text-dim)`.

### Grid System
- Use Ant Design `<Row gutter={[24, 24]}>`.
- Large displays should utilize `xl={16}` and `xl={8}` splits for a 2/3 - 1/3 visual balance.

---

## 📝 6. Copywriting & Tone
- **Bilingual**: Use Bengali for primary labels and English for technical terms (e.g., `1. 🥇 Dashboard (কমান্ড সেন্টার)`).
- **Persona**: Use a "High-Clearance AI" persona. Instead of "Loading", use "Syncing Neural Link...". Instead of "User", use "Operator" or "Architect".

---

## ✅ Checklist for Redesigning Other Tabs
- [ ] Added `framer-motion` entry animation?
- [ ] Replaced standard Cards with `.glass-card`?
- [ ] Applied `text-gradient` to the main header?
- [ ] Updated buttons to `cyber-button` or `glass-action-button`?
- [ ] Checked responsiveness (Mobile/Tablet)?
- [ ] Used `JetBrains Mono` for technical data strings?
