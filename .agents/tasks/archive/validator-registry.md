# Task: Implement a registry for custom validators of Protobuf messages

This task involves creating a registry system that allows for the dynamic registration and retrieval
of custom validators for Protobuf messages.
The registry should support adding new validators at runtime and efficiently look up validators
based on message types.

## Goals
- Design a flexible and efficient registry for custom validators implementing
the `MessageValidator` interface.

## Target class
- `io.spine.validation.ValidatorRegistry`

## Module
- `jvm-runtime`

## Features
- Ability to add custom validators for specific Protobuf message types.
- Support several validators per type.
- Ability to remove validators from the registry for a given type.
- Ability to clear the whole registry.
- Load validators from the classpath using Java's ServiceLoader mechanism.
- API for validating a message by looking up its type in the registry and
  applying all associated validators.
