# LocalPlebs

LocalPlebs is a simple app designed to connect with local plebs around the world using the [Nostr](https://nostr.com/) protocol. The app allows users to create public posts and connect with others via fully end-to-end encrypted chat.

**Status:** Alpha release — for proof of concept only. Do not use for production or sensitive communications.

---

## Project Status: Android → Kotlin Multiplatform (KMP) Migration (WIP)

This project was originally developed as an Android app, but is currently being migrated to [Kotlin Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html). The goal is to support multiple platforms (Android, JVM, and more) from a shared codebase. The migration is a work in progress (WIP), and the codebase and structure are actively evolving.

### Current Structure
- `app/` — Android-specific code
- `crypto/`, `data/`, `nostr/`, `presentation/` — Shared KMP modules (with `commonMain`, `androidMain`, `jvmMain`, etc.)
- `testrun/` — Test and utility code

---

## Features
- Discover and connect with local plebs globally
- Create and view public publications ([NIP-15](https://github.com/nostr-protocol/nips/blob/master/15.md) compliant)
- End-to-end encrypted private messaging ([NIP-17](https://github.com/nostr-protocol/nips/blob/master/17.md) compliant)

## Protocols Used
- [NIP-15: Nostr Publications](https://github.com/nostr-protocol/nips/blob/master/15.md)
- [NIP-17: Nostr Encrypted Chat](https://github.com/nostr-protocol/nips/blob/master/17.md)

## Disclaimer
This is an alpha release and should only be used for testing and proof of concept purposes. Features and security are experimental and may change.

---

Made with ❤️ for the Nostr community.
