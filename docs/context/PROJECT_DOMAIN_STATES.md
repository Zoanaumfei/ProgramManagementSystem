# Project Domain States

## Project
- `DRAFT`: initial editable state.
- `PLANNED`: scoped and scheduled, but not executing.
- `ACTIVE`: execution in progress.
- `ON_HOLD`: temporarily paused.
- `COMPLETED`: finished, immutable.
- `CANCELLED`: terminated, immutable.

Allowed transitions:
- `DRAFT -> PLANNED|CANCELLED`
- `PLANNED -> ACTIVE|CANCELLED`
- `ACTIVE -> ON_HOLD|COMPLETED|CANCELLED`
- `ON_HOLD -> ACTIVE|CANCELLED`

## Deliverable
- `NOT_STARTED`: initial state.
- `READY_FOR_SUBMISSION`: prepared for formal submission.
- `SUBMITTED`: sent for review.
- `UNDER_REVIEW`: being evaluated.
- `APPROVED`: accepted.
- `REJECTED`: rejected and may require resubmission.

## Submission
- `SUBMITTED`: created and awaiting action.
- `APPROVED`: accepted as final decision.
- `REJECTED`: rejected with review comment.

## Invariants
- Completed or cancelled projects do not accept regular mutation.
- Deliverable and submission transitions are enforced in domain/application flow and covered by negative tests.
