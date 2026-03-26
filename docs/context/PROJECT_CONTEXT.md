# Project Context

Oryzem is being positioned as a SaaS platform for PMEs that need simple multi-tenant and multi-market administration across customer and supplier organizations.

The current product scope is the core administration slice:
- user lifecycle
- organization hierarchy
- membership-based access control
- tenant and market administration
- authentication, authorization and audit support around that core

Core themes:
- user and organization administration as the release anchor
- contextual access control by tenant, organization and market
- low-friction administration suitable for smaller and medium-sized businesses
- clear separation between identity data and runtime authorization context

Out of scope for the active product:
- portfolio
- program
- project
- product, item and deliverable execution
- operations
- reports
- portfolio-specific document storage

Those capabilities are not frozen in place anymore. They were removed from runtime and schema support so the codebase matches the active product boundary.
