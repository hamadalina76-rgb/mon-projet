// src/app/features/zones/zones-map/zones-map.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslateModule } from '@ngx-translate/core';
import { ZonesService } from '../services/zones.service';

@Component({
  selector: 'app-zones-map',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    TranslateModule,
  ],
  templateUrl: './zones-map.component.html',
  styleUrls: ['./zones-map.component.scss'],
})
export class ZonesMapComponent implements OnInit {
  private zonesService = inject(ZonesService);

  zones: any[] = [];
  selectedZone: any = null;
  loading = false;

  ngOnInit(): void {
    this.loadZones();
  }

  loadZones(): void {
    // TODO: Implement
  }

  selectZone(zone: any): void {
    this.selectedZone = zone;
  }
}
