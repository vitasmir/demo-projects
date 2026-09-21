import { CommonModule } from '@angular/common';
import { Component, Inject, AfterViewInit, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { EmployeeService } from '../service/EmployeeService';
import { Employee } from '../shared/model/Employee';

declare var $: any; // jQuery / DataTables

@Component({
  selector: 'app-manager-selector',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatProgressSpinnerModule, TranslatePipe],
  templateUrl: './manager-selector.component.html',
  styleUrls: ['./manager-selector.component.scss']
})
export class ManagerSelectorComponent implements OnInit, AfterViewInit, OnDestroy {
  private dt: any = null;
  managers: Employee[] = [];
  loading = true;
  private viewInitialized = false;

  constructor(
    public dialogRef: MatDialogRef<ManagerSelectorComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { managers?: Employee[]; selected?: Employee | string | null; currentEmployeeId?: string | null },
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.data.managers && this.data.managers.length > 0) {
      this.managers = this.prepareManagers(this.data.managers);
      this.loading = false;
      return;
    }

    this.loading = true;
    this.employeeService.getEmployees()
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.markForCheck();
        this.tryInitializeDataTable();
      }))
      .subscribe((allEmployees: Employee[]) => {
        this.managers = this.prepareManagers(allEmployees);
        this.cdr.markForCheck();
      });
  }

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.tryInitializeDataTable();
  }

  private tryInitializeDataTable(): void {
    if (this.loading || !this.viewInitialized) {
      return;
    }

    setTimeout(() => this.initializeDataTable());
  }

  private initializeDataTable(): void {
    if (this.dt) {
      return;
    }

    try {
      this.dt = ($('#managerTable') as any).DataTable({
        pageLength: 20,
        lengthMenu: [20, 50, 100],
        ordering: true,
        dom: 'Bfrtip',
        // enable Select extension for consistent single-row selection UX
        select: { style: 'single' }
      });

      // If a manager was pre-selected, mark that row as selected in DataTable
      const preId = this.getSelectedManagerId();
      if (preId) {
        const preRow = $(`#managerTable tbody tr[data-id='${preId}']`);
        if (preRow && preRow.length) {
          // use DataTables API to select
          try {
            const rowApi = this.dt.row(preRow);
            rowApi.select && rowApi.select();
            preRow.addClass('selected');
          } catch (e) {
            preRow.addClass('selected');
          }
        }
      }

      $('#managerTable tbody').on('click', 'tr', (event: any) => {
        const tr = $(event.currentTarget);
        const id = tr.attr('data-id');
        if (!id) return;
        const selected = this.managers.find(m => m.id === id);
        if (selected) {
          // visually mark selected row
          $('#managerTable tbody tr').removeClass('selected');
          tr.addClass('selected');
          this.selectManager(selected);
        }
      });
    } catch (e) {
      // If DataTables isn't available, fall back to simple click handlers (Angular also supports click binding)
      console.warn('DataTable init failed or not available', e);
    }
  }

  private prepareManagers(allEmployees: Employee[]): Employee[] {
    const potentialManagers = allEmployees.filter((employee) => employee.hasManagerRights);
    const uniqueManagers = potentialManagers.filter((manager, index, self) =>
      index === self.findIndex((m) => m.id === manager.id)
    );

    if (this.data.currentEmployeeId) {
      return uniqueManagers.filter(manager => manager.id !== this.data.currentEmployeeId);
    }

    return uniqueManagers;
  }

  private getSelectedManagerId(): string | null {
    if (!this.data.selected) {
      return null;
    }

    return typeof this.data.selected === 'string'
      ? this.data.selected
      : this.data.selected.id;
  }

  ngOnDestroy(): void {
    try {
      if (this.dt) {
        this.dt.destroy(true);
        this.dt = null;
      }
      $('#managerTable tbody').off('click', 'tr');
    } catch (e) {
      // ignore
    }
  }

  selectManager(m: Employee) {
    this.dialogRef.close(m);
  }

  close() {
    this.dialogRef.close(null);
  }
}


