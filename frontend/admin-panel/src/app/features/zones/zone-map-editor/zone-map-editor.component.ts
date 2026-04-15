import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  inject,
  ViewChild,
  ElementRef,
  AfterViewInit,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogData,
} from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { ToastrService } from 'ngx-toastr';
import { MapboxService } from '../services/mapbox.service';
import { GeometryService } from '../services/geometry.service';
import { ZonesService } from '../services/zones.service';
import {
  Zone,
  ZoneType,
  ZoneCreateRequest,
  ZoneUpdateRequest,
} from '../models/zone.model';
import { Subject, takeUntil } from 'rxjs';
import mapboxgl from 'mapbox-gl';
import * as turf from '@turf/turf';
import 'mapbox-gl/dist/mapbox-gl.css';

@Component({
  selector: 'app-zone-map-editor',
  host: { '[attr.dir]': 'isRtl() ? "rtl" : "ltr"' },
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './zone-map-editor.component.html',
  styleUrls: ['./zone-map-editor.component.scss'],
})
export class ZoneMapEditorComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef<HTMLDivElement>;

  mapboxService = inject(MapboxService);
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toastr = inject(ToastrService);
  private geometryService = inject(GeometryService);
  private zonesService = inject(ZonesService);
  private translate = inject(TranslateService);
  private dialog = inject(MatDialog);

  loading = signal(false);
  saving = signal(false);
  isEditMode = signal(false);
  drawingMode = signal(false);
  selectedZone = signal<Zone | null>(null);
  zones = signal<Zone[]>([]);

  zoneSearch = '';
  zoneActiveFilter: 'all' | 'active' | 'inactive' = 'all';
  private searchDebounceRef: ReturnType<typeof setTimeout> | null = null;
  geocodeQuery = '';
  collapsedZoneIds = signal<Set<number>>(new Set());
  sidebarCollapsed = signal(false);
  rightSidebarCollapsed = signal(false);
  geocodeLoading = signal(false);
  polygonCoordinates = signal<[number, number][]>([]);
  private onMapMouseMoveBound: ((e: any) => void) | null = null;
  private destroy$ = new Subject<void>();
  validationError = signal<string | null>(null);
  overlapWarning = signal<string | null>(null);
  selfIntersectWarning = signal(false);  // live: preview line would cross an edge
  polygonHasKinks = signal(false);        // persistent: placed polygon has actual kinks
  nameExistsError = signal(false);

  zoneForm: FormGroup = this.fb.group({
    name: ['', [Validators.required]],
    city: ['Tunis', Validators.required],
    type: [ZoneType.DELIVERY, Validators.required],
    description: [''],
    deliveryFee: [0, [Validators.required, Validators.min(0), Validators.max(999.999)]],
    serviceFee: [0, [Validators.min(0), Validators.max(999.999)]],
    minDeliveryTime: [30],
    maxDeliveryTime: [60],
    radiusKm: [null, [Validators.min(0.1), Validators.max(500)]],
    isActive: [true],
  });

  zoneAreaKm2 = computed(() => {
    const coords = this.polygonCoordinates();
    if (coords.length < 3) return 0;
    return this.geometryService.calculateArea(coords);
  });

  /** RTL pour l'arabe : inverser les chevrons des panneaux */
  currentLang = signal(this.translate.currentLang || 'fr');
  isRtl = computed(() => this.currentLang() === 'ar');
  chevronLeftExpand = computed(() => (this.isRtl() ? 'chevron_left' : 'chevron_right'));
  chevronLeftCollapse = computed(() => (this.isRtl() ? 'chevron_right' : 'chevron_left'));
  chevronRightExpand = computed(() => (this.isRtl() ? 'chevron_right' : 'chevron_left'));
  chevronRightCollapse = computed(() => (this.isRtl() ? 'chevron_left' : 'chevron_right'));
  /** Icône compacte pour "retour à la liste" */
  backIcon = 'arrow_back';

  ZoneType = ZoneType;

  /** Les 24 gouvernorats de Tunisie */
  tunisianCities = [
    'Ariana', 'Béja', 'Ben Arous', 'Bizerte', 'Gabès', 'Gafsa', 'Jendouba',
    'Kairouan', 'Kasserine', 'Kébili', 'Le Kef', 'Mahdia', 'La Manouba',
    'Médenine', 'Monastir', 'Nabeul', 'Sfax', 'Sidi Bouzid', 'Siliana',
    'Sousse', 'Tataouine', 'Tozeur', 'Tunis', 'Zaghouan'
  ];

  ngOnInit(): void {
    this.currentLang.set(this.translate.currentLang || 'fr');
    this.translate.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((event) => this.currentLang.set(event.lang));

    // Réinitialiser l'erreur "nom existe" quand le nom change
    this.zoneForm.get('name')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.nameExistsError.set(false);
    });

    const zoneId = this.route.snapshot.paramMap.get('id');
    if (zoneId) {
      this.isEditMode.set(true);
      this.loadZone(Number(zoneId));
    }
    this.loadZones();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.mapContainer) {
        this.mapContainer.nativeElement.id = 'map-container-' + Date.now();
        this.initializeMap();
      }
    }, 100);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.searchDebounceRef) clearTimeout(this.searchDebounceRef);
    this.mapboxService.destroy();
  }

  private initializeMap(): void {
    if (!this.mapContainer) return;
    try {
      const containerId =
        this.mapContainer.nativeElement.id || 'map-container-' + Date.now();
      this.mapContainer.nativeElement.id = containerId;
      this.mapboxService.initializeMap(containerId, {
        center: [10.1815, 36.8065],
        zoom: 12,
      });
      const map = this.mapboxService.getMap();
      if (map) {
        const boundZoneClick = this.onZoneMapClick.bind(this);
        map.on('click', (e: { point: { x: number; y: number } }) => {
          if (!this.drawingMode()) boundZoneClick(e);
        });
        map.on('load', () => {
          this.loadZonesOnMap();
          this.setupDrawingListeners();
          setTimeout(() => this.mapboxService.resize(), 200);
        });
        if (map.loaded()) {
          this.loadZonesOnMap();
          this.setupDrawingListeners();
          setTimeout(() => this.mapboxService.resize(), 200);
        }
      }
    } catch (error) {
      console.error('Erreur lors de l\'initialisation de la carte', error);
      this.toastr.error('Impossible d\'initialiser la carte Mapbox');
    }
  }

  private setupDrawingListeners(): void {
    const map = this.mapboxService.getMap();
    if (!map) return;
    if (this.drawingMode()) {
      this.onMapMouseMoveBound = this.onMapMouseMove.bind(this);
      map.on('click',     this.onMapClick.bind(this));
      map.on('dblclick',  this.onMapDoubleClick.bind(this));
      map.on('mousemove', this.onMapMouseMoveBound);
      map.doubleClickZoom.disable();
      map.getCanvas().style.cursor = 'crosshair';
    } else {
      map.off('click',    this.onMapClick.bind(this));
      map.off('dblclick', this.onMapDoubleClick.bind(this));
      if (this.onMapMouseMoveBound) {
        map.off('mousemove', this.onMapMouseMoveBound);
        this.onMapMouseMoveBound = null;
      }
      map.doubleClickZoom.enable();
      map.getCanvas().style.cursor = '';
      this.clearPreviewLine();
    }
  }

  getDisplayedZones(): Zone[] {
    return this.zones();
  }

  setActiveFilter(filter: 'all' | 'active' | 'inactive'): void {
    this.zoneActiveFilter = filter;
    this.loadZones();
  }

  onSearchChange(): void {
    if (this.searchDebounceRef) clearTimeout(this.searchDebounceRef);
    this.searchDebounceRef = setTimeout(() => {
      this.loadZones();
      this.searchDebounceRef = null;
    }, 350);
  }

  getZoneTypeLabel(type: ZoneType): string {
    const labels: Record<ZoneType, string> = {
      [ZoneType.DELIVERY]: 'Standard',
      [ZoneType.PREMIUM]: 'Premium',
      [ZoneType.RESTRICTED]: 'Restricted',
      [ZoneType.EXPRESS]: 'Express',
    };
    return labels[type] || type;
  }

  toggleZoneActive(event: Event, zone: Zone): void {
    event.stopPropagation();
    const newActive = !zone.isActive;
    this.zonesService
      .setActiveStatus(zone.id, newActive)
      .subscribe({
        next: (updated) => {
          this.zones.update((list) =>
            list.map((z) => (z.id === zone.id ? { ...z, isActive: updated.isActive } : z))
          );
          this.toastr.success(newActive ? 'Zone activée' : 'Zone désactivée');
          if (this.mapboxService.getMap()?.loaded()) {
            if (!newActive) {
              this.mapboxService.removeZoneFromMap(zone.id);
              if (this.selectedZone()?.id === zone.id) {
                this.polygonCoordinates.set([]);
                this.mapboxService.removeLayer('current-polygon-line');
                this.mapboxService.removeLayer('current-polygon-layer');
                this.mapboxService.removeSource('current-polygon');
              }
            }
            this.loadZonesOnMap();
          }
        },
        error: () => this.toastr.error('Erreur lors de la mise à jour'),
      });
  }

  onGeocodeSearch(): void {
    const query = this.geocodeQuery?.trim();
    if (!query) {
      this.toastr.warning('Saisissez une adresse à rechercher');
      return;
    }
    this.geocodeLoading.set(true);
    this.mapboxService.geocode(query).then((results) => {
      this.geocodeLoading.set(false);
      if (results.length > 0) {
        this.mapboxService.flyTo(results[0].center, 15);
        this.toastr.success(`Trouvé: ${results[0].placeName}`);
      } else {
        this.toastr.warning('Aucun résultat. Essayez une autre adresse ou un lieu en Tunisie.');
      }
    }).catch(() => {
      this.geocodeLoading.set(false);
      this.toastr.error('Erreur lors de la recherche');
    });
  }

  onGeolocate(): void {
    this.mapboxService.locateUser().then(() => {
      this.toastr.success('Position trouvée');
    }).catch((err: Error) => {
      this.toastr.error(err.message || 'Impossible d\'obtenir votre position');
    });
  }

  onClosePanel(): void {
    this.router.navigate(['/zones']);
  }

  private loadZones(): void {
    this.loading.set(true);
    const search = this.zoneSearch?.trim() || undefined;
    const isActive = this.zoneActiveFilter === 'all' ? undefined : this.zoneActiveFilter === 'active';
    this.zonesService.getZones(0, 200, search, isActive).subscribe({
      next: (res) => {
        this.zones.set(res.content || []);
        this.loading.set(false);
        if (this.mapboxService.getMap()?.loaded()) {
          this.loadZonesOnMap();
        }
      },
      error: () => {
        this.loading.set(false);
        this.toastr.error('Erreur lors du chargement des zones');
      },
    });
  }

  private loadZone(id: number): void {
    this.loading.set(true);
    this.zonesService.getZone(id).subscribe({
      next: (zone) => {
        this.selectedZone.set(zone);
        this.zoneForm.patchValue({
          name: zone.name,
          city: zone.city,
          type: zone.type,
          description: zone.description,
          deliveryFee: zone.deliveryFee,
          serviceFee: zone.serviceFee ?? 0,
          minDeliveryTime: zone.minDeliveryTime,
          maxDeliveryTime: zone.maxDeliveryTime,
          radiusKm: zone.radiusKm ?? null,
          isActive: zone.isActive,
        });
        if (zone.boundaryJson) {
          const coords =
            this.geometryService.parseBoundaryJson(zone.boundaryJson);
          this.polygonCoordinates.set(coords);
          this.checkOverlaps(coords);
          setTimeout(() => this.updatePolygonOnMap(), 100);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastr.error('Erreur lors du chargement de la zone');
      },
    });
  }

  private checkOverlaps(coords: [number, number][]): void {
    if (coords.length < 3) {
      this.overlapWarning.set(null);
      return;
    }
    const zones = this.zones();
    const currentId = this.selectedZone()?.id;
    for (const z of zones) {
      if (z.id === currentId || !z.boundaryJson) continue;
      const zCoords = this.geometryService.parseBoundaryJson(z.boundaryJson);
      if (this.geometryService.checkOverlap(coords, zCoords)) {
        this.overlapWarning.set(
          `This zone overlaps with '${z.name}'. This might cause fee calculation conflicts.`
        );
        return;
      }
    }
    this.overlapWarning.set(null);
  }

  private loadZonesOnMap(): void {
    this.mapboxService.clearAllZoneLayers();
    const zones = this.zones().filter((z) => z.isActive);
    zones.forEach((zone) => {
      if (!zone.boundaryJson) return;
      const coords = this.geometryService.parseBoundaryJson(zone.boundaryJson);
      if (coords.length < 3) return;
      const mapboxCoords =
        this.geometryService.convertToMapboxCoordinates(coords);
      const color = this.getZoneColor(zone.type);
      const closed =
        mapboxCoords.length > 0 &&
        (mapboxCoords[0][0] !== mapboxCoords[mapboxCoords.length - 1][0] ||
          mapboxCoords[0][1] !== mapboxCoords[mapboxCoords.length - 1][1])
          ? [...mapboxCoords, mapboxCoords[0]]
          : mapboxCoords;
      const geojson = {
        type: 'Feature',
        properties: { zoneId: zone.id },
        geometry: { type: 'Polygon', coordinates: [closed] },
      };
      const sid = `zone-${zone.id}`;
      const lid = `zone-layer-${zone.id}`;
      const lineId = `zone-line-${zone.id}`;
      this.mapboxService.addGeoJSONSource(sid, geojson);
      this.mapboxService.addPolygonLayer(lid, sid, {
        'fill-color': color,
        'fill-opacity': zone.isActive ? 0.35 : 0.12,
        'fill-outline-color': color,
      });
      this.mapboxService.addLineLayer(lineId, sid, {
        'line-color': color,
        'line-width': zone.isActive ? 3 : 1.5,
      });
      const center = this.geometryService.calculateCenter(coords);
      const mapboxCenter: [number, number] = [center[1], center[0]];
      this.mapboxService.addZoneLabel(sid, mapboxCenter, zone.name, '#0f172a');
    });
  }

  private onZoneMapClick(event: { point: { x: number; y: number } }): void {
    if (this.drawingMode()) return;
    const map = this.mapboxService.getMap();
    if (!map) return;
    const zones = this.zones();
    const layerIds = zones
      .filter((z) => z.boundaryJson && z.isActive)
      .map((z) => `zone-layer-${z.id}`);
    const point: [number, number] = [event.point.x, event.point.y];
    const features = map.queryRenderedFeatures(point, { layers: layerIds });
    const feature = features.find((f) => f.properties?.['zoneId']);
    if (feature) {
      const zoneId = feature.properties!['zoneId'] as number;
      const zone = zones.find((z) => z.id === zoneId);
      if (zone) this.selectZone(zone);
    }
  }

  private getZoneColor(type: ZoneType): string {
    const colors: Record<ZoneType, string> = {
      [ZoneType.DELIVERY]: '#3b82f6',
      [ZoneType.PREMIUM]: '#E31E24',
      [ZoneType.RESTRICTED]: '#94a3b8',
      [ZoneType.EXPRESS]: '#3b82f6',
    };
    return colors[type] || '#3b82f6';
  }

  toggleDrawingMode(): void {
    const newMode = !this.drawingMode();
    this.drawingMode.set(newMode);
    this.validationError.set(null);
    this.setupDrawingListeners();
  }

  onMapClick(event: any): void {
    if (!this.drawingMode()) return;

    // Backend format: [lat, lon]
    const point: [number, number] = [event.lngLat.lat, event.lngLat.lng];
    const currentCoords = this.polygonCoordinates();
    currentCoords.push(point);
    this.polygonCoordinates.set([...currentCoords]);

    if (currentCoords.length >= 3) {
      const v = this.geometryService.validatePolygon(currentCoords);
      this.validationError.set(v.valid ? null : (v.error || ''));
      this.checkOverlaps(currentCoords);
      this.polygonHasKinks.set(this.checkKinks(currentCoords));
    }
    this.updatePolygonOnMap();
  }

  /**
   * Vérifie si le polygone placé contient des auto-intersections réelles via turf.kinks().
   * Contrairement à selfIntersectWarning (live preview), cette vérification est persistante.
   */
  private checkKinks(coords: [number, number][]): boolean {
    if (coords.length < 3) return false;
    const mapboxCoords = this.geometryService.convertToMapboxCoordinates(coords);
    const closed =
      mapboxCoords[0][0] !== mapboxCoords[mapboxCoords.length - 1][0] ||
      mapboxCoords[0][1] !== mapboxCoords[mapboxCoords.length - 1][1]
        ? [...mapboxCoords, mapboxCoords[0]]
        : [...mapboxCoords];
    if (closed.length < 4) return false;
    try {
      return turf.kinks(turf.polygon([closed])).features.length > 0;
    } catch {
      return false;
    }
  }

  /** Mouvement souris : met à jour la ligne de prévisualisation + détection intersection en temps réel */
  onMapMouseMove(event: any): void {
    if (!this.drawingMode()) return;
    const coords = this.polygonCoordinates();
    if (coords.length === 0) return;
    const last = coords[coords.length - 1];
    // last is [lat, lon] → Mapbox needs [lon, lat]
    const from: [number, number] = [last[1], last[0]];
    const to: [number, number] = [event.lngLat.lng, event.lngLat.lat];
    const intersects = coords.length >= 2 && this.wouldSelfIntersect(coords, to);
    this.selfIntersectWarning.set(intersects);
    this.updatePreviewLine(from, to, intersects);
  }

  /**
   * Vérifie si le segment (dernier point → curseur) couperait
   * l'un des segments existants du polygone en cours de dessin.
   */
  private wouldSelfIntersect(coords: [number, number][], cursor: [number, number]): boolean {
    // coords are [lat, lon] ; cursor is [lon, lat] (Mapbox)
    const last = coords[coords.length - 1];
    const newSeg = turf.lineString([[last[1], last[0]], cursor]);
    // Check all edges except the one that ends at 'last' (index coords.length-2 → coords.length-1)
    for (let i = 0; i < coords.length - 2; i++) {
      const edge = turf.lineString([[coords[i][1], coords[i][0]], [coords[i + 1][1], coords[i + 1][0]]]);
      if (turf.lineIntersect(newSeg, edge).features.length > 0) return true;
    }
    return false;
  }

  private updatePreviewLine(from: [number, number], to: [number, number], intersects = false): void {
    const map = this.mapboxService.getMap();
    if (!map) return;
    const lineColor = intersects ? '#f97316' : '#E31E24';
    const geojson: any = {
      type: 'Feature',
      geometry: { type: 'LineString', coordinates: [from, to] },
    };
    if (map.getSource('preview-line')) {
      (map.getSource('preview-line') as mapboxgl.GeoJSONSource).setData(geojson);
      if (map.getLayer('preview-line-layer')) {
        map.setPaintProperty('preview-line-layer', 'line-color', lineColor);
      }
    } else {
      map.addSource('preview-line', { type: 'geojson', data: geojson });
      map.addLayer({
        id: 'preview-line-layer',
        type: 'line',
        source: 'preview-line',
        paint: {
          'line-color': lineColor,
          'line-width': 1.5,
          'line-dasharray': [4, 3],
        },
      });
    }
  }

  private clearPreviewLine(): void {
    const map = this.mapboxService.getMap();
    if (!map) return;
    if (map.getLayer('preview-line-layer')) map.removeLayer('preview-line-layer');
    if (map.getSource('preview-line')) map.removeSource('preview-line');
    this.selfIntersectWarning.set(false);
  }

  /** Double-clic : ferme le polygone et désactive le mode dessin */
  onMapDoubleClick(event: any): void {
    event.preventDefault?.();
    const coords = this.polygonCoordinates();
    if (coords.length < 3) {
      this.toastr.warning(this.translate.instant('zones.editor.minPolygon'));
      return;
    }
    this.drawingMode.set(false);
    this.setupDrawingListeners();
    this.toastr.success(this.translate.instant('zones.editor.polygonClosed', { count: coords.length }));
  }

  private updatePolygonOnMap(): void {
    const coords = this.polygonCoordinates();
    if (coords.length === 0 || !this.mapboxService.getMap()) return;
    const mapboxCoords =
      this.geometryService.convertToMapboxCoordinates(coords);
    const closed =
      mapboxCoords.length > 0 &&
      (mapboxCoords[0][0] !== mapboxCoords[mapboxCoords.length - 1][0] ||
        mapboxCoords[0][1] !== mapboxCoords[mapboxCoords.length - 1][1])
        ? [...mapboxCoords, mapboxCoords[0]]
        : mapboxCoords;
    const geojson = {
      type: 'Feature',
      geometry: { type: 'Polygon', coordinates: [closed] },
    };
    const sid = 'current-polygon';
    const lid = 'current-polygon-layer';
    const lineId = 'current-polygon-line';
    this.mapboxService.addGeoJSONSource(sid, geojson);
    if (!this.mapboxService.getMap()?.getLayer(lid)) {
      this.mapboxService.addPolygonLayer(lid, sid, {
        'fill-color': '#E31E24',
        'fill-opacity': 0.3,
      });
      this.mapboxService.addLineLayer(lineId, sid, {
        'line-color': '#C81E1E',
        'line-width': 2,
      });
    }
  }

  clearPolygon(): void {
    this.polygonCoordinates.set([]);
    this.validationError.set(null);
    this.overlapWarning.set(null);
    this.selfIntersectWarning.set(false);
    this.polygonHasKinks.set(false);
    this.mapboxService.removeLayer('current-polygon-line');
    this.mapboxService.removeLayer('current-polygon-layer');
    this.mapboxService.removeSource('current-polygon');
    this.clearPreviewLine();
  }

  removeLastPoint(): void {
    const coords = this.polygonCoordinates();
    if (coords.length === 0) return;
    const next = coords.slice(0, -1);
    this.polygonCoordinates.set(next);
    if (next.length < 3) {
      this.validationError.set(null);
      this.overlapWarning.set(null);
      this.polygonHasKinks.set(false);
    } else {
      const v = this.geometryService.validatePolygon(next);
      this.validationError.set(v.valid ? null : (v.error || ''));
      this.checkOverlaps(next);
      this.polygonHasKinks.set(this.checkKinks(next));
    }
    if (next.length === 0) {
      this.mapboxService.removeLayer('current-polygon-line');
      this.mapboxService.removeLayer('current-polygon-layer');
      this.mapboxService.removeSource('current-polygon');
    } else {
      this.updatePolygonOnMap();
    }
  }

  onSubmit(): void {
    this.nameExistsError.set(false);
    this.validationError.set(null);
    if (this.zoneForm.invalid) {
      this.zoneForm.markAllAsTouched();
      this.toastr.error(this.translate.instant('zones.editor.requiredFields'));
      return;
    }
    const name = (this.zoneForm.get('name')?.value || '').trim();
    const currentId = this.selectedZone()?.id;
    const exists = this.zones().some(
      (z) => z.name?.toLowerCase() === name.toLowerCase() && z.id !== currentId
    );
    if (exists) {
      this.nameExistsError.set(true);
      this.toastr.error(this.translate.instant('zones.editor.nameUnique'));
      return;
    }
    const coordinates = this.polygonCoordinates();
    if (coordinates.length < 3) {
      this.toastr.error(this.translate.instant('zones.editor.minPolygon'));
      return;
    }
    // Bloquer si le polygone placé contient des intersections
    if (this.polygonHasKinks() || this.checkKinks(coordinates)) {
      const msg = this.translate.instant('zones.editor.selfIntersectError');
      this.validationError.set(msg);
      this.toastr.error(msg);
      return;
    }
    const validation = this.geometryService.validatePolygon(coordinates);
    if (!validation.valid) {
      const isSelfIntersect = validation.error?.toLowerCase().includes('auto-intersection')
        || validation.error?.toLowerCase().includes('self-intersect');
      const msg = isSelfIntersect
        ? this.translate.instant('zones.editor.selfIntersectError')
        : (validation.error || this.translate.instant('zones.editor.invalidPolygon'));
      this.validationError.set(msg);
      this.toastr.error(msg);
      return;
    }
    this.saving.set(true);
    const boundaryJson =
      this.geometryService.coordinatesToBoundaryJson(coordinates);
    const payload: ZoneCreateRequest = {
      ...this.zoneForm.value,
      boundaryJson,
    };
    const op = this.isEditMode()
      ? this.zonesService.updateZone(
          this.selectedZone()!.id,
          payload as ZoneUpdateRequest
        )
      : this.zonesService.createZone(payload);
    op.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastr.success(
          this.isEditMode() ? this.translate.instant('zones.editor.saveSuccess') : this.translate.instant('zones.editor.createSuccess')
        );
        this.router.navigate(['/zones']);
      },
      error: (err) => {
        this.saving.set(false);
        const body = err.error;
        // Erreurs de validation par champ renvoyées par le backend
        if (body?.fieldErrors) {
          const fields: Record<string, string> = body.fieldErrors;
          Object.entries(fields).forEach(([field, msg]) => {
            const ctrl = this.zoneForm.get(field);
            if (ctrl) {
              ctrl.setErrors({ serverError: msg });
              ctrl.markAsTouched();
            }
          });
          const summary = Object.values(fields).join(' • ');
          this.validationError.set(summary);
          this.toastr.error(summary, undefined, { timeOut: 6000 });
        } else {
          const errorCode = body?.error as string | undefined;
          const isSelfIntersect = errorCode === 'SELF_INTERSECTION'
            || body?.message?.toLowerCase().includes('intersection');
          const msg = isSelfIntersect
            ? this.translate.instant('zones.editor.selfIntersectError')
            : (body?.message || err.message || this.translate.instant('zones.editor.saveError'));
          this.validationError.set(msg);
          this.toastr.error(msg);
        }
      },
    });
  }

  createNewZone(): void {
    this.selectedZone.set(null);
    this.isEditMode.set(false);
    this.zoneForm.reset({
      name: '',
      city: 'Tunis',
      type: ZoneType.DELIVERY,
      description: '',
      deliveryFee: 0,
      serviceFee: 0,
      minDeliveryTime: 30,
      maxDeliveryTime: 60,
      radiusKm: null,
      isActive: true,
    });
    this.polygonCoordinates.set([]);
    this.validationError.set(null);
    this.overlapWarning.set(null);
    this.clearPolygon();
    this.drawingMode.set(true);
    this.setupDrawingListeners();
  }

  selectZone(zone: Zone): void {
    this.selectedZone.set(zone);
    this.zoneForm.patchValue({
      name: zone.name,
      city: zone.city,
      type: zone.type,
      description: zone.description,
      deliveryFee: zone.deliveryFee,
      serviceFee: zone.serviceFee ?? 0,
      minDeliveryTime: zone.minDeliveryTime,
      maxDeliveryTime: zone.maxDeliveryTime,
      radiusKm: zone.radiusKm,
      isActive: zone.isActive,
    });
        if (zone.boundaryJson) {
          const coords =
            this.geometryService.parseBoundaryJson(zone.boundaryJson);
          this.polygonCoordinates.set(coords);
          this.checkOverlaps(coords);
          this.flyToZone(zone);
          this.updatePolygonOnMap();
        } else {
      this.polygonCoordinates.set([]);
      this.overlapWarning.set(null);
    }
  }

  private flyToZone(zone: Zone): void {
    if (!zone.boundaryJson) return;
    const coords = this.geometryService.parseBoundaryJson(zone.boundaryJson);
    const center = this.geometryService.calculateCenter(coords);
    if (center[0] !== 0 || center[1] !== 0) {
      this.mapboxService.flyTo([center[1], center[0]], 15);
    }
  }

  toggleZoneCardExpand(event: Event, zone: Zone): void {
    event.stopPropagation();
    this.collapsedZoneIds.update((set) => {
      const next = new Set(set);
      if (next.has(zone.id)) next.delete(zone.id);
      else next.add(zone.id);
      return next;
    });
  }

  isZoneCardExpanded(zone: Zone): boolean {
    return !this.collapsedZoneIds().has(zone.id);
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
  }

  toggleRightSidebar(): void {
    this.rightSidebarCollapsed.update((v) => !v);
  }

  onDeleteZone(event: Event, zone: Zone): void {
    event.stopPropagation();
    event.preventDefault();
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('zones.editor.deleteZone'),
        message: this.translate.instant('zones.editor.deleteConfirm', { name: zone.name }),
        confirmLabel: this.translate.instant('common.delete'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'danger',
        icon: 'delete',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.zonesService.deleteZone(zone.id).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('zones.editor.deleteSuccess'));
            this.loadZones();
            if (this.selectedZone()?.id === zone.id) {
              this.createNewZone();
            }
          },
          error: () => this.toastr.error(this.translate.instant('zones.editor.deleteError')),
        });
      }
    });
  }
}
