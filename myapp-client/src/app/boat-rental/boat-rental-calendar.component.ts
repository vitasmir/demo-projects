import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import flatpickr from 'flatpickr';
import { Czech } from 'flatpickr/dist/l10n/cs.js';
import { Instance as FlatpickrInstance } from 'flatpickr/dist/types/instance';
import { AuthService } from '../auth/auth.service';
import { BoatRentalReservation, BoatRentalSchedule } from './boat-rental.model';
import { BoatRentalService } from './boat-rental.service';

@Component({
  selector: 'app-boat-rental-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './boat-rental-calendar.component.html',
  styleUrl: './boat-rental-calendar.component.scss'
})
export class BoatRentalCalendarComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('dateInput', { static: true }) private readonly dateInputRef?: ElementRef<HTMLInputElement>;

  selectedDate = this.getTodayString();
  visibleDate = this.formatDmy(this.parseInternalDate(this.selectedDate));
  schedule: BoatRentalSchedule | null = null;
  slotTimes: string[] = [];
  reservationLookup = new Map<string, BoatRentalReservation>();
  loading = false;
  errorMessage = '';
  savingMessage = '';
  currentUsername = '';
  currentUserIsAdmin = false;
  private datePicker: FlatpickrInstance | null = null;

  constructor(
    private readonly boatRentalService: BoatRentalService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUsername = this.authService.getUsername() ?? '';
    this.currentUserIsAdmin = this.authService.isAdmin();
    this.loadSchedule();
  }

  ngAfterViewInit(): void {
    const input = this.dateInputRef?.nativeElement;
    if (input == null) {
      return;
    }

    input.value = this.visibleDate;

    this.datePicker = flatpickr(input, {
      locale: Czech,
      dateFormat: 'd.m.Y',
      defaultDate: this.parseInternalDate(this.selectedDate),
      allowInput: true,
      disableMobile: true,
      onChange: selectedDates => {
        const selected = selectedDates[0];
        if (selected == null) {
          return;
        }

        this.submitDate(selected);
      },
      onClose: (_selectedDates, dateStr) => {
        if (dateStr.trim() === '') {
          this.syncDatePicker();
          return;
        }

        const parsed = this.parseDmy(dateStr);
        if (parsed == null) {
          this.syncDatePicker();
          return;
        }

        this.submitDate(parsed);
      }
    });
  }

  ngOnDestroy(): void {
    this.datePicker?.destroy();
    this.datePicker = null;
  }

  loadSchedule(): void {
    this.loading = true;
    this.errorMessage = '';
    this.savingMessage = '';
    this.boatRentalService.getSchedule(this.selectedDate).subscribe({
      next: schedule => {
        this.schedule = schedule;
        this.slotTimes = this.buildSlotTimes(schedule.settings.dayStartTime, schedule.settings.dayEndTime, schedule.settings.slotDurationMinutes);
        this.rebuildReservationLookup(schedule.reservations);
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        this.errorMessage = this.extractErrorMessage(error, 'Nepodařilo se načíst kalendář rezervací.');
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  goToPreviousDay(): void {
    this.submitDate(this.shiftDate(this.selectedDate, -1));
  }

  goToNextDay(): void {
    this.submitDate(this.shiftDate(this.selectedDate, 1));
  }

  handleVisibleDateChange(rawValue: string): void {
    const parsed = this.parseDmy(rawValue);
    if (parsed == null) {
      this.syncDatePicker();
      return;
    }

    this.submitDate(parsed);
  }

  canReserve(): boolean {
    return this.currentUsername.trim() !== '';
  }

  canCancel(reservation: BoatRentalReservation): boolean {
    return this.currentUsername.trim() !== ''
      && (reservation.reservedByUserId === this.currentUsername || this.currentUserIsAdmin);
  }

  isReserved(boatId: number, slotTime: string): boolean {
    return this.reservationLookup.has(this.buildCellKey(boatId, slotTime));
  }

  getReservation(boatId: number, slotTime: string): BoatRentalReservation | undefined {
    return this.reservationLookup.get(this.buildCellKey(boatId, slotTime));
  }

  reserveSlot(boatId: number, slotTime: string): void {
    if (!this.canReserve()) {
      this.errorMessage = 'Zadejte prosím své jméno před vytvořením rezervace.';
      return;
    }

    this.savingMessage = '';
    this.errorMessage = '';
    this.boatRentalService.createReservation({
      boatId,
      date: this.selectedDate,
      startTime: slotTime
    }).subscribe({
      next: reservation => {
        this.savingMessage = 'Rezervace byla úspěšně vytvořena.';
        if (this.schedule != null) {
          this.schedule = {
            ...this.schedule,
            reservations: [...this.schedule.reservations, reservation]
          };
        }
        this.reservationLookup.set(this.buildCellKey(boatId, slotTime), reservation);
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        this.errorMessage = this.extractErrorMessage(error, 'Rezervaci se nepodařilo vytvořit.');
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  cancelReservation(reservation: BoatRentalReservation): void {
    this.savingMessage = '';
    this.errorMessage = '';
    this.boatRentalService.cancelReservation(reservation.id).subscribe({
      next: () => {
        this.savingMessage = 'Rezervace byla zrušena.';
        if (this.schedule != null) {
          this.schedule = {
            ...this.schedule,
            reservations: this.schedule.reservations.filter(item => item.id !== reservation.id)
          };
        }
        this.reservationLookup.delete(this.buildCellKey(reservation.boatId, reservation.startDateTime.substring(11, 16)));
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        this.errorMessage = this.extractErrorMessage(error, 'Rezervaci se nepodařilo zrušit.');
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  trackByBoatId(_index: number, boat: { id: number }): number {
    return boat.id;
  }

  trackBySlot(_index: number, slot: string): string {
    return slot;
  }

  private rebuildReservationLookup(reservations: BoatRentalReservation[]): void {
    this.reservationLookup.clear();
    reservations.forEach(reservation => {
      const start = reservation.startDateTime.substring(11, 16);
      this.reservationLookup.set(this.buildCellKey(reservation.boatId, start), reservation);
    });
  }

  private buildSlotTimes(startTime: string, endTime: string, slotDurationMinutes: number): string[] {
    const slots: string[] = [];
    let currentMinutes = this.timeToMinutes(startTime);
    const endMinutes = this.timeToMinutes(endTime);

    while (currentMinutes + slotDurationMinutes <= endMinutes) {
      slots.push(this.minutesToTime(currentMinutes));
      currentMinutes += slotDurationMinutes;
    }

    return slots;
  }

  private buildCellKey(boatId: number, slotTime: string): string {
    return `${boatId}_${slotTime}`;
  }

  private shiftDate(dateString: string, dayOffset: number): Date {
    const date = this.parseInternalDate(dateString);
    date.setDate(date.getDate() + dayOffset);
    return date;
  }

  private getTodayString(): string {
    return this.formatInternalDate(new Date());
  }

  private submitDate(date: Date): void {
    const normalizedDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const nextSelectedDate = this.formatInternalDate(normalizedDate);
    if (nextSelectedDate === this.selectedDate) {
      this.visibleDate = this.formatDmy(normalizedDate);
      this.syncDatePicker();
      return;
    }

    this.selectedDate = nextSelectedDate;
    this.visibleDate = this.formatDmy(normalizedDate);
    this.syncDatePicker();
    this.loadSchedule();
  }

  private syncDatePicker(): void {
    const nextDate = this.parseInternalDate(this.selectedDate);
    this.visibleDate = this.formatDmy(nextDate);
    this.datePicker?.setDate(nextDate, false);
    if (this.dateInputRef?.nativeElement != null) {
      this.dateInputRef.nativeElement.value = this.visibleDate;
    }
  }

  private parseInternalDate(value: string): Date {
    const normalized = value.trim();
    const match = normalized.match(/^(\d{4})[.-](\d{1,2})[.-](\d{1,2})$/);
    if (match == null) {
      return new Date();
    }

    return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  }

  private parseDmy(value: string): Date | null {
    const normalized = value.trim();
    const match = normalized.match(/^(\d{1,2})\.(\d{1,2})\.(\d{4})$/);
    if (match == null) {
      return null;
    }

    const [, dayValue, monthValue, yearValue] = match;
    const day = Number(dayValue);
    const month = Number(monthValue);
    const year = Number(yearValue);
    const parsed = new Date(year, month - 1, day);

    if (
      Number.isNaN(parsed.getTime())
      || parsed.getFullYear() !== year
      || parsed.getMonth() !== month - 1
      || parsed.getDate() !== day
    ) {
      return null;
    }

    return parsed;
  }

  private formatInternalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
  }

  private formatDmy(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${day}.${month}.${year}`;
  }

  private timeToMinutes(time: string): number {
    const [hours, minutes] = time.split(':').map(Number);
    return hours * 60 + minutes;
  }

  private minutesToTime(totalMinutes: number): string {
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const candidate = error as { error?: { message?: string; error?: string }; message?: string };
    return candidate?.error?.message || candidate?.error?.error || candidate?.message || fallback;
  }
}
