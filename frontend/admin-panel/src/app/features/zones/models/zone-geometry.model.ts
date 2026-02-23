export interface ZoneGeometry {
  areaKm2: number;
  center: [number, number]; // [latitude, longitude]
  perimeterKm: number;
  pointCount: number;
}

export interface Point {
  latitude: number;
  longitude: number;
}

export interface PolygonCoordinates {
  coordinates: [number, number][]; // [[lat, lon], [lat, lon], ...]
}
