# Contributing to Spell-Craft

We want to make it easy for you to contribute to Spell-Craft. Here are the most common types of changes that get merged:

- Bug fixes
- Additional AI providers
- Improvements to prompt templates and action parsing
- New Minecraft command/actions support
- Documentation improvements
- Performance optimizations

If you are unsure if a PR would be accepted, feel free to open an issue first or reach out to a maintainer.

## Project Structure

```
spell-craft/
├── build.gradle                    # Gradle build configuration
├── settings.gradle
├── gradle.properties
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   ├── ExampleMod.java          # Main mod entry point
    │   │   └── ai/                       # AI subsystem
    │   │       ├── ApiClient.java        # Interface for AI providers
    │   │       ├── GeminiApiClient.java  # Google Gemini implementation
    │   │       ├── OllamaApiClient.java # Ollama implementation
    │   │       ├── Config.java          # Configuration management
    │   │       ├── WorldState.java      # Minecraft state for AI context
    │   │       ├── ActionHandler.java   # Executes AI response actions
    │   │       ├── CommandExecutor.java  # Minecraft command execution
    │   │       ├── GoalManager.java     # Goal tracking & auto-trigger
    │   │       ├── LocationMemory.java  # Location saving/recall
    │   │       └── ReflexHandler.java   # Automatic reflex actions
    │   └── resources/
    │       └── fabric.mod.json           # Mod metadata
    └── client/
        └── java/com/example/
            └── ExampleModClient.java      # Client-side mod
```

## Developing Spell-Craft

### Requirements

- Java 25
- Gradle (wrapper included)
- Minecraft 1.26.1 with Fabric mod loader

### Building

From the project root:

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`.

### Running in Development

Use Fabric Loom to run with the development Minecraft instance:

```bash
./gradlew runClient   # Run client
./gradlew runServer   # Run dedicated server
```

### Configuration

The mod reads configuration from `config/modid.json`:

```json
{
  "gemini_api_key": "YOUR_API_KEY_HERE",
  "ollama_endpoint": "http://localhost:11434",
  "ollama_model": "llama3"
}
```

- If `gemini_api_key` is set and valid, Gemini is used
- Otherwise, if `ollama_endpoint` and `ollama_model` are set, Ollama is used

## Adding New AI Providers

To add a new AI provider:

1. **Create a new class** implementing `ApiClient`:

```java
public class MyApiClient implements ApiClient {
    @Override
    public String translateToCommand(String userInput, WorldState worldState) throws Exception {
        // Build prompt using WorldState.getPromptTemplate() + userInput
        // Make API call and return the response text
        // The response should be JSON matching the expected format
    }
}
```

2. **Update `ExampleMod.java`** to use your provider:

Add a condition in `createApiClient()`:
```java
private static ApiClient createApiClient() {
    Config config = Config.getInstance();
    if (config.useOllama()) {
        return new OllamaApiClient(config.getOllamaEndpoint(), config.getOllamaModel());
    }
    if (config.useMyProvider()) {  // Add this
        return new MyApiClient(config.getMyApiKey());
    }
    return new GeminiApiClient(config.getGeminiApiKey());
}
```

3. **Update `Config.java`** to add configuration fields and getter methods for your provider.

### Expected Response Format

All providers must return text that can be parsed as JSON with this structure:

```json
{
  "thought": "brief explanation of what action to take",
  "actions": [
    {
      "action": "action_type",
      "params": { ... }
    }
  ]
}
```

Supported action types are defined in `WorldState.java` prompt template.

## Supported Action Types

Actions are processed by `ActionHandler.java`. To add a new action type:

1. Add the action name to the prompt template in `WorldState.java`
2. Add the case in `ActionHandler.executeActions()`
3. Implement the actual logic (typically in `CommandExecutor.java`)

## Pull Request Expectations

### Issue First Policy

**All PRs should reference an existing issue.** Before opening a PR, consider opening an issue describing the bug or feature. This helps maintainers triage and prevents duplicate work.

### General Requirements

- Keep pull requests small and focused
- Explain the issue and why your change fixes it
- Ensure the code compiles with `./gradlew build`
- Test your changes in-game

### PR Titles

PR titles should follow conventional commit standards:

- `feat:` new feature or functionality
- `fix:` bug fix
- `docs:` documentation changes
- `chore:` maintenance tasks, dependency updates
- `refactor:` code refactoring without changing behavior

Examples:
- `feat: add Anthropic Claude provider support`
- `fix: handle empty inventory gracefully`
- `docs: update contributing guidelines`

## Issue Guidelines

When opening issues, please include:

- **Bug reports**: Description of the issue, steps to reproduce, expected vs actual behavior
- **Feature requests**: Description of the feature and why it would be useful
- **Questions**: Clearly state your question

## Code Style

- Follow existing conventions in the codebase
- Use meaningful variable names
- Keep methods focused and reasonably sized
- Add appropriate error handling
