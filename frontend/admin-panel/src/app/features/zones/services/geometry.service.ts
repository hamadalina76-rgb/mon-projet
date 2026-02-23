import { Injectable } from '@angular/core';
import * as turf from '@turf/turf';
import { PolygonCoordinates, ZoneGeometry, Point } from '../models/zone-geometry.model';

@Injectable({
  providedIn: 'root',
})
export class GeometryService {
  /**
   * Convertir les coordonnées du format backend [lat, lon] vers le format Mapbox [lon, lat]
   */
  convertToMapboxCoordinates(coordinates: [number, number][]): [number, number][] {
    return coordinates.map(([lat, lon]) => [lon, lat]);
  }

  /**
   * Convertir les coordonnées du format Mapbox [lon, lat] vers le format backend [lat, lon]
   */
  convertToBackendCoordinates(coordinates: [number, number][]): [number, number][] {
    return coordinates.map(([lon, lat]) => [lat, lon]);
  }

  /**
   * Parser le boundaryJson en tableau de coordonnées [lat, lon]
   * Accepte: [[lat,lon],...] ou GeoJSON {type:"Polygon", coordinates:[[[lon,lat],...]]}
   */
  parseBoundaryJson(boundaryJson: string): [number, number][] {
    try {
      const parsed = JSON.parse(boundaryJson);
      if (Array.isArray(parsed)) {
        return parsed as [number, number][];
      }
      if (parsed?.type === 'Polygon' && Array.isArray(parsed.coordinates?.[0])) {
        const ring = parsed.coordinates[0] as [number, number][];
        return ring.map(([lon, lat]) => [lat, lon]);
      }
      return [];
    } catch (e) {
      console.error('Erreur lors du parsing du boundaryJson', e);
      return [];
    }
  }

  /**
   * Convertir un tableau de coordonnées en boundaryJson
   */
  coordinatesToBoundaryJson(coordinates: [number, number][]): string {
    return JSON.stringify(coordinates);
  }

  /**
   * Créer un ring GeoJSON valide (min 4 positions pour Turf/GeoJSON LinearRing)
   */
  private toValidRing(coordinates: [number, number][]): [number, number][] | null {
    if (!coordinates || coordinates.length < 3) return null;
    const mapboxCoords = this.convertToMapboxCoordinates(coordinates);
    const closed =
      mapboxCoords[0][0] !== mapboxCoords[mapboxCoords.length - 1][0] ||
      mapboxCoords[0][1] !== mapboxCoords[mapboxCoords.length - 1][1]
        ? [...mapboxCoords, mapboxCoords[0]]
        : mapboxCoords;
    return closed.length >= 4 ? closed : null;
  }

  /**
   * Calculer la surface d'un polygone en km²
   */
  calculateArea(coordinates: [number, number][]): number {
    const ring = this.toValidRing(coordinates);
    if (!ring) return 0;
    try {
      const polygon = turf.polygon([ring]);
      const area = turf.area(polygon);
      return parseFloat((area / 1000000).toFixed(2));
    } catch {
      return 0;
    }
  }

  /**
   * Calculer le centre géométrique d'un polygone
   */
  calculateCenter(coordinates: [number, number][]): [number, number] {
    const ring = this.toValidRing(coordinates);
    if (!ring) return [0, 0];
    try {
      const polygon = turf.polygon([ring]);
      const centroid = turf.centroid(polygon);
      const [lon, lat] = centroid.geometry.coordinates;
      return [lat, lon];
    } catch {
      return [0, 0];
    }
  }

  /**
   * Calculer le périmètre d'un polygone en km
   */
  calculatePerimeter(coordinates: [number, number][]): number {
    const ring = this.toValidRing(coordinates);
    if (!ring) return 0;
    try {
      let perimeter = 0;
      for (let i = 0; i < ring.length - 1; i++) {
        perimeter += turf.distance(turf.point(ring[i]), turf.point(ring[i + 1]), { units: 'kilometers' });
      }
      return parseFloat(perimeter.toFixed(2));
    } catch {
      return 0;
    }
  }

  /**
   * Vérifier si un point est dans un polygone
   */
  isPointInPolygon(point: Point, coordinates: [number, number][]): boolean {
    const ring = this.toValidRing(coordinates);
    if (!ring) return false;
    try {
      const polygon = turf.polygon([ring]);
      const pt = turf.point([point.longitude, point.latitude]);
      return turf.booleanPointInPolygon(pt, polygon);
    } catch {
      return false;
    }
  }

  /**
   * Vérifier si deux polygones se chevauchent
   */
  checkOverlap(coords1: [number, number][], coords2: [number, number][]): boolean {
    const ring1 = this.toValidRing(coords1);
    const ring2 = this.toValidRing(coords2);
    if (!ring1 || !ring2) return false;
    try {
      const polygon1 = turf.polygon([ring1]);
      const polygon2 = turf.polygon([ring2]);
      return turf.booleanOverlap(polygon1, polygon2) || turf.booleanIntersects(polygon1, polygon2);
    } catch {
      return false;
    }
  }

  /**
   * Valider un polygone (min 3 points, pas d'auto-intersection)
   */
  validatePolygon(coordinates: [number, number][]): { valid: boolean; error?: string } {
    if (coordinates.length < 3) {
      return { valid: false, error: 'Un polygone doit contenir au moins 3 points' };
    }
    
    // Vérifier les coordonnées valides
    for (const [lat, lon] of coordinates) {
      if (lat < -90 || lat > 90) {
        return { valid: false, error: `Latitude invalide: ${lat}` };
      }
      if (lon < -180 || lon > 180) {
        return { valid: false, error: `Longitude invalide: ${lon}` };
      }
    }
    
    // Vérifier l'auto-intersection avec Turf.js
    const ring = this.toValidRing(coordinates);
    if (!ring) return { valid: false, error: 'Un polygone doit contenir au moins 3 points' };
    try {
      const polygon = turf.polygon([ring]);
      if (!turf.booleanValid(polygon)) {
        return { valid: false, error: 'Le polygone contient des auto-intersections' };
      }
    } catch {
      return { valid: false, error: 'Erreur lors de la validation du polygone' };
    }
    
    return { valid: true };
  }

  /**
   * Calculer toutes les propriétés géométriques d'une zone
   */
  calculateGeometry(coordinates: [number, number][]): ZoneGeometry {
    return {
      areaKm2: this.calculateArea(coordinates),
      center: this.calculateCenter(coordinates),
      perimeterKm: this.calculatePerimeter(coordinates),
      pointCount: coordinates.length
    };
  }
}
