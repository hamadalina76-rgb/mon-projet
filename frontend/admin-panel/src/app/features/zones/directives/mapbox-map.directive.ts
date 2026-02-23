import { Directive, ElementRef, Input, OnInit, OnDestroy, inject } from '@angular/core';
import { MapboxService } from '../services/mapbox.service';
import { Map } from 'mapbox-gl';

@Directive({
  selector: '[appMapboxMap]',
  standalone: true
})
export class MapboxMapDirective implements OnInit, OnDestroy {
  @Input() center?: [number, number];
  @Input() zoom?: number;
  @Input() style?: string;

  private mapboxService = inject(MapboxService);
  private map: Map | null = null;

  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    const containerId = `map-${Date.now()}`;
    this.el.nativeElement.id = containerId;

    this.map = this.mapboxService.initializeMap(containerId, {
      center: this.center,
      zoom: this.zoom,
      style: this.style
    });
  }

  ngOnDestroy(): void {
    // Le service Mapbox gère le nettoyage
  }

  getMap(): Map | null {
    return this.map;
  }
}
