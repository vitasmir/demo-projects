import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ManagerSelectorComponent } from '../manager-selector/manager-selector.component';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { EmployeeService } from '../service/EmployeeService';
import { Employee } from '../shared/model/Employee';
import { TranslateModule } from '@ngx-translate/core';
import { TranslatePipe } from '@ngx-translate/core';
import flatpickr from 'flatpickr';
import { Czech } from 'flatpickr/dist/l10n/cs.js';
import { Instance as FlatpickrInstance } from 'flatpickr/dist/types/instance';

@Component({
  selector: 'app-add-employee',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    TranslatePipe,
    MatDialogModule],
  providers: [
  ],
  templateUrl: './add-employee.component.html',
  styleUrls: ['./add-employee.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddEmployeeComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('startDateInput', { static: true }) private readonly startDateInputRef?: ElementRef<HTMLInputElement>;

  onClose() {
    this.dialogRef.close();
  }

  employeeObject: Employee = new Employee('', '', '', '', '', '', '', '', '', false, null);

  isEditMode: boolean = false;
  managers: Employee[] = [];
  employeeNameExists = false;
  employeeNameCheckPending = false;
  startDateDisplay = '';
  private originalName: string | undefined;
  private startDatePicker: FlatpickrInstance | null = null;

  constructor(
    public dialogRef: MatDialogRef<AddEmployeeComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Employee | null,
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) {
    this.isEditMode = !!data?.id;

    if (this.data) {
      this.employeeObject = {
        ...this.employeeObject,
        ...this.data,
        start_date: this.data.start_date ?? '',
        managerId: this.data.managerId ?? this.employeeObject.managerId,
        manager: this.data.manager ?? this.employeeObject.manager,
        hasManagerRights: this.data.hasManagerRights ?? false
      };

      if (this.isEditMode) {
        this.originalName = this.data.name;
      }
    }
  }

  openManagerSelector(): void {
    const dialogRef = this.dialog.open(ManagerSelectorComponent, {
      data: {
        selected: this.employeeObject.manager ?? this.employeeObject.managerId,
        currentEmployeeId: this.employeeObject.id || null
      },
      width: '600px'
    });

    dialogRef.afterClosed().subscribe((selected: Employee | null) => {
      if (selected) {
        this.employeeObject.manager = selected;
        this.employeeObject.managerId = selected.id;
        this.cdr.markForCheck();
      }
    });
  }

  clearManager(): void {
    this.employeeObject.manager = null;
    this.employeeObject.managerId = null;
    this.cdr.markForCheck();
  }

  openStartDatePicker(): void {
    this.startDatePicker?.open();
  }

  ngAfterViewInit(): void {
    const input = this.startDateInputRef?.nativeElement;
    if (input == null) {
      return;
    }

    input.value = this.startDateDisplay;

    this.startDatePicker = flatpickr(input, {
      locale: Czech,
      dateFormat: 'd.m.Y',
      allowInput: true,
      disableMobile: true,
      static: true,
      defaultDate: this.parseStoredStartDate(this.employeeObject.start_date) ?? undefined,
      onChange: selectedDates => {
        const selectedDate = selectedDates[0];
        if (selectedDate == null) {
          return;
        }

        this.applyStartDate(selectedDate);
      },
      onOpen: (_selectedDates, _dateStr, instance) => {
        instance.calendarContainer.style.zIndex = '10000';
      },
      onClose: (_selectedDates, dateStr) => {
        const normalized = dateStr.trim();
        if (normalized === '') {
          this.startDateDisplay = '';
          this.employeeObject.start_date = '';
          this.syncStartDatePicker();
          this.cdr.markForCheck();
          return;
        }

        const parsed = this.parseDisplayDate(normalized);
        if (parsed == null) {
          this.syncStartDatePicker();
          return;
        }

        this.applyStartDate(parsed);
      }
    });

    this.syncStartDatePicker();
  }

  ngOnDestroy(): void {
    this.startDatePicker?.destroy();
    this.startDatePicker = null;
  }

  ngOnInit(): void {
    this.loadManagers();
    this.employeeObject.start_date = this.normalizeInternalDate(this.employeeObject.start_date);
    this.startDateDisplay = this.toDisplayDate(this.employeeObject.start_date);
    console.log('AddEmployeeComponent initialized', {
      employeeObject: this.employeeObject,
    });
  }

  loadManagers(onLoaded?: () => void) {
    this.employeeService.getEmployees().subscribe((allEmployees: Employee[]) => {
      // A manager is an employee who has the `hasManagerRights` flag set.
      // Top-level employees (no manager) are NOT considered managers unless this flag is true.
      const potentialManagers = allEmployees.filter(
        (employee) => employee.hasManagerRights
      );

      // Ensure the list of managers is unique by employee ID
      const allManagers = potentialManagers.filter((manager, index, self) =>
        index === self.findIndex((m) => m.id === manager.id)
      );

      if (this.isEditMode) {
        // Exclude the current employee from the list of potential managers
        this.managers = allManagers.filter(manager => manager.id !== this.employeeObject.id);
      } else {
        this.managers = allManagers;
      }

      if (this.isEditMode) {
        const resolvedManagerId = this.getCurrentManagerId();
        if (resolvedManagerId) {
          this.employeeObject.manager = this.managers.find(m => m.id === resolvedManagerId) || null;
          this.employeeObject.managerId = this.employeeObject.manager?.id || resolvedManagerId;
        }
      }

      this.cdr.markForCheck();
      onLoaded?.();
    });
  }

  checkEmployeeName(name: string): void {
    // Don't validate if the name is empty or hasn't changed from the original in edit mode
    if (!name || (this.isEditMode && name === this.originalName)) {
      this.employeeNameExists = false;
      this.employeeNameCheckPending = false;
      return;
    }

    this.employeeNameCheckPending = true;
    this.employeeService.employeeByNameExists(name).subscribe((exists: boolean) => {
      this.employeeNameExists = exists;
      this.employeeNameCheckPending = false;
      this.cdr.markForCheck(); // Manually trigger change detection for OnPush
    });
  }


  compareEmployees(e1: Employee, e2: Employee): boolean {
    return e1 && e2 ? e1.id === e2.id : e1 === e2;
  }

  getSelectedManagerName(): string {
    if (!this.employeeObject.manager) {
      return '';
    }

    if (typeof this.employeeObject.manager === 'object') {
      return this.employeeObject.manager.name || '';
    }

    const managerId = this.employeeObject.manager as unknown as string;
    return this.managers.find(m => m.id === managerId)?.name || '';
  }

  private getCurrentManagerId(): string | null {
    if (this.employeeObject.manager && typeof this.employeeObject.manager === 'object') {
      return this.employeeObject.manager.id || null;
    }

    if (this.employeeObject.manager && typeof this.employeeObject.manager === 'string') {
      return this.employeeObject.manager;
    }

    return this.employeeObject.managerId || null;
  }

  isStartDateValid(): boolean {
    const date = this.employeeObject.start_date;
    if (!date) return false;

    return this.parseStoredStartDate(date) != null;
  }

  handleStartDateInput(rawValue: string): void {
    const normalized = rawValue.trim();
    if (normalized === '') {
      this.startDateDisplay = '';
      this.employeeObject.start_date = '';
      this.syncStartDatePicker();
      this.cdr.markForCheck();
      return;
    }

    const parsed = this.parseDisplayDate(normalized);
    if (parsed == null) {
      this.syncStartDatePicker();
      return;
    }

    this.applyStartDate(parsed);
  }

  onSubmit(form: any): void {
    if (!form.valid || this.employeeNameCheckPending || this.employeeNameExists || !this.isStartDateValid()) {
      return;
    }

    if (this.isEditMode) {
      this.closeWithEmployee();
      return;
    }

    this.employeeNameCheckPending = true;
    this.employeeService.employeeByNameExists(this.employeeObject.name.trim()).subscribe({
      next: exists => {
        this.employeeNameExists = exists;
        this.employeeNameCheckPending = false;
        this.cdr.markForCheck();
        if (!exists) {
          this.closeWithEmployee();
        }
      },
      error: () => {
        this.employeeNameCheckPending = false;
        this.cdr.markForCheck();
      }
    });
  }

  getModalTitle(): string {
    return this.isEditMode ? 'edit-employee' : 'new-employee';
  }

  private closeWithEmployee(): void {
    this.employeeObject.managerId = this.employeeObject.manager?.id || null;
    this.dialogRef.close({
      ...this.employeeObject,
      start_date: this.toApiDate(this.employeeObject.start_date)
    });
  }

  private applyStartDate(date: Date): void {
    const normalizedDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    this.employeeObject.start_date = this.formatInternalDate(normalizedDate);
    this.startDateDisplay = this.formatDisplayDate(normalizedDate);
    this.syncStartDatePicker();
    this.cdr.markForCheck();
  }

  private syncStartDatePicker(): void {
    const parsed = this.parseStoredStartDate(this.employeeObject.start_date);
    this.startDateDisplay = parsed == null ? '' : this.formatDisplayDate(parsed);

    if (this.startDateInputRef?.nativeElement != null) {
      this.startDateInputRef.nativeElement.value = this.startDateDisplay;
    }

    if (this.startDatePicker == null) {
      return;
    }

    if (parsed == null) {
      this.startDatePicker.clear(false);
      return;
    }

    this.startDatePicker.setDate(parsed, false);
  }

  private normalizeInternalDate(value: string | null | undefined): string {
    const parsed = this.parseStoredStartDate(value);
    return parsed == null ? '' : this.formatInternalDate(parsed);
  }

  private toDisplayDate(value: string | null | undefined): string {
    const parsed = this.parseStoredStartDate(value);
    return parsed == null ? '' : this.formatDisplayDate(parsed);
  }

  private toApiDate(value: string): string {
    const parsed = this.parseStoredStartDate(value);
    if (parsed == null) {
      return value;
    }

    const year = parsed.getFullYear();
    const month = String(parsed.getMonth() + 1).padStart(2, '0');
    const day = String(parsed.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private parseStoredStartDate(value: string | null | undefined): Date | null {
    const normalized = value?.trim() ?? '';
    if (normalized === '') {
      return null;
    }

    const match = normalized.match(/^(\d{4})[-\.](\d{1,2})[-\.](\d{1,2})$/);
    if (match == null) {
      return null;
    }

    const [, yearValue, monthValue, dayValue] = match;
    return this.createValidDate(Number(yearValue), Number(monthValue), Number(dayValue));
  }

  private parseDisplayDate(value: string): Date | null {
    const match = value.match(/^(\d{1,2})\.(\d{1,2})\.(\d{4})$/);
    if (match == null) {
      return null;
    }

    const [, dayValue, monthValue, yearValue] = match;
    return this.createValidDate(Number(yearValue), Number(monthValue), Number(dayValue));
  }

  private createValidDate(year: number, month: number, day: number): Date | null {
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

  private formatDisplayDate(date: Date): string {
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}.${month}.${year}`;
  }

  private formatInternalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
  }


}
