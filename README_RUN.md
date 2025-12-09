How to run the game with JDK 25 (WAV-only audio)

- Ensure you have JDK 25 installed at `C:\Program Files\Java\jdk-25` (or update paths below).
 

Run from PowerShell (uses JDK 25 explicitly):

```powershell
# Run without MP3/JLayer
.\run.ps1

 
```

VS Code:
- The workspace file `.vscode/settings.json` sets `java.home` to JDK 25. If VS Code doesn't pick it up, set the `JAVA_HOME` environment variable or update the path in settings.
- Use the launch config named `MainClass (JDK 25)` from the Run view.

If you need to produce a build compatible with Java 22 (for other machines), recompile with `--release 22` using `javac`.
