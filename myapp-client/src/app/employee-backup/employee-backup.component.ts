import { Component } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { signal } from '@angular/core';
import { from, of, throwError } from 'rxjs';
import { catchError, concatMap, toArray } from 'rxjs/operators';
import { EmployeeService } from '../service/EmployeeService';
import { SpinnerService } from '../service/spinner.service';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../shared/confirmation-dialog/confirmation-dialog.component';
import { Employee } from '../shared/model/Employee';

@Component({
  selector: 'app-employee-backup',
  templateUrl: './employee-backup.component.html'
})
export class EmployeeBackupComponent {
  readonly isBusy = signal(false);
  readonly message = signal('');

  constructor(
    private readonly dialog: MatDialog,
    private readonly employeeService: EmployeeService,
    private readonly spinnerService: SpinnerService
  ) {}

  exportEmployees(): void {
    this.setBusy(true);
    this.employeeService.getEmployeesWithManagers().subscribe({
      next: employees => {
        try {
          this.download('employees.json', JSON.stringify(this.sortEmployeesParentFirst(employees), null, 2), 'application/json');
          this.message.set('Employees exported successfully.');
        } catch (error: any) {
          this.showError(`Could not export employees: ${error.message}`);
        } finally {
          this.setBusy(false);
        }
      },
      error: error => this.showError(`Could not export employees: ${error.message}`)
    });
  }

