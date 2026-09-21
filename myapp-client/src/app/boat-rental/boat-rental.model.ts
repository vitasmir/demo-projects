export interface BoatRentalBoat {
  id: number;
  name: string;
}

export interface BoatRentalSettings {
  dayStartTime: string;
  dayEndTime: string;
  slotDurationMinutes: number;
}

export interface BoatRentalReservation {
  id: number;
  boatId: number;
  reservedBy: string;
  reservedByUserId: string;
  startDateTime: string;
  endDateTime: string;
}

export interface BoatRentalSchedule {
  date: string;
  settings: BoatRentalSettings;
  boats: BoatRentalBoat[];
  reservations: BoatRentalReservation[];
}

export interface BoatRentalAdminOverview {
  settings: BoatRentalSettings;
  boats: BoatRentalBoat[];
  suggestedBoatName: string;
}

export interface CreateBoatRentalReservationRequest {
  boatId: number;
  date: string;
  startTime: string;
}

export interface CreateBoatRequest {
  name: string;
}

export interface UpdateBoatRentalSettingsRequest {
  dayStartTime: string;
  dayEndTime: string;
}
