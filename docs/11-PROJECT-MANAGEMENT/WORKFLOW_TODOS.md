# GitHub Workflow TODOs

## Active

- [x] TODO #1: Fix `flutter-ci-cd.yml` so Android tag releases can actually run
- [x] TODO #2: Fix `code-quality.yml` summary job so it waits for all relevant jobs and reports real statuses
- [x] TODO #3: Fix `java-ci.yml` final result job so non-build job failures cannot be masked
- [x] TODO #4: Simplify `flutter-ci-cd.yml` Firebase deploy job to use the tested artifact instead of rebuilding and then downloading over it
- [x] TODO #5: Clean mojibake step names in workflow files to improve GitHub Actions log readability

## Notes

- Created on 2026-04-14 during GitHub Actions workflow review.
- Work should be completed in order unless a dependency between items changes the sequence.
