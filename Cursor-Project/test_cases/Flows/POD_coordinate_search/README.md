# POD coordinate search – Map view (PHN-2197)

This folder contains manual test cases for backend support of **searching/filtering PODs (Points of Delivery)** by geographic coordinates for a map view.

- Main endpoint under test: **GET `/pod/list`** (search), including coordinate-based variants (e.g. bounding box, point+radius).
- Additional focus: request validation, backward compatibility for existing callers, pagination/sorting behaviour, and handling PODs with missing/invalid coordinates.

