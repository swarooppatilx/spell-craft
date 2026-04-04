# SpellCraft Mod

SpellCraft mod is a minecraft mod that aims to provide an environment for autonomous/semi-autonomous agents that can understand natural language to perform tasks in the game. This can be used to run series of experiments and study how agents behave in open environments.
![collage](screenshots/collage.png)

## Features

- **Natural Language Parsing** - Use plain English for any Minecraft operation
- **Local AI Support** - Works offline with Ollama (default)
- **Cloud Fallback** - Use Gemini API when Ollama is unavailable
- **Universal Command Support** - Spawn, give, teleport, weather, time, etc.
- **Auto-detect Cheat Requirements** - Warns if command needs cheats/op
- **Retry Logic** - Automatic retry on network timeouts

## Setup

### Option 1: Ollama (Recommended - Works Offline)

1. Install Ollama from [ollama.ai](https://ollama.ai)
2. Pull the model:
   ```bash
   ollama pull deepseek-r1:1.5b #You can use any other model
   ```
3. Start Ollama (runs on `localhost:11434`)

### Option 2: Gemini API (Cloud)

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Set `gemini_api_key` in config

### Configure the Mod

The config file is created automatically on first run at:

```
.minecraft/config/modid.json
```

Default config (uses Ollama):

```json
{
	"gemini_api_key": "",
	"ollama": {
		"endpoint": "http://localhost:11434",
		"model": "deepseek-r1:1.5b"
	}
}
```

To use Gemini instead:

```json
{
	"gemini_api_key": "YOUR_API_KEY_HERE",
	"ollama": {
		"endpoint": "http://localhost:11434",
		"model": "deepseek-r1:1.5b"
	}
}
```

**Auto-detection:** If `gemini_api_key` is empty, uses Ollama. If set, uses Gemini.

### Add to Prism Launcher

1. Open Prism Launcher
2. Create/edit your 26.1.1 instance
3. Go to **Loader Mods** tab
4. Add `modid-1.0.0.jar`
5. Add `fabric-api-0.145.3+26.1.1.jar` (download from [fabricmc.net](https://fabricmc.net/use/server/))
6. Launch the game

## Usage

### Command

- `/ai <query>` - Execute a natural language command

### Examples

```bash
/ai spawn a dragon
/ai give me a diamond sword
/ai make it night
/ai change weather to rain
/ai teleport me to spawn
/ai set time to noon
/ai give me netherite armor
/ai kill all mobs
/ai create a mountain of diamond
```

## How It Works

1. You type: `/ai spawn 5 wolves`
2. AI translates it to: `execute at @p run summon wolf ~ ~ ~5`
3. The mod executes the command directly
4. Result is shown in chat

**Provider Selection:**

- If `gemini_api_key` is empty → uses Ollama (local)
- If `gemini_api_key` is set → uses Gemini (cloud)

## Requirements

- Minecraft 26.1.1
- Fabric Loader 0.18.6+
- Fabric API 0.145.3+
- Java 25 (to build from source)
- **Ollama** (optional, for local AI)

## Building from Source

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
./gradlew build
```

Jar output: `build/libs/modid-1.0.0.jar`

## Configuration

Config file location: `.minecraft/config/modid.json`

| Setting           | Description       | Default                |
| ----------------- | ----------------- | ---------------------- |
| `gemini_api_key`  | Google AI API key | (empty)                |
| `ollama.endpoint` | Ollama server URL | http://localhost:11434 |
| `ollama.model`    | Ollama model name | deepseek-r1:1.5b       |
