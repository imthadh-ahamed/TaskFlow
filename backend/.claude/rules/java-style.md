# Java coding standards

- Target Java 21 unless the user explicitly changes the target.
- Follow the existing formatting and naming conventions in the repository.
- Use `UpperCamelCase` for types, `lowerCamelCase` for fields and methods, and meaningful constant names.
- Prefer composition over inheritance unless inheritance models a real subtype relationship.
- Use `final` for fields that should not be reassigned.
- Avoid field injection; use constructor injection.
- Avoid catching broad `Exception` unless the boundary genuinely requires it and the exception is handled meaningfully.
- Do not silently swallow exceptions.
- Use `Optional` primarily for return values where absence is meaningful, not for every field or parameter.
- Use Java records for simple immutable data carriers when appropriate.
- Avoid streams when a straightforward loop is clearer.
- Do not add Lombok or other convenience libraries unless already present or explicitly approved.
