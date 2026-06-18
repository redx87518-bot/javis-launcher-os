# Contributing to JAVIS Launcher OS

Thank you for your interest in contributing to JAVIS Launcher OS!

## How to Contribute

### Reporting Bugs
1. Check existing [Issues](https://github.com/redx87518-bot/javis-launcher-os/issues)
2. Create a new issue with:
   - Device model and Android version
   - Steps to reproduce
   - Expected vs actual behavior
   - Logs if available

### Suggesting Features
- Open a GitHub Issue with the `enhancement` label
- Describe the feature and its use case

### Pull Requests
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Follow the code style (Kotlin + Compose conventions)
4. Write clear commit messages
5. Open a PR against `develop` branch

## Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Jetpack Compose best practices
- Keep ViewModels thin — business logic in use cases/managers
- Add comments for complex AI/brain logic

## Architecture Guidelines

- New AI features → `brain/` or `agents/`
- New UI screens → `ui/<screen>/`
- New data → add to `data/model/Models.kt` + Room DAO
- New API endpoints → `data/network/AiApiService.kt`

## License

By contributing, you agree your contributions are licensed under MIT.
