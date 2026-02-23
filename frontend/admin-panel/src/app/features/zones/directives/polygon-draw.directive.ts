import { Directive, Input, Output, EventEmitter, OnInit, OnDestroy, inject } from '@angular/core';
import { MapboxService } from '../services/mapbox.service';
import { GeometryService } from '../services/geometry.service';
import { Map, MapMouseEvent, MapTouchEvent } from 'mapbox-gl';

export interface PolygonPoint {
  id: string;
  lngLat: [number, number];
}

@Directive({
  selector: '[appPolygonDraw]',
  standalone: true
})
export class PolygonDrawDirective implements OnInit, OnDestroy {
  @Input() enabled: boolean = false;
  @Input() coordinates: [number, number][] = [];
  @Input() editable: boolean = true;
  @Output() coordinatesChange = new EventEmitter<[number, number][]>();
  @Output() pointAdded = new EventEmitter<[number, number]>();
  @Output() pointRemoved = new EventEmitter<number>();
  @Output() polygonComplete = new EventEmitter<[number, number][]>();

  private mapboxService = inject(MapboxService);
  private geometryService = inject(GeometryService);
  private map: Map | null = null;
  private markers: any[] = [];
  private isDrawing: boolean = false;
  private currentPolygon: [number, number][] = [];

  ngOnInit(): void {
    this.map = this.mapboxService.getMap();
    if (!this.map) {
      console.error('Mapbox map not initialized');
      return;
    }

    this.map.on('load', () => {
      this.initializeDrawing();
    });

    if (this.map.loaded()) {
      this.initializeDrawing();
    }

    if (this.coordinates.length > 0) {
      this.loadPolygon(this.coordinates);
    }
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  private initializeDrawing(): void {
    if (!this.map) return;

    if (this.enabled) {
      this.map.getCanvas().style.cursor = 'crosshair';
      this.map.on('click', this.onMapClick.bind(this));
    }
  }

  private onMapClick(e: MapMouseEvent | MapTouchEvent): void {
    if (!this.enabled || !this.map) return;

    const lngLat: [number, number] = [e.lngLat.lng, e.lngLat.lat];
    this.addPoint(lngLat);
  }

  addPoint(lngLat: [number, number]): void {
    this.currentPolygon.push(lngLat);
    this.coordinatesChange.emit([...this.currentPolygon]);
    this.pointAdded.emit(lngLat);
    this.updatePolygonDisplay();
  }

  removePoint(index: number): void {
    if (index >= 0 && index < this.currentPolygon.length) {
      this.currentPolygon.splice(index, 1);
      this.coordinatesChange.emit([...this.currentPolygon]);
      this.pointRemoved.emit(index);
      this.updatePolygonDisplay();
    }
  }

  completePolygon(): void {
    if (this.currentPolygon.length >= 3) {
      // Fermer le polygone si nécessaire
      const first = this.currentPolygon[0];
      const last = this.currentPolygon[this.currentPolygon.length - 1];
      if (first[0] !== last[0] || first[1] !== last[1]) {
        this.currentPolygon.push([...first]);
      }
      this.polygonComplete.emit([...this.currentPolygon]);
    }
  }

  loadPolygon(coordinates: [number, number][]): void {
    // Convertir du format backend [lat, lon] vers Mapbox [lon, lat]
    this.currentPolygon = this.geometryService.convertToMapboxCoordinates(coordinates);
    this.updatePolygonDisplay();
  }

  private updatePolygonDisplay(): void {
    if (!this.map) return;

    const sourceId = 'polygon-source';
    const layerId = 'polygon-layer';
    const lineLayerId = 'polygon-line-layer';

    // Créer le GeoJSON pour le polygone
    const closedCoords = this.currentPolygon.length > 0 && 
      (this.currentPolygon[0][0] !== this.currentPolygon[this.currentPolygon.length - 1][0] ||
       this.currentPolygon[0][1] !== this.currentPolygon[this.currentPolygon.length - 1][1])
      ? [...this.currentPolygon, this.currentPolygon[0]]
      : this.currentPolygon;

    const geojson = {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [closedCoords]
      }
    };

    // Ajouter ou mettre à jour la source
    this.mapboxService.addGeoJSONSource(sourceId, geojson);

    // Ajouter les couches si elles n'existent pas
    if (!this.map.getLayer(layerId)) {
      this.mapboxService.addPolygonLayer(layerId, sourceId, {
        'fill-color': '#3b82f6',
        'fill-opacity': 0.3
      });
    }

    if (!this.map.getLayer(lineLayerId)) {
      this.mapboxService.addLineLayer(lineLayerId, sourceId, {
        'line-color': '#1e40af',
        'line-width': 2
      });
    }

    // Mettre à jour les marqueurs pour les points
    this.updateMarkers();
  }

  private updateMarkers(): void {
    // Supprimer les anciens marqueurs
    this.markers.forEach(marker => marker.remove());
    this.markers = [];

    if (!this.map) return;

    // Ajouter un marqueur pour chaque point
    this.currentPolygon.forEach((lngLat, index) => {
      const marker = this.mapboxService.addMarker(lngLat, {
        color: '#3b82f6',
        draggable: this.editable
      });

      if (this.editable) {
        marker.on('dragend', () => {
          const newLngLat = marker.getLngLat();
          this.currentPolygon[index] = [newLngLat.lng, newLngLat.lat];
          this.coordinatesChange.emit([...this.currentPolygon]);
          this.updatePolygonDisplay();
        });
      }

      this.markers.push(marker);
    });
  }

  clear(): void {
    this.currentPolygon = [];
    this.coordinatesChange.emit([]);
    this.cleanup();
  }

  private cleanup(): void {
    this.markers.forEach(marker => marker.remove());
    this.markers = [];

    if (this.map) {
      this.map.getCanvas().style.cursor = '';
      this.map.off('click', this.onMapClick);
    }
  }

  getCoordinates(): [number, number][] {
    // Retourner au format backend [lat, lon]
    return this.geometryService.convertToBackendCoordinates(this.currentPolygon);
  }
}
