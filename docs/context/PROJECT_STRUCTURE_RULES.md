# Project Structure Rules

## Root Node
- Every project has exactly one root structure node: `PROJECT_ID-ROOT`.
- The root node always uses sequence `1`.
- The root node is created from the first structure level of the linked `ProjectStructureTemplate`.

## Level Semantics
- Level sequences must be contiguous and start at `1`.
- Every intermediate level must allow children.
- The last level must not allow children.
- Child nodes can only be created under the immediately previous level.

## Artifact Placement
- Milestones can only target levels where `allowsMilestones = true`.
- Deliverables can only target levels where `allowsDeliverables = true`.
- In simple templates, milestones and deliverables may live on the root node.
- In complex templates, milestones and deliverables belong to the node instantiated for the configured structure level.

## Visibility Inheritance
- The root node inherits the project visibility.
- New child nodes inherit the parent node visibility unless an explicit override is provided.
- Milestones and deliverables keep their own configured visibility scope.

## Ordering
- Levels are ordered by `sequenceNo`.
- Sibling nodes are ordered by `sequenceNo` and appended at the end on creation.
- Tree reads return nodes in stable sequence order.

## Read Model Split
- Write flows stay in application use cases.
- Read flows for project lists, dashboard, milestones, deliverables, pending review queue, and responsible queue live in `application.query`.
- Controllers return dedicated DTO/view models and do not expose domain aggregates.
