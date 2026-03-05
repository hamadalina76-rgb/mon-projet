// src/app/shared/components/map-location-selector/map-location-selector.component.ts
import { Component, OnInit, OnDestroy, AfterViewInit, Output, EventEmitter, Input, signal, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import * as mapboxgl from 'mapbox-gl';
import MapboxLanguage from '@mapbox/mapbox-gl-language';
import { environment } from '@environments/environment';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';

export interface LocationData {
  latitude: number;
  longitude: number;
  address?: string;
  city?: string;
  postalCode?: string;
  state?: string;
  country?: string;
  neighborhood?: string;
}

export interface SearchSuggestion {
  placeName: string;
  text: string;
  center: [number, number];
  placeType: string[];
}

@Component({
  selector: 'app-map-location-selector',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule
  ],
  templateUrl: './map-location-selector.component.html',
  styleUrls: ['./map-location-selector.component.scss']
})
export class MapLocationSelectorComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() initialLatitude?: number;
  @Input() initialLongitude?: number;
  @Output() locationSelected = new EventEmitter<LocationData>();

  map?: mapboxgl.Map;
  marker?: mapboxgl.Marker;
  loading = signal(false);
  searchQuery = signal('');
  isMapReady = signal(false);
  searchResults = signal<SearchSuggestion[]>([]);
  showSuggestions = signal(false);

  private mapboxToken = environment.mapboxToken;
  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  // Default location: Tunis, Tunisia
  private defaultLat = 36.8065;
  private defaultLng = 10.1815;

  constructor() {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      filter((q: string) => q.length >= 2),
    ).subscribe((query: string) => this.fetchSuggestions(query));
  }

  ngOnInit(): void {
    // Empty - map initialization moved to ngAfterViewInit
  }

  ngAfterViewInit(): void {
    // Initialize map after view is ready
    setTimeout(() => {
      this.initMap();
    }, 100);
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    // Check if container exists
    const container = document.getElementById('map');
    if (!container) {
      console.error('Map container not found. Retrying...');
      setTimeout(() => this.initMap(), 100);
      return;
    }

    this.loading.set(true);

    const hasInitial = typeof this.initialLatitude === 'number' && typeof this.initialLongitude === 'number';
    const lat = hasInitial ? this.initialLatitude! : this.defaultLat;
    const lng = hasInitial ? this.initialLongitude! : this.defaultLng;

    try {
      // Set Mapbox access token using Object.assign to avoid immutability error
      Object.assign(mapboxgl, { accessToken: this.mapboxToken });

      // Initialize map
      this.map = new mapboxgl.Map({
        container: 'map',
        style: 'mapbox://styles/mapbox/streets-v11',
        center: [lng, lat],
        zoom: 13,
        accessToken: this.mapboxToken
      });

      // Force French labels on map (independent of app language)
      this.map.addControl(new MapboxLanguage({ defaultLanguage: 'fr' }));

      // Add navigation controls
      this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');

      // Add geolocate control
      const geolocate = new mapboxgl.GeolocateControl({
        positionOptions: {
          enableHighAccuracy: true
        },
        trackUserLocation: true,
        showUserHeading: true
      });
      this.map.addControl(geolocate, 'top-right');

      // Initialize marker
      this.marker = new mapboxgl.Marker({
        draggable: true,
        color: '#EF4444'
      })
        .setLngLat([lng, lat])
        .addTo(this.map);

      // Handle marker drag end
      this.marker.on('dragend', () => {
        const lngLat = this.marker!.getLngLat();
        this.onLocationChange(lngLat.lat, lngLat.lng);
      });

      // Handle map click
      this.map.on('click', (e) => {
        this.marker!.setLngLat(e.lngLat);
        this.onLocationChange(e.lngLat.lat, e.lngLat.lng);
      });

      // Map loaded
      this.map.on('load', () => {
        this.loading.set(false);
        this.isMapReady.set(true);
        // Emit initial location
        this.onLocationChange(lat, lng);
      });

      // Trigger geolocate on load only when no initial position from backend
      const hasInitialPos = typeof this.initialLatitude === 'number' && typeof this.initialLongitude === 'number';
      if (!hasInitialPos) {
        this.map.on('load', () => {
          geolocate.trigger();
        });
      }

    } catch (error) {
      console.error('Error initializing map:', error);
      this.loading.set(false);
    }
  }

  private async onLocationChange(lat: number, lng: number): Promise<void> {
    try {
      // Use OpenStreetMap Nominatim for reverse geocoding — far better street-level coverage in Tunisia
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2` +
        `&lat=${lat}&lon=${lng}&accept-language=fr&addressdetails=1&zoom=18`
      );
      const data = await response.json();

      if (!data || data.error) {
        this.locationSelected.emit({ latitude: lat, longitude: lng });
        return;
      }

      const addr = data.address || {};

      // Build street address: house_number + road
      const houseNumber = addr.house_number || '';
      const road = addr.road || addr.pedestrian || addr.footway || addr.path || '';
      let streetAddress = [houseNumber, road].filter(Boolean).join(' ');

      // If no road, try amenity/building/shop name
      if (!streetAddress) {
        streetAddress = addr.amenity || addr.building || addr.shop || addr.tourism || addr.leisure || '';
      }

      const neighborhood = addr.neighbourhood || addr.suburb || addr.quarter || '';
      const city = addr.city || addr.town || addr.village || addr.municipality || '';
      const postalCode = addr.postcode || '';
      const state = addr.state || addr.governorate || addr.province || '';
      const country = addr.country || '';

      // Build display address: street + neighborhood
      const parts = [streetAddress, neighborhood].filter(Boolean);
      const displayAddress = parts.join(', ');

      this.locationSelected.emit({
        latitude: lat,
        longitude: lng,
        address: displayAddress || undefined,
        city: city || undefined,
        postalCode: postalCode || undefined,
        state: state || undefined,
        country: country || undefined,
        neighborhood: neighborhood || undefined,
      });
    } catch (error) {
      console.error('Reverse geocoding error:', error);
      this.locationSelected.emit({ latitude: lat, longitude: lng });
    }
  }

  async searchLocation(): Promise<void> {
    const query = this.searchQuery();
    if (!query || !this.map) return;

    this.loading.set(true);
    this.showSuggestions.set(false);

    try {
      const center = this.map.getCenter();
      const viewbox = this.getViewbox();
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=jsonv2` +
        `&q=${encodeURIComponent(query)}` +
        `&accept-language=fr` +
        `&limit=1` +
        `&addressdetails=1` +
        `&viewbox=${viewbox}` +
        `&bounded=0`
      );
      const data = await response.json();

      if (Array.isArray(data) && data.length > 0) {
        const place = data[0];
        const lat = parseFloat(place.lat);
        const lng = parseFloat(place.lon);

        this.map.flyTo({
          center: [lng, lat],
          zoom: 16
        });

        this.marker!.setLngLat([lng, lat]);
        this.onLocationChange(lat, lng);
      }
    } catch (error) {
      console.error('Error searching location:', error);
    } finally {
      this.loading.set(false);
    }
  }

  /** Called on each keystroke in the search input → debounced autocomplete */
  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    if (value.length < 2) {
      this.searchResults.set([]);
      this.showSuggestions.set(false);
      return;
    }
    this.searchSubject.next(value);
  }

  /** Fetch autocomplete suggestions from Nominatim (OpenStreetMap) */
  private async fetchSuggestions(query: string): Promise<void> {
    try {
      const viewbox = this.getViewbox();
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=jsonv2` +
        `&q=${encodeURIComponent(query)}` +
        `&accept-language=fr` +
        `&limit=5` +
        `&addressdetails=1` +
        `&viewbox=${viewbox}` +
        `&bounded=0`
      );
      const data = await response.json();

      if (Array.isArray(data) && data.length > 0) {
        this.searchResults.set(
          data.map((place: any) => {
            const addr = place.address || {};
            const road = addr.road || addr.pedestrian || '';
            const city = addr.city || addr.town || addr.village || '';
            const mainText = road || place.name || city || place.display_name?.split(',')[0] || '';
            return {
              placeName: place.display_name || '',
              text: mainText,
              center: [parseFloat(place.lon), parseFloat(place.lat)] as [number, number],
              placeType: [place.type || place.category || 'place'],
            };
          })
        );
        this.showSuggestions.set(true);
      } else {
        this.searchResults.set([]);
        this.showSuggestions.set(false);
      }
    } catch (error) {
      console.error('Error fetching suggestions:', error);
    }
  }

  /** Select a suggestion from the autocomplete dropdown */
  selectSuggestion(suggestion: SearchSuggestion): void {
    const [lng, lat] = suggestion.center;
    this.searchQuery.set(suggestion.placeName);
    this.showSuggestions.set(false);
    this.searchResults.set([]);

    if (this.map && this.marker) {
      this.map.flyTo({ center: [lng, lat], zoom: 16 });
      this.marker.setLngLat([lng, lat]);
      this.onLocationChange(lat, lng);
    }
  }

  /** Hide suggestions dropdown (with delay to allow click) */
  hideSuggestions(): void {
    setTimeout(() => this.showSuggestions.set(false), 200);
  }

  /** Build Nominatim viewbox from current map bounds for proximity bias */
  private getViewbox(): string {
    if (this.map) {
      const bounds = this.map.getBounds();
      if (bounds) {
        return `${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()},${bounds.getSouth()}`;
      }
    }
    // Fallback: Tunisia bounding box
    return '7.5,37.5,11.6,30.2';
  }

  getCurrentLocation(): void {
    if (!navigator.geolocation) {
      alert('La géolocalisation n\'est pas supportée par votre navigateur');
      return;
    }

    this.loading.set(true);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        if (this.map && this.marker) {
          this.map.flyTo({
            center: [lng, lat],
            zoom: 15
          });

          this.marker.setLngLat([lng, lat]);
          this.onLocationChange(lat, lng);
        }

        this.loading.set(false);
      },
      (error) => {
        console.error('Error getting location:', error);
        alert('Impossible d\'obtenir votre position');
        this.loading.set(false);
      }
    );
  }
}
