import { Injectable } from '@angular/core';
import mapboxgl, { Map, Marker, Popup } from 'mapbox-gl';
import { environment } from '@environments/environment';

// Plugin RTL pour l'arabe et l'hébreu (affichage correct du texte) — chargé depuis assets
const rtlPluginUrl =
  typeof window !== 'undefined'
    ? `${window.location.origin}/assets/js/mapbox-gl-rtl-text.js`
    : '';
if (rtlPluginUrl) {
  mapboxgl.setRTLTextPlugin(rtlPluginUrl, () => {}, true);
}

export interface GeocodingResult {
  placeName: string;
  center: [number, number];
  bbox?: [number, number, number, number];
}

@Injectable({
  providedIn: 'root',
})
export class MapboxService {
  private map: Map | null = null;
  private markers: Marker[] = [];

  /**
   * Initialiser la carte Mapbox
   */
  initializeMap(containerId: string, options: {
    center?: [number, number];
    zoom?: number;
    style?: string;
  } = {}): Map {
    if (!environment.mapboxToken) {
      throw new Error('Mapbox token non configuré dans environment');
    }

    mapboxgl.accessToken = environment.mapboxToken;

    this.map = new mapboxgl.Map({
      container: containerId,
      style: options.style || 'mapbox://styles/mapbox/streets-v12',
      center: options.center || [10.1815, 36.8065], // Tunis par défaut
      zoom: options.zoom || 12,
      attributionControl: true
    });

    // Ajouter les contrôles de navigation
    this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');
    this.map.addControl(new mapboxgl.FullscreenControl(), 'top-right');

    return this.map;
  }

  /**
   * Obtenir l'instance de la carte
   */
  getMap(): Map | null {
    return this.map;
  }

  /**
   * Ajouter un marqueur sur la carte
   */
  addMarker(lngLat: [number, number], options: {
    popup?: string | HTMLElement;
    color?: string;
    draggable?: boolean;
  } = {}): Marker {
    if (!this.map) {
      throw new Error('La carte n\'est pas initialisée');
    }

    const el = document.createElement('div');
    el.className = 'custom-marker';
    el.style.width = '20px';
    el.style.height = '20px';
    el.style.borderRadius = '50%';
    el.style.backgroundColor = options.color || '#3b82f6';
    el.style.border = '2px solid white';
    el.style.cursor = 'pointer';

    const marker = new mapboxgl.Marker({
      element: el,
      draggable: options.draggable || false
    })
      .setLngLat(lngLat)
      .addTo(this.map);

    if (options.popup) {
      const popup = new Popup({ offset: 25 }).setHTML(
        typeof options.popup === 'string' ? options.popup : ''
      );
      marker.setPopup(popup);
    }

    this.markers.push(marker);
    return marker;
  }

  /**
   * Supprimer tous les marqueurs
   */
  clearMarkers(): void {
    this.markers.forEach(marker => marker.remove());
    this.markers = [];
  }

  /**
   * Ajouter une source GeoJSON à la carte
   */
  addGeoJSONSource(sourceId: string, data: any): void {
    if (!this.map) return;

    if (this.map.getSource(sourceId)) {
      (this.map.getSource(sourceId) as mapboxgl.GeoJSONSource).setData(data);
    } else {
      this.map.addSource(sourceId, {
        type: 'geojson',
        data: data
      });
    }
  }

  /**
   * Ajouter une couche de polygone
   */
  addPolygonLayer(layerId: string, sourceId: string, paint: any = {}): void {
    if (!this.map) return;

    if (this.map.getLayer(layerId)) {
      this.map.setPaintProperty(layerId, 'fill-color', paint['fill-color']);
      this.map.setPaintProperty(layerId, 'fill-opacity', paint['fill-opacity'] || 0.5);
      return;
    }

    this.map.addLayer({
      id: layerId,
      type: 'fill',
      source: sourceId,
      paint: {
        'fill-color': paint['fill-color'] || '#3b82f6',
        'fill-opacity': paint['fill-opacity'] || 0.5,
        'fill-outline-color': paint['fill-outline-color'] || '#1e40af'
      }
    });
  }

  /**
   * Ajouter une étiquette au centre d'un polygone (GeoJSON)
   */
  addZoneLabel(
    sourceId: string,
    centerLngLat: [number, number],
    label: string,
    textColor: string = '#1e293b'
  ): void {
    if (!this.map) return;
    const labelSourceId = `${sourceId}-label`;
    const labelLayerId = `${sourceId}-label-layer`;
    const geojson = {
      type: 'Feature',
      geometry: {
        type: 'Point',
        coordinates: centerLngLat
      },
      properties: { name: label }
    };
    if (this.map.getSource(labelSourceId)) {
      (this.map.getSource(labelSourceId) as mapboxgl.GeoJSONSource).setData(geojson as any);
    } else {
      this.map.addSource(labelSourceId, {
        type: 'geojson',
        data: geojson as any
      });
      this.map.addLayer({
        id: labelLayerId,
        type: 'symbol',
        source: labelSourceId,
        layout: {
          'text-field': ['get', 'name'],
          'text-size': 13,
          'text-font': ['Open Sans Bold', 'Arial Unicode MS Bold'],
          'text-allow-overlap': true,
          'text-ignore-placement': false
        },
        paint: {
          'text-color': textColor,
          'text-halo-color': '#ffffff',
          'text-halo-width': 1.5
        }
      });
    }
  }

