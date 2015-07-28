[![Build Status](https://travis-ci.org/tschulte/gradle-semantic-release-plugin.svg?branch=master)](https://travis-ci.org/tschulte/gradle-semantic-release-plugin)
[![Coverage Status](https://coveralls.io/repos/tschulte/gradle-semantic-release-plugin/badge.png?branch=master)](https://coveralls.io/r/tschulte/gradle-semantic-release-plugin?branch=master)

# Gradle implementation of [semantic release](http://github.com/semantic-release/semantic-release)

This plugin is work in progress, the first version 1.0.0 will be released soon, when the following works:

- Infer the version based on the commit messages
- Create the changelog
- Upload the changelog to github

## What is `semantic-release` about?

At its core `semantic-release` is a set of conventions that gives you **entirely automated, semver-compliant package publishing**. _Coincidentally_ these conventions make sense on their own – like meaningful commit messages.

This removes the immediate connection between human emotions and version numbers, so strictly following the [SemVer](http://semver.org/) spec is not a problem anymore – and that’s ultimately `semantic-release`’s goal.

> ### “We fail to follow SemVer – and why it needn’t matter”
> #### JSConf Budapest 2015

> [![JSConfBP Talk](https://cloud.githubusercontent.com/assets/908178/8032541/e9bf6300-0dd6-11e5-92c9-8a39211368af.png)](https://www.youtube.com/watch?v=tc2UgG5L7WM&index=6&list=PLFZ5NyC0xHDaaTy6tY9p0C0jd_rRRl5Zm)

This talk gives you a complete introduction to the underlying concepts of this module
-- <cite>[semantic release](http://github.com/semantic-release/semantic-release)</cite>

## How does it work?

Instead of writing [meaningless commit messages](http://whatthecommit.com/), we can take our time to think about the changes in the codebase and write them down. Following formalized conventions it this then possible to generate a helpful changelog and to derive the next semantic version number from them.

When `semantic-release` got setup it will do that after every successful continuous integration build of your master branch (or any other branch you specify) and publish the new version for you. That way no human is directly involved in the release process and your releases are guaranteed to be [unromantic and unsentimental](http://sentimentalversioning.org/).

This module ships with the [AngularJS Commit Message Conventions](https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit) and changelog generator, but you can [define your own](#configuration) style.

Under the hood this plugin uses [gradle-git](https://github.com/ajoberstar/gradle-git)'s release-base plugin and configures it to automatically increment the major, minor or patch version depending on the commit messages since the last release. Releases are only allowed on certain branches (master, release/\d+\.x, release/\d+\.\d+\.x by default). On other branches only devSnapshot releases are allowed. On these branches the branch name is automatically appended to the version.

The plugin does -- inspired by the [nebula-release](https://github.com/nebula-plugins/nebula-release-plugin/) plugin -- create new tasks based on the gradle-git version strategies e.g. `final`, `alpha`, `devSnapshot` and `snapshot` to build a final, alpha, devSnapshot or SNAPSHOT version. But these tasks are just convenience tasks and do the same as e.g. `./gradlew release -Prelease.stage=final`

When doing a final release -- in addition to the default behavior of gradle-git to tag the release and push the tag -- if the origin repository is a github repository, the plugin generates a changelog and creates a [release](https://help.github.com/articles/about-releases/) on GitHub.

## Setup

apply the plugin in the rootProject (and only there) of your gradle build.

### Gradle >= 2.1
```groovy
plugins {
    id 'de.gliderpilot.semantic-release' version '1.0.0'
}
```

### Gradle < 2.1
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'de.gliderpilot.gradle.semantic-release:gradle-semantic-release-plugin:1.0.0'
    }
}
apply plugin: 'de.gliderpilot.semantic-release'
```

## Configuration

Since this plugin uses gradle-git under the cover, you can configure this plugin as [described in their wiki](https://github.com/ajoberstar/gradle-git/wiki/Release%20Plugins%201.x).

There are two plugins, `de.gliderpilot.semantic-release` and `de.gliderpilot.semantic-release-base`. The first one does already define versionStrategies for gradle-git. The base-plugin does not. When using the base plugin, it is up to you to configure the version strategies of gradle git.

The `gradle-semantic-release-plugin` defines it's own extension `semanticRelease`. When configuring gradle-git, you **must not** use the VersionStrategies defined by gradle-git directly, but you **must use** the method toSemanticReleaseStrategy of the semanticRelease extension to convert the strategies to comply with the semantic-release rules. Otherwise the version will not be inferred based on the commit messages.

```groovy
release {
    versionStrategy semanticRelease.toSemanticReleaseStrategy(Strategies.ALPHA)
    versionStrategy semanticRelease.toSemanticReleaseStrategy(Strategies.FINAL)
    defaultVersionStrategy semanticRelease.toSemanticReleaseStrategy(Strategies.SNAPSHOT)
}
```

## ITYM*FAQ*LT
> I think you might frequently ask questions like these

### Is there a way to preview which version would currently get published?

If you run `./gradlew -Prelease.stage=final` locally, the version that would be build gets logged.

### Can I run this on my own machine rather than on a CI server?

Of course you can, but this doesn't necessarily mean you should. Running your tests on an independent machine before releasing software is a crucial part of this workflow. Also it is a pain to set this up locally, with tokens lying around and everything.

### Can I manually trigger the release of a specific version?

You can trigger a release by pushing to your GitHub repository. You deliberately cannot trigger a _specific_ version release, because this is the whole point of `semantic-release`. Start your packages with `1.0.0` and semver on. You can however prevent an accidental major version bump by using a branch pattern `release/\d+\.x`. And you can prevent an accidental minor version bump by using a branch pattern `release/\d+\.\d+\.x`.

### Is it _really_ a good idea to release on every push?

It is indeed a great idea because it _forces_ you to follow best practices. If you don't feel comfortable making every passing feature or fix on your master branch addressable you might not treat your master right. Have a look at [branch workflows](https://guides.github.com/introduction/flow/index.html). If you still think you should have control over the exact point in time of your release, e.g. because you are following a release schedule, you can release only on the release` branch and push your code there in certain intervals.

### Why should I trust `semantic-release` with my releases?

`gradle-semantic-release-plugin` has a full unit- and integration-test-suite. Additionally we eat our own dogfood and release using our own plugin -- A new version won't get published if there is an error.

Note: Currently integration-tests don't run on Travis CI.

## License

Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/
2015 © Tobias Schulte, based on the ideas of the semantic-release plugin of Stephan Bönnemann and [contributors](https://github.com/semantic-release/semantic-release/graphs/contributors)
