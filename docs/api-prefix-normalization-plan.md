# API Prefix Normalization Plan

## Why
Current routes mix two styles:
- `/admin/**` (e.g. products, categories, orders)
- `/api/admin/**` (e.g. product-parameters, parameter-options)

This increases frontend integration cost and makes permission rules harder to reason about.

## Target
Unify admin routes to one prefix:
- Preferred: `/api/admin/**`

Public and user routes stay unchanged:
- Public: `/api/products/**`, `/api/quote`, `/uploads/**`
- User-auth routes: `/api/cart/**`, `/api/orders/**`, `/api/addresses/**`, `/api/auth/**`

## Migration Steps (Non-breaking)
1. Add v2 aliases
- Keep existing `/admin/**` endpoints.
- Add equivalent `/api/admin/**` endpoints for products/categories/orders.

2. Frontend switch
- Update `admin-web` API files to call only `/api/admin/**`.
- Keep backend dual routes during transition.

3. Permission simplification
- In `SaTokenConfig`, protect `/api/admin/**` once.
- Keep `/admin/**` protection until old clients are removed.

4. Deprecation window
- Mark `/admin/**` as deprecated in docs and release notes.
- Remove `/admin/**` in a later version.

## Acceptance Checklist
- Admin web can complete product/category/order/parameter operations with only `/api/admin/**`.
- No 404/401 regressions during migration.
- Interceptor config has no duplicate gaps.
