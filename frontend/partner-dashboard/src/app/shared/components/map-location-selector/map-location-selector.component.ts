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

  private mapboxToken = environment.mapboxToken;

  // Default location: Tunis, Tunisia
  private defaultLat = 36.8065;
  private defaultLng = 10.1815;

  constructor() {
    // Mapbox token will be set in initMap
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
        style: 'mapbox://styles/mapbox/streets-v12',
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
    // Reverse geocode to get address
    try {
      const response = await fetch(
        `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?access_token=${this.mapboxToken}&language=fr&types=address,poi`
      );
      const data = await response.json();

      if (data.features && data.features.length > 0) {
        const place = data.features[0];
        
        // Extract address components from context
        let streetAddress = '';
        let city = '';
        let postalCode = '';
        let state = '';
        let country = '';
        let neighborhood = '';

        // The main place text is usually the street/POI name
        if (place.place_type?.includes('address')) {
          // For address type: text = street name, address = house number
          streetAddress = place.address 
            ? `${place.address} ${place.text}` 
            : place.text;
        } else if (place.place_type?.includes('poi')) {
          streetAddress = place.text;
        } else {
          streetAddress = place.text || place.place_name;
        }

        // Parse context for city, state, postcode, country
        place.context?.forEach((ctx: any) => {
          const id = ctx.id || '';
          if (id.startsWith('neighborhood') || id.startsWith('locality')) {
            neighborhood = ctx.text;
          } else if (id.startsWith('place')) {
            city = ctx.text;
          } else if (id.startsWith('district')) {
            // Fallback for city if place is not available
            if (!city) city = ctx.text;
          } else if (id.startsWith('region')) {
            state = ctx.text;
          } else if (id.startsWith('postcode')) {
            postalCode = ctx.text;
          } else if (id.startsWith('country')) {
            country = ctx.text;
          }
        });

        // If no city found, try from place_type
        if (!city && place.place_type?.includes('place')) {
          city = place.text;
        }

        // Build a clean full address
        const addressParts = [streetAddress];
        if (neighborhood) addressParts.push(neighborhood);
        const fullAddress = addressParts.join(', ');

        const locationData: LocationData = {
          latitude: lat,
          longitude: lng,
          address: fullAddress || place.place_name,
          city,
          postalCode,
          state,
          country,
          neighborhood,
        };

        this.locationSelected.emit(locationData);
      } else {
        // Emit location without address details
        this.locationSelected.emit({
          latitude: lat,
          longitude: lng
        });
      }
    } catch (error) {
      console.error('Error geocoding:', error);
      // Emit location without address details
      this.locationSelected.emit({
        latitude: lat,
        longitude: lng
      });
    }
  }

  async searchLocation(): Promise<void> {
    const query = this.searchQuery();
    if (!query || !this.map) return;

    this.loading.set(true);

    try {
      const response = await fetch(
        `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?access_token=${this.mapboxToken}&limit=1`
      );
      const data = await response.json();

      if (data.features && data.features.length > 0) {
        const place = data.features[0];
        const [lng, lat] = place.center;

        // Move map and marker
        this.map.flyTo({
          center: [lng, lat],
          zoom: 15
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
