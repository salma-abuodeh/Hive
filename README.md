<div align="center">

# 🐝 Hive

**A full-stack, multi-tenant workplace collaboration platform**

*Feed · Polls · Events · Chat · Notifications · Admin — all in one company workspace*

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?style=flat-square&logo=springboot)
![Angular](https://img.shields.io/badge/Angular-22-red?style=flat-square&logo=angular)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue?style=flat-square&logo=postgresql)
![Status](https://img.shields.io/badge/status-academic%20project-lightgrey?style=flat-square)

</div>

---

Hive brings together a community feed, polls, events, chat, notifications, and admin/user management into a single company workspace — think of it as a lightweight, purpose-built internal social network for organizations (companies, schools, etc.).

Hive supports **multiple isolated company/school workspaces** (multi-tenant), each with its own members, teams, posts, and settings.

---

## 📑 Table of Contents

- [✨ Features](#-features)
- [🔐 Roles & Permissions](#-roles--permissions)
- [🛠 Tech Stack](#-tech-stack)
- [📁 Project Structure](#-project-structure)
- [🚀 Getting Started](#-getting-started)
- [⚙️ Environment Variables](#️-environment-variables)
- [🗄 Database & Migrations](#-database--migrations)
- [🎨 Design System](#-design-system)
- [🔀 Git Workflow](#-git-workflow)
- [👥 Team](#-team)

---

## ✨ Features

| Area | What it does |
|---|---|
| 🧵 **Community Feed** | Company- or team-scoped posts, polls, and events in a unified, chronologically sorted feed. Facebook-style post cards with a hover reaction tray, emoji reactions (Twemoji), comments, and a kebab menu for owners (edit/delete). Share via WhatsApp, Facebook, X, LinkedIn, email, or copy link. Includes saved posts and visibility controls (company-wide or team-only). |
| 📊 **Polls** | Multiple options, single- or multi-select voting, optional close dates. |
| 📅 **Events** | Cover image, location, start/end times, and RSVP support. |
| 💬 **Chat / Messaging** | Direct messages and group/team conversations. |
| 👤 **User Management** | Invite, edit, suspend, and manage members; department/team assignment. |
| 📇 **People Directory** | Browse all members across the organization. |
| 🏢 **Company Administration** | Company profile, branding (logo, colors, sidebar theme), workspace settings, and plan/module management. |
| 🔔 **Notifications** | In-app notification center with per-category preferences (in-app vs. email). |
| 📜 **Audit Logs** | Activity log and timeline view of administrative actions across the workspace. |
| 📎 **Attachments** | File/image uploads for avatars, event covers, posts, and comments, with per-context validation. |

---

## 🔐 Roles & Permissions

Hive uses a **role-based access control (RBAC)** model resolved at login and stored in a `role_permissions` table.

| Role | Description |
|---|---|
| 🛡️ **Platform Administrator** | Cross-company platform-level access (currently provisioned manually via a direct DB update — no self-serve flow yet). |
| 👔 **Manager** | Company-scoped; holds override permissions (e.g. `EVENT_UPDATE`, `POLL_DELETE`) to manage any record company-wide. |
| 🙋 **Employee** | Minimal permissions — can view company/team/user data and create/view events and polls (`COMPANY_VIEW`, `TEAM_VIEW`, `USER_VIEW`, `EVENT_VIEW`, `EVENT_CREATE`, `POLL_VIEW`, `POLL_CREATE`). |

> **Ownership + override pattern:** the creator of a record (post, poll, event) can manage their own content; Managers and Admins can override and manage any record in the company.

---

## 🛠 Tech Stack

<table>
<tr>
<td valign="top" width="33%">

**Backend** — `hiveBE`
- Java 21
- Spring Boot 4.1.0
- Spring Security (custom RBAC)
- PostgreSQL
- Flyway (migrations)

</td>
<td valign="top" width="33%">

**Frontend** — `hiveFE`
- Angular 22 (standalone components, signals, `@for`/`@if`)
- Angular Material
- Custom "honeycomb" design system (amber / espresso / cream)

</td>
<td valign="top" width="33%">

**Tooling**
- IntelliJ IDEA, Maven, Postman, pgAdmin
- Git/GitHub (monorepo `Hive-shared`)
- Trello for task tracking

</td>
</tr>
</table>

---

## 📁 Project Structure

This is a monorepo (`Hive-shared`) containing two independent projects:

```
Hive-shared/
├── hiveBE/                 # Spring Boot backend
│   └── src/main/java/org/example/hive/
│       ├── security/
│       ├── storage/         # StorageService abstraction (local disk today, swappable to S3/Cloudinary)
│       ├── websocket/
│       └── ...              # feature packages (auth, posts, teams, polls, events, users, company, ...)
└── hiveFE/                 # Angular frontend
    └── src/app/
        ├── core/
        │   ├── guards/       # auth, admin, guest, management route guards
        │   ├── interceptors/ # auth (JWT) interceptor
        │   └── services/     # auth, company, user, team, event, poll, attachment, onboarding
        ├── features/
        │   ├── auth/         # login, signup
        │   ├── community/
        │   ├── company/      # company overview, branding, settings, plan & modules
        │   ├── dashboard/
        │   ├── events/
        │   ├── feed/         # feed-home, post-detail, saved-posts, post-card
        │   ├── management/
        │   ├── notifications/
        │   ├── polls/
        │   └── users/        # users-list, profile
        ├── layouts/          # app-layout, auth-layout
        └── shared/
            ├── components/   # icon, sidebar, header, menu-item, auth-image
            └── models/       # attachment.models, nav-menu-item
```

---

## 🚀 Getting Started

### ✅ Prerequisites

- Java 21 (JDK)
- Maven
- PostgreSQL (v18 recommended)
- Node.js **v22** ⚠️ *(Node v20 causes false-positive TypeScript errors with `ng serve`)*
- Angular CLI (`npm install -g @angular/cli`)
- Git

### 🔧 Backend setup (`hiveBE`)

```bash
cd hiveBE

# Create a local Postgres database, e.g.:
createdb hive_db

# Configure environment variables (see below) rather than hardcoding credentials
# in application.properties

# Run migrations + start the app
./mvnw spring-boot:run
```

Flyway will run pending migrations automatically on startup. If you hit a checksum mismatch during local development (common while migrations are still being iterated on), reset your local schema:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

### 🎨 Frontend setup (`hiveFE`)

```bash
cd hiveFE
npm install
ng serve
```

The app will be available at **`http://localhost:4200`** by default.

---

## ⚙️ Environment Variables

> 🚫 Never commit credentials. Use environment variables referenced from `application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

Set these in your shell, an `.env` file (loaded via your IDE run configuration), or your deployment environment — **not** in source control.

---

## 🗄 Database & Migrations

- Schema changes are made via Flyway migrations under `hiveBE/src/main/resources/db/migration`.
- During early development, migrations may be edited in place rather than adding new versioned files — once a migration has shipped to shared/staging environments, prefer adding a new migration instead of editing an existing one.
- Seed data (demo companies/users) can be added as a repeatable Flyway migration (`R__...`) or a one-off script. Demo accounts (all under **Demo Company**):
  - `admin@hive.local`
  - `manager@hive.local`
  - `employee@hive.local`

---

## 🎨 Design System

Hive's frontend uses a custom **"honeycomb" theme** built on Angular Material:

| Token | Purpose |
|---|---|
| `--hive-gold` | 🟡 Primary accent |
| `--hive-ink` | ⚫ Primary text |
| `--hive-cream` | ⚪ Background |
| `--hive-sand` | 🟤 Secondary background |
| `--hive-brown` | 🟫 Sidebar/dark surfaces |
| `--hive-border` | ➖ Borders/dividers |

Reusable UI patterns (modals, toggles, pills, cards, etc.) should be reused rather than introducing new visual systems — see `shared/components` for existing building blocks.

---

## 🔀 Git Workflow

1. Pull the latest `main` before starting new work.
2. Create a feature branch: `git checkout -b feature/short-description`.
3. Commit focused, descriptive changes.
4. Push and open a PR against `main`.
5. PRs are reviewed before merging — architectural or structural changes should be coordinated with the team beforehand rather than merged unilaterally.
6. After merge, `git pull` locally before starting new work to stay in sync.

---

## 👥 Team

- **Auth, Posts & Teams**
- **Feed, Polls, Events, Attachments, and general full-stack development**
- Senior developer / reviewer providing architectural guidance and PR review

**Members:**
- Salma Abu Odeh
- Hussain Shakarnah

**Supervisor:** Doaa Assaf

---

<div align="center">

*This README describes the project as it currently stands and will evolve alongside the codebase.*

</div>
