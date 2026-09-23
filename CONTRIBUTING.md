# Contributing

Issues and pull requests are welcome.

Before sending a change:
1. keep the project compatible with rootless Android unless the feature is explicitly marked as root-only;
2. do not remove upstream license or attribution notices;
3. keep network behavior transparent — do not add telemetry, hidden remote endpoints or bundled credentials;
4. run `./gradlew assembleDebug`;
5. describe which behavior changed and how it was checked.

For new strategies, explain the affected protocol (TCP/UDP), the intended symptom, and the exact ByeDPI arguments.
