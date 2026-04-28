# vue3-ts-blog

A frontend-only blog web application built with Vue 3, TypeScript, and Vite.

## Tech Stack

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- Cypress

## Requirements

- Node.js 18+
- npm 9+

## Quick Start

```bash
# install dependencies
npm install

# start development server
npm run dev
```

The app runs on the default Vite port shown in your terminal after startup.

## Available Scripts

```bash
# development
npm run dev

# type-check + production build
npm run build

# preview production build
npm run preview

# lint and auto-fix
npm run lint

# component tests (Cypress)
npm run test:unit
npm run test:unit:dev

# e2e tests (Cypress)
npm run test:e2e
npm run test:e2e:dev
```

## Project Architecture

```text
src/
├─ assets/       # Static assets (images, icons, fonts)
├─ components/   # Reusable UI components
├─ language/     # i18n resources and language setup
├─ mixins/       # Shared component logic
├─ mock/         # Mock data and mock handlers
├─ model/        # Frontend data models / typings
├─ pages/        # Page-level views
├─ router/       # Route definitions and navigation guards
├─ stores/       # Pinia stores
├─ style/        # Global styles and theme files
├─ utils/        # General utility functions
├─ App.vue       # Root component
└─ main.ts       # App bootstrap entry
```

## Environment Variables

Use environment files to manage runtime configuration:

- `.env.development`
- `.env.production`
- `.env.example`

Copy `.env.example` and adjust values based on your local environment.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/)
- [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)
- Disable Vetur if installed

## Notes

- Keep business logic in `stores/` and `utils/`, not in page templates.
- Keep shared UI in `components/` to avoid duplication.
