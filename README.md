# D&D Game Master Frontend

A React + Vite frontend for the Once Upon Agentic AI workshop. Connects to the Game Master orchestrator API to provide an immersive D&D experience.

## Prerequisites

- Node.js 18+
- The Game Master backend running (see Chapter 5 of the workshop)

## Setup

```bash
cd sample-once-upon-agentic-ai-typescript-frontend
npm install
```

## Development

```bash
npm run dev
```

Opens at http://localhost:5173

## Usage

1. Fill in your character details (name, gender, race, class)
2. Enter the Game Master server URL (default: `http://localhost:8009`)
3. Click "Start" to create your character and begin the adventure
4. Chat with the Game Master, click action suggestions, and roll dice

## Build

```bash
npm run build
```

Output goes to `dist/`.
