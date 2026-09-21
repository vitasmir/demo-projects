import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BoatRentalAdminOverview,
  BoatRentalSchedule,
  BoatRentalSettings,
  BoatRentalBoat,
  BoatRentalReservation,
  CreateBoatRentalReservationRequest,
  CreateBoatRequest,
  UpdateBoatRentalSettingsRequest
} from './boat-rental.model';

@Injectable({
  providedIn: 'root'
})
export class BoatRentalService {
  private readonly baseUrl = '/api/boat-rental';

  constructor(private readonly http: HttpClient) {}

  getSchedule(date: string): Observable<BoatRentalSchedule> {
    return this.http.get<BoatRentalSchedule>(`${this.baseUrl}/schedule`, {
      params: { date: this.toApiDate(date) }
    });
  }

  createReservation(request: CreateBoatRentalReservationRequest): Observable<BoatRentalReservation> {
    return this.http.post<BoatRentalReservation>(`${this.baseUrl}/reservations`, {
      ...request,
      date: this.toApiDate(request.date)
    });
  }

  cancelReservation(reservationId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/reservations/${reservationId}`);
  }

  getAdminOverview(): Observable<BoatRentalAdminOverview> {
    return this.http.get<BoatRentalAdminOverview>(`${this.baseUrl}/admin/overview`);
  }

  createBoat(request: CreateBoatRequest): Observable<BoatRentalBoat> {
    return this.http.post<BoatRentalBoat>(`${this.baseUrl}/admin/boats`, request);
  }

  updateSettings(request: UpdateBoatRentalSettingsRequest): Observable<BoatRentalSettings> {
    return this.http.put<BoatRentalSettings>(`${this.baseUrl}/admin/settings`, request);
  }

  private toApiDate(value: string): string {
    const normalized = value.trim();
    const match = normalized.match(/^(\d{4})[-\.](\d{1,2})[-\.](\d{1,2})$/);
    if (match == null) {
      return value;
    }

    const [, yearValue, monthValue, dayValue] = match;
    const month = monthValue.padStart(2, '0');
    const day = dayValue.padStart(2, '0');
    return `${yearValue}-${month}-${day}`;
  }
}
