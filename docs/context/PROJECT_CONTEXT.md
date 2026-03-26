# Project Context

Oryzem is being positioned as a SaaS platform for PMEs that need simple multi-tenant, multi-market coordination across customer and supplier operations.

For the current product slice, the active focus is temporarily narrowed to the User + Organization core so the initial release can stabilize identity, membership and tenant administration before portfolio execution returns.

Core themes:
- user and organization administration as the first release anchor
- straightforward collaboration between customer and supplier organizations
- visibility over delivery risk, execution bottlenecks and operational commitments
- contextual access control by tenant, organization and market
- low-friction administration suitable for smaller and medium-sized businesses

This is not an automotive-only positioning. The platform architecture should stay broad enough for regional, commercial and operational collaboration scenarios across different SME segments.

Temporary scope note:
- `portfolio`, `program`, `project`, `product`, `item`, `deliverable`, `document` and `open issue` runtime surfaces are frozen
- portfolio data has been reset through Flyway while keeping `organization`, `tenant`, `user_membership` and related authorization structures intact
- future portfolio retomada should reuse the preserved schema instead of recreating the domain from scratch