  importEmployees(): void {
    this.selectFile('.json,application/json', file => {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const employees = JSON.parse(String(reader.result));
          if (!Array.isArray(employees)) {
            throw new Error('The file must contain an employee array.');
          }
          const payload = this.sortEmployeesParentFirst(employees).map(employee => ({
            id: employee.id,
            name: employee.name,
            firstName: employee.firstName,
            lastName: employee.lastName,
            position: employee.position,
            extn: employee.extn,
            salary: employee.salary,
            start_date: employee.start_date,
            office: employee.office,
            hasManagerRights: employee.hasManagerRights,
            managerId: this.getManagerId(employee)
          }));
          this.setBusy(true);
          this.employeeService.restoreEmployees(payload, false).subscribe({
            next: () => this.finish('Employees imported successfully.'),
            error: error => this.showError(`Could not import employees: ${error.message}`)
          });
        } catch (error: any) {
          this.showError(`Could not import employees: ${error.message}`);
        }
      };
      reader.onerror = () => this.showError('Could not read the employee file.');
      reader.readAsText(file);
    });
  }

  exportChanges(): void {
    this.setBusy(true);
    this.employeeService.getIntradayChanges().subscribe({
      next: blob => {
        this.downloadBlob('intra_day_changes.csv', blob);
        this.finish('Changes exported successfully.');
      },
      error: error => this.showError(`Could not export changes: ${error.message}`)
    });
  }

  importChanges(): void {
    this.selectFile('.csv,text/csv', file => {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const operations = this.parseChanges(String(reader.result));
          this.setBusy(true);
          from(operations).pipe(
            concatMap(change => this.replayChange(change)),
            toArray()
          ).subscribe({
            next: () => this.finish(`${operations.length} changes imported successfully.`),
            error: error => this.showError(`Could not import changes: ${error.message}`)
          });
        } catch (error: any) {
          this.showError(`Could not import changes: ${error.message}`);
        }
      };
      reader.onerror = () => this.showError('Could not read the changes file.');
      reader.readAsText(file);
    });
  }

  deleteEmployees(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Delete employees',
        message: 'Do you really want to delete all employees?',
        confirmText: 'Delete',
        cancelText: 'Cancel'
      } as ConfirmationDialogData
    });
    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) {
        return;
      }
      this.setBusy(true);
      this.employeeService.deleteAllEmployees().subscribe({
        next: () => this.finish('All employees deleted successfully.'),
        error: error => this.showError(`Could not delete employees: ${error.message}`)
      });
    });
  }

  private replayChange(change: any) {
    const employee = {
      id: change.employeeId,
      name: change.newName,
      firstName: change.newFirstName,
      lastName: change.newLastName,
      position: change.newPosition,
      extn: change.newExtn,
      salary: change.newSalary,
      start_date: change.newStartDate,
      office: change.newOffice,
      hasManagerRights: change.newHasManagerRights.toLowerCase() === 'true',
      managerId: change.newManagerId || null,
      manager: change.newManagerId ? ({ id: change.newManagerId } as Employee) : null
    } as Employee;
    if (change.action === 'CREATE') {
      return this.employeeService.restoreEmployees([employee], false);
    }
    if (change.action === 'UPDATE') {
      return this.employeeService.updateEmployee(employee, false);
    }
    return this.employeeService.deleteEmployee(change.employeeId, false).pipe(
      catchError(error => error.status === 404 ? of(null) : throwError(() => error))
    );
  }

  private parseChanges(content: string): any[] {
    const lines = content.split(/\r?\n/).filter(line => line.trim().length > 0);
    const header = this.parseCsvLine(lines[0]?.replace(/^\uFEFF/, '').trim() ?? '');
    if (header[0] !== 'timestamp' || header[1] !== 'action') {
      throw new Error('The file does not contain a valid intraday changes header.');
    }
    return lines.slice(1).map(line => {
      let fields = this.parseCsvLine(line);
      if (fields.length === 22) {
        const normalized = fields.slice(0, 3);
        for (let index = 0; index < 9; index++) {
          normalized.push(fields[3 + index], fields[13 + index]);
        }
        fields = normalized;
      }
      if (fields.length === 21 && !fields[2]) {
        const employeeIdIndex = fields.findIndex((value, index) => index >= 3 && value.length > 0);
        if (employeeIdIndex > 2) {
          const normalized = fields.slice(0, 2);
          normalized.push(fields[employeeIdIndex]);
          for (let index = 0; index < 9; index++) {
            normalized.push('', fields[employeeIdIndex + 1 + index] ?? '');
          }
          fields = normalized;
        }
      }
      if (fields.length === 21) {
        fields = [...fields.slice(0, 5), '', '', '', '', ...fields.slice(5)];
      }
      if (fields.length !== 25 || !['CREATE', 'UPDATE', 'DELETE'].includes(fields[1])) {
        throw new Error('Invalid intraday changes row.');
      }
      return {
        action: fields[1], employeeId: fields[2], newName: fields[4], newFirstName: fields[6],
        newLastName: fields[8], newPosition: fields[10], newExtn: fields[12], newSalary: fields[14],
        newStartDate: fields[16], newOffice: fields[18], newHasManagerRights: fields[20], newManagerId: fields[22]
      };
    });
  }

  private parseCsvLine(line: string): string[] {
    const fields: string[] = [];
    let field = '';
    let quoted = false;
    for (let index = 0; index < line.length; index++) {
      const character = line[index];
      if (character === '"' && line[index + 1] === '"' && quoted) {
        field += '"';
        index++;
      } else if (character === '"') {
        quoted = !quoted;
      } else if (character === ',' && !quoted) {
        fields.push(field);
        field = '';
      } else {
        field += character;
      }
    }
    fields.push(field);
    return fields;
  }

  private sortEmployeesParentFirst(employees: Employee[]): Employee[] {
    const byId = new Map(employees.map(employee => [employee.id, employee]));
    const ordered: Employee[] = [];
    const visited = new Set<string>();
    const stack = employees.filter(employee => {
      const managerId = this.getManagerId(employee);
      return !managerId || !byId.has(managerId);
    }).reverse();
    while (stack.length > 0) {
      const employee = stack.pop();
      if (!employee || visited.has(employee.id)) continue;
      visited.add(employee.id);
      ordered.push(employee);
      stack.push(...employees.filter(candidate => this.getManagerId(candidate) === employee.id).reverse());
    }
    return [...ordered, ...employees.filter(employee => !visited.has(employee.id))];
  }

  private getManagerId(employee: Employee): string | null {
    return employee.manager?.id ?? employee.managerId ?? null;
  }

  private selectFile(accept: string, callback: (file: File) => void): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.onchange = () => {
      const file = input.files?.[0];
      if (file) callback(file);
    };
    input.click();
  }

  private download(filename: string, content: string, type: string): void {
    this.downloadBlob(filename, new Blob([content], { type }));
  }

  private downloadBlob(filename: string, blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  private setBusy(value: boolean): void {
    this.isBusy.set(value);
    if (value) this.spinnerService.show(); else this.spinnerService.hide();
  }

  private finish(text: string): void {
    this.message.set(text);
    this.setBusy(false);
  }

  private showError(text: string): void {
    this.message.set(text);
    this.setBusy(false);
    alert(text);
  }
}