// src/app/features/dashboard/components/active-users/active-users.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-active-users',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatListModule],
  templateUrl: './active-users.component.html',
  styleUrls: ['./active-users.component.scss'],
})
export class ActiveUsersComponent implements OnInit {
  ngOnInit(): void {
    // TODO: Load active users
  }
}
