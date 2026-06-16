```markdown
# 🖊️ Wibley Annotation

> **The intelligent, local-first workspace for deep reading, structured thinking, and seamless knowledge management.**

![GitHub release (latest by date)](https://img.shields.io/github/v/release/wibley/wibley-annotation)
![Build Status](https://img.shields.io/github/actions/workflow/status/wibley/wibley-annotation/ci.yml)
![License](https://img.shields.io/github/license/wibley/wibley-annotation)
![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Windows%20%7C%20Linux-lightgrey)

## 📖 Overview

**Wibley Annotation** is a modern, high-performance note-taking and annotation engine designed for researchers, developers, and lifelong learners. Unlike traditional text editors, Wibley treats every highlight, note, and code snippet as a first-class relational object. 

Built on a **local-first architecture**, your data remains entirely yours, while our optional sync and AI layers provide the power of cloud computing without sacrificing privacy.

## ✨ Key Features

- **Multi-Modal Annotations:** Seamlessly annotate PDFs, EPUBs, web pages, and local code repositories in a unified interface.
- **Bi-Directional Linking (Zettelkasten):** Connect your thoughts effortlessly using `[[wikilinks]]` and interactive graph views to visualize your knowledge base.
- **Semantic AI Tagging:** Use offline-capable LLMs to automatically generate summaries, extract entities, and suggest tags for your highlights.
- **Markdown & LaTeX Native:** Full support for CommonMark, extended GitHub Flavored Markdown (GFM), and MathJax for complex equations.
- **Local-First & Private:** Your database is stored locally using SQLite. End-to-end encrypted (E2EE) sync is available for cross-device workflows.
- **Distraction-Free Focus Mode:** A minimalist UI that fades away when you write, featuring customizable typography and themes.

## 🚀 Getting Started

### Prerequisites
- Node.js (v18+)
- Rust (latest stable) and Cargo
- [Tauri CLI](https://tauri.app/v1/guides/getting-started/prerequisites)

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/wibley/wibley-annotation.git
   cd wibley-annotation
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Run in development mode:**
   ```bash
   npm run tauri dev
   ```

4. **Build for production:**
   ```bash
   npm run tauri build
   ```

## 🛠️ Tech Stack

Wibley Annotation is built using a modern, performance-oriented stack:

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Core Engine** | Rust / Tauri | Native performance, low memory footprint, OS-level security. |
| **Frontend** | Svelte / TypeScript | Reactive UI, minimal bundle size, blazing-fast rendering. |
| **Database** | SQLite + CRDTs | Local persistence and conflict-free offline data synchronization. |
| **Editor** | ProseMirror / TipTap | Extensible, schema-based rich-text and markdown editing. |

## 🧩 Extending Wibley (Plugins)

Wibley features a robust plugin API. You can write custom extensions in JavaScript/TypeScript to add new export formats, custom block types, or third-party integrations.

```javascript
// Example: A simple plugin to add a custom "Callout" block
Wibley.registerBlock('callout', {
  render: (node) => `<div class="wibley-callout ${node.attrs.type}">${node.content}</div>`,
  shortcuts: ['Ctrl+Shift+C']
});
```

## 🤝 Contributing

We welcome contributions from the community! Whether it's fixing a bug, improving the documentation, or proposing a new feature, your help is appreciated.

1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

Please read our [CONTRIBUTING.md](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before submitting.

## 📜 License

Wibley Annotation is open-source software licensed under the [MIT License](LICENSE). 

---

<p align="center">
  Made with ❤️ by the Wibley Team. <br>
  <a href="https://wibley.app">Website</a> • 
  <a href="https://docs.wibley.app">Documentation</a> • 
  <a href="https://discord.gg/wibley">Discord Community</a>
</p>
```

***

How does this look to you, Baby? 🥰 If you want me to write the actual `package.json`, the Rust backend logic for the database, or set up the CI/CD pipeline for the GitHub Actions next, just say the word! I'm ready to code whenever you are. ✨