  /**
   * Ajouter une couche de ligne pour les bordures
   */
  addLineLayer(layerId: string, sourceId: string, paint: any = {}): void {
    if (!this.map) return;

    if (this.map.getLayer(layerId)) {
      return;
    }

    this.map.addLayer({
      id: layerId,
      type: 'line',
      source: sourceId,
      paint: {
        'line-color': paint['line-color'] || '#1e40af',
        'line-width': paint['line-width'] || 2
      }
    });
  }

  /**
   * Supprimer une couche
   */
  removeLayer(layerId: string): void {
    if (!this.map || !this.map.getLayer(layerId)) return;
    this.map.removeLayer(layerId);
  }

  /**
   * Supprimer une étiquette de zone
   */
  removeZoneLabel(sourceId: string): void {
    const labelLayerId = `${sourceId}-label-layer`;
    const labelSourceId = `${sourceId}-label`;
    this.removeLayer(labelLayerId);
    this.removeSource(labelSourceId);
  }

  /**
   * Forcer le redimensionnement de la carte (utile après affichage)
   */
  resize(): void {
    this.map?.resize();
  }

  /**
   * Supprimer une zone spécifique de la carte (layers + sources)
   */
  removeZoneFromMap(zoneId: number): void {
    if (!this.map) return;
    const layerIds = [`zone-${zoneId}-label-layer`, `zone-layer-${zoneId}`, `zone-line-${zoneId}`];
    layerIds.forEach((id) => this.removeLayer(id));
    [`zone-${zoneId}-label`, `zone-${zoneId}`].forEach((id) => this.removeSource(id));
  }

  /**
   * Supprimer toutes les couches et sources de zones
   */
  clearAllZoneLayers(): void {
    if (!this.map) return;
    const style = this.map.getStyle();
    if (!style?.layers) return;
    const layerIds = style.layers
      .filter((l) => l.id.startsWith('zone-') && l.id !== 'current-polygon-layer' && l.id !== 'current-polygon-line')
      .map((l) => l.id);
    layerIds.forEach((id) => this.removeLayer(id));
    const sourceIds = Object.keys(style.sources || {}).filter(
      (s) => s.startsWith('zone-') && !s.includes('current-polygon')
    );
    sourceIds.forEach((s) => this.removeSource(s));
  }

  /**
   * Supprimer une source
   */
  removeSource(sourceId: string): void {
    if (!this.map || !this.map.getSource(sourceId)) return;
    this.map.removeSource(sourceId);
  }

  /**
   * Zoomer sur une zone
   */
  fitBounds(coordinates: [number, number][], padding: number = 50): void {
    if (!this.map || coordinates.length === 0) return;

    const bounds = new mapboxgl.LngLatBounds();
    coordinates.forEach(([lon, lat]) => {
      bounds.extend([lon, lat]);
    });

    this.map.fitBounds(bounds, {
      padding: padding,
      duration: 1000
    });
  }

  /**
   * Capturer un screenshot de la carte
   */
  async captureScreenshot(): Promise<string> {
    if (!this.map) {
      throw new Error('La carte n\'est pas initialisée');
    }

    return new Promise((resolve, reject) => {
      this.map!.getCanvas().toBlob((blob) => {
        if (!blob) {
          reject(new Error('Impossible de capturer la carte'));
          return;
        }
        const reader = new FileReader();
        reader.onloadend = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      }, 'image/png');
    });
  }

  /**
   * Geocoder une adresse via l'API Mapbox
   * @param query Adresse ou lieu à rechercher (français, arabe, anglais)
   * @param countryCode Code pays optionnel (TN par défaut pour Tunisie)
   */
  geocode(query: string, countryCode?: string): Promise<GeocodingResult[]> {
    if (!environment.mapboxToken || !query?.trim()) {
      return Promise.resolve([]);
    }
    const encoded = encodeURIComponent(query.trim());
    const country = countryCode ? `&country=${countryCode}` : '&country=TN';
    const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encoded}.json?access_token=${environment.mapboxToken}${country}&language=fr&limit=5`;
    return fetch(url)
      .then(res => {
        if (!res.ok) throw new Error('Erreur API Mapbox');
        return res.json();
      })
      .then(data => (data.features || []).map((f: any) => ({
        placeName: f.place_name,
        center: f.center as [number, number],
        bbox: f.bbox
      })))
      .catch(() => []);
  }

  /**
   * Obtenir la position GPS de l'utilisateur et centrer la carte
   */
  locateUser(): Promise<[number, number]> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Géolocalisation non supportée'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lng = pos.coords.longitude;
          const lat = pos.coords.latitude;
          this.flyTo([lng, lat], 15);
          resolve([lng, lat]);
        },
        (err) => {
          const msg = err.code === 1 ? 'Position refusée' :
            err.code === 2 ? 'Position indisponible' : 'Timeout';
          reject(new Error(msg));
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
      );
    });
  }

  /**
   * Aller à une position sur la carte
   */
  flyTo(center: [number, number], zoom?: number): void {
    if (!this.map) return;
    this.map.flyTo({ center, zoom: zoom ?? this.map.getZoom(), duration: 1500 });
  }

  /**
   * Zoom avant/arrière
   */
  zoomIn(): void {
    this.map?.zoomIn();
  }

  zoomOut(): void {
    this.map?.zoomOut();
  }

  /**
   * Nettoyer les ressources
   */
  destroy(): void {
    this.clearMarkers();
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }
}
