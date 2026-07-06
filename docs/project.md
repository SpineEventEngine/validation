# Project: Validation

## Overview

Validation is a Spine SDK JVM library that provides the project's validation
model and related integration points. Its role in the organisation is to define
and expose validation behaviour in a reusable form so other modules can apply
consistent validation rules and report validation results through a stable API.

## Architecture

This repository is a library module. Its public surface should stay focused on
validation concepts, rule execution, and result/reporting types that other Spine
SDK components can depend on without pulling in application-specific behaviour.
Keep implementation details behind the library boundary, prefer additive changes
to public APIs, and preserve compatibility for downstream JVM consumers.

Read [`.agents/guidelines/jvm-project.md`](../.agents/guidelines/jvm-project.md) for build stack, coding style,
tests, and versioning.
