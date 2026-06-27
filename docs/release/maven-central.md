# Maven Central Release

This project has Maven Central publishing infrastructure, but public release is blocked until the project namespace and repository metadata are real.

## Metadata Blocker

This worktree did not have an `origin` remote configured when the release metadata was added. The root POM and standalone `jfoundry-dependencies` BOM therefore use the conservative placeholder URL `https://github.com/REPLACE_WITH_OWNER/jfoundry` for `<url>` and `<scm>`.

Before any public Maven Central release, replace all placeholder URL and SCM values with the real repository URL and verify that the published groupId is authorized for that namespace.

## Prerequisites

- Java 21 or newer.
- Maven 3.9.0 or newer.
- A Sonatype Central Portal account with publishing rights for `org.jfoundry`.
- A Maven server entry named `central` in `~/.m2/settings.xml`.
- A GPG key available to Maven for artifact signing.
- Release versions in all reactor POMs. Central releases must not publish `*-SNAPSHOT` versions.
- Real project URL and SCM metadata in `pom.xml`.

Example server configuration:

```xml
<servers>
  <server>
    <id>central</id>
    <username>${env.CENTRAL_USERNAME}</username>
    <password>${env.CENTRAL_PASSWORD}</password>
  </server>
</servers>
```

## Local Verification

Run the regular package build first:

```bash
mvn -DskipTests package
```

Then run the release profile through `verify` so sources, Javadocs, and local signatures are exercised:

```bash
mvn -Prelease -DskipTests verify
```

The `verify` phase checks local artifact generation and signatures up to the GPG signing step. It does not upload or stage a Central Portal deployment bundle; that behavior is triggered during `deploy`.

If GPG is not configured locally, the release-profile verification may fail at the signing step. Failures before signing, including compilation, source JARs, Javadocs, metadata, placeholder metadata guards, or Central publishing plugin setup, must be fixed before release.

## Publish

After replacing placeholder metadata, setting release versions, and configuring Central credentials and GPG, deploy with the release profile:

```bash
mvn -Prelease -DskipTests deploy
```

The Central publishing plugin is configured with `autoPublish=false`, so the deployment uploads a staged deployment to Central Portal for manual review and publishing.
