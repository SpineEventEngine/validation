## Architecture

The library is a set of plugins for [Spine Compiler](https://github.com/SpineEventEngine/compiler).

Each target language is a separate Compiler plugin.

Take a look at the following diagram to grasp a high-level library structure:

![High-level library structure overview](.github/readme/high_level_overview.png)

The workflow is the following:

- (1), (2) – user defines Protobuf messages with validation options.
- (3) – Protobuf compiler generates Java classes.
- (4), (5) – policies and views build the validation model.
- (6), (7) – Java plugin generates and injects validation code.
