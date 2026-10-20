# Source plan notes

- The source implementation plan contains `T0.1`, `T0.2`, and `T0.4`; it does not define a `T0.3`. No task was invented for the missing ID.
- Dependencies are not explicitly declared for every source task. The generated `Depends on` field uses the previous task in plan order as an inferred sequencing dependency, and each task says to confirm it before working out of sequence.
- Verification sections are intentionally marked "Not run yet" because generating these specifications does not execute the implementation's build or tests.
