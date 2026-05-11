# Task: Describe the technical details of the documentation process

## Content that we know needs to be there
- Description of the `docs` module and its purpose.
- Brief explanation that we use Hugo, the `_preview` directory and how the `docs` module
  is going to be used when building the documentation section of the spine.io website.
- Explanation of the build tasks defined in `docs/build.gradle.kts` and their purposes.
  Use `docs/GRADLE.md` as a reference or even text source for this section.
- Explanation of the paths used in the scripts and how to work with them.
- Description of how the example sources are included in the documentation project.
- Explanation of using Spine Time as an implementaiton example and that the library is added as GitModule because of this.
- Documentation of `settings/embed-code.yml` file and source roots under it.


## Procedures to be documented

### Incrementing the version of Validation
1. Update the version in `version.gradle.kts` file.
2. Run `./gradlew :docs:updatePluginVersions` to update the version of Validation Graldle plugin
   in the examples and files documentation.
3. Commit the change to `version.gradle.kts` file using the commit message
   ```text
   Bump version -> `<new_version>`
   ```
4. Commit the change to the remaining files using the commit message
   ```text
   Bump Validation -> `<new_version>`
   ```
5. Run `./gradlew clean build` and commit `pom.xml` and `dependencies.md` using the commit
   message `Update dependency reports` as usually when incrementing the version in
   Spine SDK projects.

### Updating the version of the CoreJvm Compiler
1. Update the version in the dependency object `CoreJvmCompiler` under `buildSrc`.
2. Run `./gradlew :docs:updatePluginVersions` to update the version of CoreJvm Graldle plugin
   in the examples and files documentation.

## Tasks that are known to be done
- [ ] Add `docs/README.md` file with the brief description, and the link to 
  the page under the `content` directory that explains the documentation process in more detail.
  Remove `GRADLE.md` file as no longer needed after that.
