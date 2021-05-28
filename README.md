# New repository template
This repository serves as a template for new repositories of the Spine framework.

Please see the [config][config-repo] repository
for shared configurations and scripts. `config` is added to this repository as a Git submodule.

## Applying to new sub-project
                              
### 1. Replace `"template"` with your project name
Search for `template` throughout the code and replace it with the name of your sub-project.

### 2. Review the project structure  

The “project” in this repository has two modules called `template-client` and
`template-server`. These modules enable client- and server-side dependencies correspondingly.
See corresponding `build.gradle.kts` under these modules files for details.

The `-client` module is for client-side API.

If your project is going to have server-side entities, you'd probably need to have
the `-server` module so that the build produces the server-side artifact.

All it all, the structure of a new project highly depends on its goal, so 
feel free to change [settings.gradle.kts](settings.gradle.kts) file according to your needs.

### 3. Rename `.travis._yml` to `.travis.yml`
The template repository goes with the rename file to avoid unnecessary Travis builds.   

### 4. Update `.travis.yml` file

#### 4.1 Encrypted variable values
  * Create encrypted value for the `GCS_SECRET` variable and put it under `env/global` section
    in the file.
  * Create encrypted value for the `GITHUB_TOKEN` variable and put it under `env/global` section
    in the file.
 
For detailed instructions please see:   
 * [Defining encrypted variables in .travis.yml][def-var-travis]
 * [Encryption keys / Usage][encryption-keys-travis] 

#### 4.2 Update keys for `credentials.tar`
Please follow [this procedure][encrypt-credentials] and update the line which starts from 
`openssl aes-256-cbc` accordingly.

## Updating configurations
When you learn that `config` was changed, run `./config/pull` to apply updated configurations
to your repository.

## Useful links
 * [Splitting a subfolder out into a new repository][folder-to-repo] 

[config-repo]: https://github.com/SpineEventEngine/config
[def-var-travis]: https://docs.travis-ci.com/user/environment-variables/#defining-encrypted-variables-in-travisyml
[encryption-keys-travis]: https://docs.travis-ci.com/user/encryption-keys#usage
[encrypt-credentials]: https://github.com/SpineEventEngine/SpineEventEngine.github.io/wiki/Encrypting-credential-files-for-Travis
[folder-to-repo]: https://docs.github.com/en/free-pro-team@latest/github/using-git/splitting-a-subfolder-out-into-a-new-repository
