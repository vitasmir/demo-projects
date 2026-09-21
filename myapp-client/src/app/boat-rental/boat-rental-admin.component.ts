import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';
import { BoatRentalAdminOverview } from './boat-rental.model';
import { BoatRentalService } from './boat-rental.service';

@Component({
  selector: 'app-boat-rental-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './boat-rental-admin.component.html',
  styleUrl: './boat-rental-admin.component.scss'
})
export class BoatRentalAdminComponent implements OnInit {
  overview: BoatRentalAdminOverview | null = null;
  newBoatName = '';
  loading = false;
  successMessage = '';
  errorMessage = '';
  readonly timeOptions = this.buildTimeOptions();

  constructor(
    private readonly boatRentalService: BoatRentalService,
    private readonly changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.boatRentalService.getAdminOverview().pipe(
      timeout(10000),
      finalize(() => {
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      })
    ).subscribe({
      next: overview => {
        this.overview = overview;
        this.newBoatName = overview.suggestedBoatName;
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        this.overview = null;
        this.errorMessage = this.extractErrorMessage(error, 'Nepodařilo se načíst administraci loděk.');
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  saveSettings(): void {
    if (this.overview == null) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.boatRentalService.updateSettings({
      dayStartTime: this.overview.settings.dayStartTime,
      dayEndTime: this.overview.settings.dayEndTime
    }).subscribe({
      next: settings => {
        if (this.overview != null) {
          this.overview = { ...this.overview, settings };
        }
        this.successMessage = 'Nastavení časového okna bylo uloženo.';
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        this.errorMessage = this.extractErrorMessage(error, 'Nepodařilo se uložit nastavení.');
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  addBoat(): void {
    if (this.newBoatName.trim() === '') {
      this.errorMessage = 'Zadejte název nové lodky.';
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';
    this.boatRentalService.createBoat({ name: this.newBoatName.trim() }).subscribe({
      next: () => {
        this.successMessage = 'Nová lodka byla přidána.';
        this.changeDetectorRef.detectChanges();
        this.loadOverview();
      },
      error: error => {
        this.errorMessage = this.extractErrorMessage(error, 'Nepodařilo se přidat lodku.');
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  trackByBoatId(_index: number, boat: { id: number }): number {
    return boat.id;
  }

  trackByTime(_index: number, time: string): string {
    return time;
  }

  private buildTimeOptions(): string[] {
    const values: string[] = [];

    for (let hour = 0; hour < 24; hour += 1) {
      for (const minute of [0, 30]) {
        values.push(`${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`);
      }
    }

    return values;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const candidate = error as { error?: { message?: string; error?: string }; message?: string };
    return candidate?.error?.message || candidate?.error?.error || candidate?.message || fallback;
  }
}
