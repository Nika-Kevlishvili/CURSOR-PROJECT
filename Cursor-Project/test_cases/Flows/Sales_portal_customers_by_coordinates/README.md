# Sales Portal – Customers by Coordinates (PHN-2529)

Test cases for the Sales Portal endpoint **GET /sales-portal/customers/by-coordinates**: request validation, authentication, success with coordinates and pagination, recontract/acquisition classification, and regression for existing callers.

| File | Content |
|------|--------|
| **Request_validation_and_auth.md** | Valid request success, invalid/missing coordinates, missing or invalid auth (401/403). |
| **By_coordinates_and_pagination.md** | Success with coordinates and pagination; empty result set; pagination metadata. |
| **Recontract_acquisition_classification.md** | Response includes recontract/acquisition classification; correct classification values. |
