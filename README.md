# 🐉 AI Game Master UI

A fantasy-themed web client for the **Once Upon an Agentic AI** DungeonMaster RPG application. Built with Vue 3, TypeScript, Pinia, and Vite, this single-page app lets players create characters and interact with an AI-powered Dungeon Master through a parchment-scroll styled interface.

![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883)
![TypeScript](https://img.shields.io/badge/TypeScript-5.8-3178c6)
![Vite](https://img.shields.io/badge/Vite-6-646cff)

---

## Features

- **Character Creation** — pick a name, gender, race, and class from classic D&D options (scroll-unroll animation included).
- **Live Game Session** — chat with the AI Dungeon Master, receive narrative responses with Markdown rendering, dice rolls, and action suggestions.
- **Character Stats Sidebar** — view ability scores, inventory, level, and experience in real time.
- **Dice Roll Display** — visual dice (d4 – d100) rendered from SVG assets.
- **Parchment & Medieval Theme** — custom CSS with paper textures, Cinzel / Crimson Text / MedievalSharp fonts, and warm earth-tone palette.
- **SPA with GitHub Pages support** — includes a 404.html redirect trick for client-side routing on static hosts.

---

## Prerequisites

| Tool | Version |
|------|---------|
| [Node.js](https://nodejs.org/) | **22+** (project extends `@tsconfig/node22`) |
| npm | Ships with Node |

You also need a running instance of the **DungeonMaster backend API** that exposes the following endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/inquire` | Send a player action / question and receive a story response |
| `GET`  | `/user/:name` | Fetch character stats |
| `GET`  | `/messages` | Retrieve conversation history |

---

## Getting Started

### 1. Clone the repository

```bash
git clone <repo-url>
cd <repo-folder>
```

### 2. Install dependencies

```bash
npm install
```

### 3. Start the dev server

```bash
npm run dev
```

Vite will start on [http://localhost:5173](http://localhost:5173) by default.

### 4. Open the app

Navigate to the URL shown in your terminal. You'll land on the **Create Your Character** screen. Fill in your character details and provide the URL of your running DungeonMaster backend, then hit **Start** to begin your adventure.

---

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start the Vite development server with HMR |
| `npm run build` | Type-check with `vue-tsc` and build for production |
| `npm run preview` | Preview the production build locally |
| `npm run test` | Run the test suite once with Vitest |
| `npm run test:watch` | Run tests in watch mode |

---

## Project Structure

```
src/
├── assets/            # Global CSS theme
├── components/        # Reusable Vue components
│   ├── ActionSuggestions.vue
│   ├── CharacterStatsPanel.vue
│   ├── DiceDisplay.vue
│   ├── MessageList.vue
│   └── PlayerInput.vue
├── composables/       # Vue composables (useGameApi)
├── router/            # Vue Router configuration
├── stores/            # Pinia stores (gameStore)
├── utils/             # Helpers (messageParser, promptFormatter, validation)
├── views/             # Route-level views
│   ├── NewGameView.vue   # Character creation screen
│   └── GameView.vue      # Main gameplay screen
├── types.ts           # TypeScript interfaces
├── App.vue            # Root component
└── main.ts            # App entry point
public/
├── dice/              # SVG dice assets (d4–d100)
└── textures/          # Paper texture background
```

---

## Building for Production

```bash
npm run build
```

Output is written to `dist/`. The build is configured with a base path of `/sample-once-upon-agentic-ai/` in `vite.config.ts` — update this if you're deploying under a different path.

---

## License

See [LICENSE](./LICENSE) for details.
