# Task: create a page about `ValidatorRegistry` and how to use it

## Main idea
In simple cases the programmer using the Validation library does not need to interact
with `ValidatorRegistry` directly, as the library discovers and applies validators automatically.

However, in some cases it may be necessary to interact with the registry directly,
for example, to add a validator explicitly or to query the registry for registered validators.

## Features
- Discovery and loading validators using the `ServiceLoader` mechanism.
- Setting `validator` placeholder in the reported violation messages.
- Adding validators.
- Quering and removal of registered validators.

## Edge case
- Overwriting validation by automatically registered validators by removing them from the
  registry and adding new validators.

## Placement
- Add the page after "Implement a validator" page.
- Update `sidenav.yml` accodringly.
