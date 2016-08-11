[![Build Status](https://travis-ci.org/tschulte/gradle-semantic-release-plugin.svg?branch=master)](https://travis-ci.org/tschulte/gradle-semantic-release-plugin)
[![Coverage Status](https://coveralls.io/repos/tschulte/gradle-semantic-release-plugin/badge.png?branch=master)](https://coveralls.io/r/tschulte/gradle-semantic-release-plugin?branch=master)

# Gradle implementation of [semantic release](http://github.com/semantic-release/semantic-release)

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

This module ships with the [AngularJS Commit Message Conventions](https://github.com/angular/angular.js/blob/master/CONTRIBUTING.md#commit) and changelog generator, but you can [define your own](#configuration) style.

> ### Commit Message Format

> Each commit message consists of a **header**, a **body** and a **footer**.  The header has a special
format that includes a **type**, a **scope** and a **subject**:

> ```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

> [Full explanation](https://github.com/conventional-changelog/conventional-changelog-angular/blob/master/convention.md)

Under the hood this plugin uses [gradle-git](https://github.com/ajoberstar/gradle-git)'s release-base plugin and configures it to automatically increment the major, minor or patch version depending on the commit messages since the last release. Releases are only performed on certain branches (/master/ and /(?:release[-\/])?\d+(?:\.\d+)?\.x/ by default). On other branches only SNAPSHOT versions are built. On these branches the branch name is automatically appended to the version.

When doing a final release -- in addition to the default behavior of gradle-git to tag the release and push the tag -- if the origin repository is a github repository, the plugin generates a changelog and creates a [release](https://help.github.com/articles/about-releases/) on GitHub.

## Setup

### Apply the plugin in the rootProject (and only there) of your gradle build.

#### Gradle >= 2.1

```groovy
plugins {
    id 'de.gliderpilot.semantic-release' version '1.0.0'
}
```

#### Gradle < 2.1

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

### Setup the release task

Don't configure the version property of the gradle project (no version in `gradle.properties`, no version in `build.gradle`)

Example for a java project deploying to maven
```groovy
apply plugin: 'java'
apply plugin: 'maven-publish'
group = 'org.example'
publishing {
    [...]
}
```

There is a new `release` task, which automatically dependsOn the `build` task and is finalizedBy the `publish` task
(and the `uploadArchives` task, if you use the old publishing mechanism).

### Uploading SNAPSHOT and releases to different repositories

Because the `release` task does automatically execute the `publish` task, you must take care of configuring only valid
repositories.

```groovy
publishing {
    repositories {
        maven {
            if (version.toString().endsWith("-SNAPSHOT") {
                url "http://.../snapshots"
            } else {
                url "http://.../releases"
            }
        }
    }
}
```

### Uploading using other plugins

If you need other mechanisms to upload your artifacts, you need to manually configure this.

```groovy
if (!version.toString().endsWith('-SNAPSHOT'))
    publish.dependsOn publishPlugins, bintrayUpload
else if ((System.getenv('TRAVIS_PULL_REQUEST') ?: "false") == "false")
    publish.dependsOn artifactoryPublish
```

### Enable upload of the changelog to GitHub

In order to automatically upload the changelog to GitHub, you need a [GitHub token](https://help.github.com/articles/creating-an-access-token-for-command-line-use/) and tell the plugin about it.

```groovy
project.ext.ghToken = project.hasProperty('ghToken') ? project.getProperty('ghToken') : System.getenv('GH_TOKEN') ?: null
semanticRelease {
    changeLog {
        ghToken = project.ghToken
    }
}
```

### Enable upload release files to GitHub 

The **ghToken** is mandatory.

```groovy
apply plugin: 'java'
task sourcesJar(type: Jar) {
    classifier = 'sources'
    from sourceSets.main.allSource
}
semanticRelease {
    changeLog {
        releaseAssets(jar, sourcesJar)
    }
}
```


### Setup travis-ci

First, you need to [configure the environment variable GH_TOKEN in travis](http://docs.travis-ci.com/user/environment-variables/). Then you need a `.travis.yml`

```yml
sudo: false
addons:
  apt:
    packages:
    - git
language: java
jdk: oraclejdk7
env: TERM=dumb
branches:
  except:
    - /^v\d+\.\d+\.\d+$/
before_install:
  - git fetch --unshallow
  - git config user.email "name@example.com"
  - git config user.name "Travis-CI"
  - git config url.https://.insteadOf git://
  - git checkout -qf $TRAVIS_BRANCH
install:
  - echo "skip default gradlew assemble"
script:
  - ./gradlew release -Dorg.ajoberstar.grgit.auth.username=${GH_TOKEN} -Dorg.ajoberstar.grgit.auth.password
```

## Supported workflows

By default the plugin allows a couple of workflows (aka branching strategies).

### Master only

Just commit on `master`. As soon as you push to origin, travis will build a new version.

### [Git-Flow](http://nvie.com/posts/a-successful-git-branching-model/)

Develop on `develop` and feature branches. Travis will build SNAPSHOT versions on these branches.

To create a release, just create a branch `release/1.x` or `release/1.0.x`, and travis will create the release.

### [GitHub-Flow](https://guides.github.com/introduction/flow/index.html)

Develop features on branches branched from master. Once a feature is merged back to master, a version is released.

## Configuration

You can tweak the default behavior of the plugin using two extensions.

### Semantic Release extension

```groovy
semanticRelease {
    changeLog {
        changeScope = { org.ajoberstar.grgit.Commit ->
            // return org.ajoberstar.gradle.git.release.semver.ChangeScope
            // return MAJOR, MINOR or PATH to create a release
            // return null, if this commit is not relevant (e.g. only doc)
            [...]
        }
        changeLog = { List<Commit> commits, org.ajoberstar.gradle.git.release.base.ReleaseVersion version ->
            """\
                Release of $version.version

                [...]
            """.stripIndent()
        }
    }
    releaseBranches {
        include 'stable'
    }
    branchNames {
        // feature branches are dev/... instead of feature/...
        // the version on branch dev/foo should be 1.0.0-foo-SNAPSHOT
        // and not 1.0.0-dev-foo-SNAPSHOT
        replace ~/^dev\/(.*)$/, '$1'
    }
}
```

### Version Strategies

Since this plugin uses gradle-git under the cover, you can configure this plugin as [described in their wiki](https://github.com/ajoberstar/gradle-git/wiki/Release%20Plugins%201.x).

The plugin does configure gradle-git with one versionStrategy and one defaultVersionStrategy. The defaultVersionStrategy is responsible for building SNAPSHOT versions. The semantic-release versionStrategy is only used, if there are

* relevant changes (features with or without BREAKING CHANGES, bugfixes or performance improvements)
* the branch is master or a release branch
* the task `release` was used and
* the workspace is clean

The `gradle-semantic-release-plugin` defines it's own extension `semanticRelease`. When configuring gradle-git, you **must not** use the VersionStrategies defined by gradle-git directly, but you **must use** the releaseStrategy or snapshotStrategy of the semanticRelease extension as a base and use copyWith. Otherwise the version will not be inferred based on the commit messages.

```groovy
release {
    // replace the default strategy to add '-FINAL' to the version
    versionStrategy semanticRelease.releaseStrategy.copyWith{
        preReleaseStrategy: { it.copyWith(inferredPreRelease: 'FINAL') }
    }

    // add a second strategy to create release candidates from 'rc/.*' branches
    versionStrategy semanticRelease.releaseStrategy.copyWith(
        // the type is important, without type you would again replace the default strategy
        type: 'rc',
        selector: { SemVerStrategyState state ->
            !state.repoDirty && state.currentBranch.name ==~ /rc\/.*/ &&
                    semanticRelease.semanticStrategy.canRelease(state) && project.gradle.startParameter.taskNames.find { it == 'release' }
        },
        preReleaseStrategy: StrategyUtil.all({ it.copyWith(inferredPreRelease: 'rc') }, Strategies.PreRelease.COUNT_INCREMENTED)
    )
}
```

## ITYM*FAQ*LT
> I think you might frequently ask questions like these

### Is there a way to preview which version would currently get published?

If you run `./gradlew` locally, the version that would be build gets logged (just remove `-SNAPSHOT`).

### Can I run this on my own machine rather than on a CI server?

Of course you can, but this doesn't necessarily mean you should. Running your tests on an independent machine before releasing software is a crucial part of this workflow. Also it is a pain to set this up locally, with tokens lying around and everything.

### Can I manually trigger the release of a specific version?

You can trigger a release by pushing to your GitHub repository. You deliberately cannot trigger a _specific_ version release, because this is the whole point of `semantic-release`. Start your packages with `1.0.0` and semver on. You can however prevent an accidental major version bump by using a branch pattern `release/\d+\.x`. And you can prevent an accidental minor version bump by using a branch pattern `release/\d+\.\d+\.x`. Using these two branchname patterns, you can also manually trigger a version bump without a correspondent commit message.

### Is it _really_ a good idea to release on every push?

It is indeed a great idea because it _forces_ you to follow best practices. If you don't feel comfortable making every passing feature or fix on your master branch addressable you might not treat your master right. Have a look at [branch workflows](https://guides.github.com/introduction/flow/index.html). If you still think you should have control over the exact point in time of your release, e.g. because you are following a release schedule, you can release only on the release` branch and push your code there in certain intervals.

### Why should I trust `semantic-release` with my releases?

`gradle-semantic-release-plugin` has a full unit- and integration-test-suite. Additionally we eat our own dogfood and release using our own plugin -- A new version won't get published if there is an error.

## License

Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/
2015 © Tobias Schulte, based on the ideas of the semantic-release plugin of Stephan Bönnemann and [contributors](https://github.com/semantic-release/semantic-release/graphs/contributors)
