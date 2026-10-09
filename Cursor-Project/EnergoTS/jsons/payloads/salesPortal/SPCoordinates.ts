
interface CustomerCoordinates {
    latitude: number;
    longitude: number;
    radiusMeters: number;
    page: number;
    size: number;
}

export function customerCoordinatePayload(): CustomerCoordinates {
    return {
        "latitude": 90,
        "longitude": 180,
        "radiusMeters": 500000,
        "page": 0,
        "size": 100
    }
}