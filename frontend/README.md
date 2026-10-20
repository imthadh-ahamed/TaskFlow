# TaskFlow — Frontend

Next.js frontend for the TaskFlow application.

## Tech Stack

- [Next.js](https://nextjs.org/) (App Router)
- [React](https://react.dev/)
- [TypeScript](https://www.typescriptlang.org/) (strict mode)
- [Tailwind CSS](https://tailwindcss.com/)
- [ESLint](https://eslint.org/)

## Prerequisites

- Node.js 18+
- npm 9+

## Getting Started

```bash
# Install dependencies
npm install

# Copy environment variables
cp .env.example .env.local

# Start development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Commands

| Command         | Description                    |
|-----------------|--------------------------------|
| `npm run dev`   | Start development server       |
| `npm run build` | Create production build        |
| `npm run start` | Start production server        |
| `npm run lint`  | Run ESLint                     |

## Project Structure

```
src/
├── app/           # Next.js App Router pages and layouts
├── components/
│   ├── ui/        # Reusable UI primitives
│   └── layout/    # Page layout components
├── features/      # Feature-scoped modules
├── hooks/         # Custom React hooks
├── lib/
│   ├── api/       # API client utilities
│   └── utils/     # General utility functions
├── types/         # Shared TypeScript types
└── constants/     # App-wide constants
tests/             # Test files
```

## Environment Variables

See `.env.example` for available variables.
