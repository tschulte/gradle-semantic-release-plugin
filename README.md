# gradle-semantic-release-plugin

> ## ⚠️ This project is discontinued
>
> **Do not use this plugin for new projects. It does not work with Gradle 9.**
>
> Use [semantic-release](https://semantic-release.org/) itself, driving Gradle through
> [`@semantic-release/exec`](https://github.com/semantic-release/exec). This README is
> now a migration guide. See [Why](#why-this-project-is-discontinued) for the reasoning
> and [Migration reference](#migration-reference) for a mapping of every configuration
> option this plugin offered.

## Why this project is discontinued

This plugin was written in 2015, when there was no practical way to run the JavaScript
`semantic-release` alongside a JVM build. That is no longer true, and the foundation this
plugin was built on has gone away:

- The entire version inference — roughly half the codebase — sits on top of
  [`org.ajoberstar:gradle-git`](https://github.com/ajoberstar/gradle-git) 1.7.2, which has
  been dead since around 2017. Its successor, [reckon](https://github.com/ajoberstar/reckon),
  uses a fundamentally different model, so there is no migration path — only a rewrite.
- The `grgit` Gradle plugin is explicitly not compatible with Gradle's configuration cache,
  and its author has stated that this is out of scope.
- `com.sun.jersey`, Bintray, Artifactory, Coveralls and Travis CI — all dependencies of this
  build — are dead or discontinued.
- `ConfigureUtil`, used by this plugin's extension, was removed in Gradle 9.

Rewriting all of that would produce something that `semantic-release` plus a four-line
`exec` configuration already does, usually better. So this plugin is being retired instead.

**One thing genuinely gets harder:** the replacement requires Node.js in your CI. On GitHub
Actions and most hosted runners it is preinstalled, so this is rarely a practical concern —
but if your organisation does not allow npm dependencies in the build pipeline at all, this
migration is not for you, and there is currently no maintained JVM-native equivalent.

## Quick start

The setup below reproduces what this plugin did: infer the version from commit messages,
tag the release, generate release notes, create a GitHub release with attached artifacts,
and publish through Gradle.

### 1. Pin a placeholder version in `gradle.properties`

```properties
version=0.0.0-SNAPSHOT
```

Remove any `version = ...` assignment from `build.gradle` / `build.gradle.kts`. This
placeholder is never updated in the repository — the real version is passed in at release
time. This mirrors what `semantic-release` does in its own repository, where `package.json`
permanently reads `"version": "0.0.0-development"`.

### 2. Add `.releaserc.json`

```json
{
  "branches": [
    "master",
    "+([0-9])?(.{+([0-9]),x}).x"
  ],
  "plugins": [
    "@semantic-release/commit-analyzer",
    "@semantic-release/release-notes-generator",
    [
      "@semantic-release/exec",
      {
        "prepareCmd": "./gradlew -Pversion=${nextRelease.version} build",
        "publishCmd": "./gradlew -Pversion=${nextRelease.version} publish"
      }
    ],
    [
      "@semantic-release/github",
      {
        "assets": [
          { "path": "build/libs/*.jar" }
        ]
      }
    ]
  ]
}
```

Replace `publish` with whatever your build actually uses — `publishPlugins`,
`publishToSonatype closeAndReleaseSonatypeStagingRepository`, `artifactoryPublish`, and so on.

### 3. Add a GitHub Actions workflow

```yaml
name: Release
on:
  push:
    branches: [master]

permissions:
  contents: write
  issues: write
  pull-requests: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0          # semantic-release needs the full history and all tags
          persist-credentials: false

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v4

      - uses: actions/setup-node@v4
        with:
          node-version: 24        # semantic-release 25 requires ^22.14.0 || >=24.10.0

      - run: npx semantic-release
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Add whatever publishing credentials your Gradle build needs as further `env` entries — for
the Gradle Plugin Portal, for instance, `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`,
wired through in the `publishCmd`.

That is the whole setup. Note the direction: Node starts, Node calls Gradle. Your Gradle
build knows nothing about `semantic-release`, and local development is unaffected —
developers keep running `./gradlew` directly.

## Migration reference

| `gradle-semantic-release-plugin` | Replacement |
| --- | --- |
| `semanticRelease.changeLog.changeScope` | `@semantic-release/commit-analyzer` → `releaseRules` |
| `semanticRelease.changeLog.changeLog` | `@semantic-release/release-notes-generator` → `preset` / `writerOpts` |
| `semanticRelease.releaseBranches` | top-level `branches` |
| `semanticRelease.branchNames` | no equivalent needed — see [SNAPSHOT versions](#snapshot-versions-on-feature-branches) |
| `semanticRelease.repo.ghToken` | `GITHUB_TOKEN` / `GH_TOKEN` environment variable |
| `semanticRelease.repo.releaseAsset` | `@semantic-release/github` → `assets` |
| `semanticRelease.repo.useGhEnterprise` | `@semantic-release/github` → `githubUrl` |
| `release.versionStrategy` (rc branches etc.) | `branches` entries with `prerelease: true` |
| `release` task, `dependsOn build`, `finalizedBy publish` | `prepareCmd` / `publishCmd` |

### Release branches

The old default was `master` plus `/(?:release[-\/])?\d+(?:\.\d+)?\.x/`. `semantic-release`
ships an equivalent maintenance-branch pattern out of the box:

```json
{
  "branches": [
    "master",
    "stable",
    "+([0-9])?(.{+([0-9]),x}).x"
  ]
}
```

`+([0-9])?(.{+([0-9]),x}).x` matches `1.x`, `1.0.x`, `2.x` and so on — the same guarantee
against accidental major or minor bumps that the old branch patterns gave you.

### Custom change scopes

The old `changeScope` closure returned `MAJOR`, `MINOR`, `PATCH` or `null` per commit. The
declarative equivalent:

```json
[
  "@semantic-release/commit-analyzer",
  {
    "preset": "conventionalcommits",
    "releaseRules": [
      { "type": "docs", "scope": "README", "release": "patch" },
      { "type": "refactor", "release": "patch" },
      { "scope": "no-release", "release": false }
    ]
  }
]
```

Defaults, if you specify nothing: breaking changes → `major`, `feat` → `minor`,
`fix` and `perf` → `patch`, everything else → no release. That is exactly what this plugin
did by default.

### Custom changelog

The old `changeLog` closure built the release notes string by hand. Use
`@semantic-release/release-notes-generator` with a preset, or `writerOpts` for full control:

```json
[
  "@semantic-release/release-notes-generator",
  {
    "preset": "conventionalcommits",
    "presetConfig": {
      "types": [
        { "type": "feat", "section": "Features" },
        { "type": "fix", "section": "Bug Fixes" },
        { "type": "perf", "section": "Performance" },
        { "type": "chore", "hidden": true }
      ]
    }
  }
]
```

If you also want a committed `CHANGELOG.md`, add
[`@semantic-release/changelog`](https://github.com/semantic-release/changelog) together with
[`@semantic-release/git`](https://github.com/semantic-release/git).

### Release assets

```json
[
  "@semantic-release/github",
  {
    "assets": [
      { "path": "build/libs/*.jar" },
      {
        "path": "build/libs/*-sources.jar",
        "name": "the-sources.jar",
        "label": "The sources jar"
      }
    ]
  }
]
```

`path` is a glob rather than a task reference, so make sure `prepareCmd` builds the
artifacts before this step runs. `name` and `label` support templating with
`${nextRelease.version}` and friends.

### GitHub Enterprise

```json
[
  "@semantic-release/github",
  {
    "githubUrl": "https://github.enterprise",
    "githubApiPathPrefix": "/api/v3"
  }
]
```

Or via the `GH_URL` and `GH_PREFIX` environment variables.

### Release candidates and other pre-releases

This used to require composing `VersionStrategy` objects with `copyWith`, `StrategyUtil.all`
and `Strategies.PreRelease.COUNT_INCREMENTED`. It is now one line:

```json
{
  "branches": [
    "master",
    { "name": "rc", "prerelease": true }
  ]
}
```

Commits on `rc` produce `1.2.0-rc.1`, `1.2.0-rc.2`, and so on.

### SNAPSHOT versions on feature branches

This has no direct equivalent, by design. The old plugin inferred a
`1.2.3-branchname-SNAPSHOT` version for every build on a non-release branch; the
`branchNames` extension existed only to tidy up those branch names.

`semantic-release` does not model unreleased builds at all — the version is treated as a
property of the published artifact, not of the source tree. Local and feature-branch builds
simply keep the `0.0.0-SNAPSHOT` placeholder.

In practice the main thing this affects is `publishToMavenLocal`, and a fixed coordinate is
arguably better there: a downstream project declares `0.0.0-SNAPSHOT` once and always
resolves your latest local build, instead of chasing a version that changes every commit.

If you genuinely need a descriptive version for local builds, derive it on the Gradle side
with a `ValueSource` wrapping `git describe --tags --abbrev=0`. That stays
configuration-cache safe, works in the IDE, and does not require Node. Do not wrap local
builds in an npm script for this — IDEs invoke Gradle directly and would silently bypass the
wrapper, giving you different versions on the command line and in the IDE.

## FAQ

### Can I preview which version would be published?

`npx semantic-release --dry-run`. This is better than what the old plugin offered, which
was reading the version out of the build log.

### Can I run a release from my own machine?

You can, but you probably should not, for the same reasons as before: releasing from an
independent machine that has run the tests is the point of the workflow, and it means no
long-lived tokens on developer laptops.

### Does the version get committed back to the repository?

Not with the setup above, and that is deliberate. The git tag remains the single source of
truth, and no chore commit is created per release. `semantic-release` itself recommends
against committing the version back, since it adds meaningful complexity to the release
process. If you want it anyway, add `@semantic-release/git` and have it commit
`gradle.properties`.

### What about the npm package `gradle-semantic-release-plugin`?

Unrelated to this project despite the identical name — it is a `semantic-release` plugin by
Kengo TODA that also drives Gradle. It works, and it auto-detects your publish task by
parsing the output of `gradle tasks`. That detection is its only advantage over
`@semantic-release/exec`, and it costs two extra full Gradle configuration phases per
release. If you already know your publish task, prefer `exec`.

Note that the scoped `@gradle/semantic-release-plugin` on npm is an obsolete alias by the
same author, last released in 2019. Do not use it.

### Should I use reckon instead?

Only if you want Gradle-native version inference without Node. Be aware that
[reckon](https://github.com/ajoberstar/reckon) is in maintenance mode — its GitHub
repository was archived in September 2025 — and that its built-in commit-message scope
calculator understands `major:` / `minor:` / `patch:` prefixes rather than Conventional
Commits, so you would need to supply your own `ScopeCalculator`.

## License

Apache License, Version 2.0
<http://www.apache.org/licenses/>

2015 © Tobias Schulte, based on the ideas of the semantic-release plugin of Stephan
Bönnemann and [contributors](https://github.com/semantic-release/semantic-release/graphs/contributors)
