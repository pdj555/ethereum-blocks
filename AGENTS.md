# Repository Guidelines

## Project Structure & Module Organization

- `src/main/java/`: Java source files (default package). Core classes: `Blocks`, `Transaction`, `EthereumBlockExplorer` (interactive CLI), `Driver` (example run), plus helper utilities.
- `src/test/java/`: JUnit tests (`Test*.java`).
- `target/`: Maven build output (generated).
- `doc/`: generated Javadoc site (generated output).
- `imgs/`: images used by `README.md`.
- Data files are in the repo root (e.g., `ethereumP1data.csv`, `ethereumtransactions1.csv`) and are loaded via relative paths—run programs from the repository root.

## Build, Test, and Development Commands

- Run unit tests: `mvn test`
- Build a runnable (shaded) JAR: `mvn -DskipTests package`
- Run the CLI: `java -jar target/ethereum-blocks-*.jar`
- (Optional) Regenerate Javadoc: `javadoc -d doc src/main/java/*.java`

## Coding Style & Naming Conventions

- Java naming: classes `UpperCamelCase`, methods/fields `lowerCamelCase`, constants `UPPER_SNAKE_CASE`.
- Keep classes in the default package unless you migrate the whole project consistently.
- Preserve console output formatting—some methods print structured output and whitespace/capitalization matters.
- Indentation varies in existing files; match the file you’re editing and keep diffs minimal.

## Testing Guidelines

- Framework: JUnit 5 (Jupiter). Tests: `src/test/java/TestBlocks.java`, `src/test/java/TestTransaction.java`.
- Run tests via Maven: `mvn test`

## AI & Configuration

- The CLI includes an “Ask AI” option that calls OpenAI if `OPENAI_API_KEY` is set.
- Optional env vars: `OPENAI_MODEL` (default `gpt-4.1-mini`), `OPENAI_BASE_URL` (default `https://api.openai.com`).

## Commit & Pull Request Guidelines

- Commit subjects are short and imperative (examples in history: `Update README.md`, `Optimize …`).
- PRs should explain the “why” + “what”, include run/test notes, and add a screenshot or pasted snippet when CLI/output formatting changes.
- `target/` and `doc/` are generated; only commit changes when intentionally regenerating outputs.
