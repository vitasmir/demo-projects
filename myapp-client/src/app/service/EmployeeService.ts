import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { Employee } from '../shared/model/Employee';


@Injectable({
  providedIn: 'root'
})
export class EmployeeService {

  // Use a relative path for API calls to work with the Nginx proxy
  private baseUrl = '/api';

  constructor(private http: HttpClient) { }


  getEmployee(id: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/employees/${id}`);
  }

  getRootEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.baseUrl}/employees/tree`);
  }

  getManagers(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.baseUrl}/employees/managers`);
  }

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.baseUrl}/employees`);
  }

  getEmployeesWithManagers(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.baseUrl}/employees/with-managers`);
  }

  getIntradayChanges(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/employees/changes`, { responseType: 'blob' });
  }

  clearIntradayChanges(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/employees/changes`);
  }

  restoreEmployees(employees: unknown[], recordChanges = true): Observable<Employee[]> {
    return this.http.post<Employee[]>(`${this.baseUrl}/employees/restore`, employees, {
      params: { recordChanges: recordChanges.toString() }
    });
  }

  getEmployeeWithManager(id: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/employees/with-manager/${id}`);
  }

  employeeByNameExists(employeeName: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/employees/employeeByNameExists/${encodeURIComponent(employeeName)}`);
  }

  createEmployee(employee: Employee): Observable<Employee> {
    let params = new HttpParams();
    const managerId = employee.manager ? employee.manager.id : null;
    if (managerId) {
      params = params.append('managerId', managerId);
    }
    // Create a clone without the manager to avoid issues with circular dependencies or object identity during JSON serialization.
    const employeeToSend = { ...employee, manager: undefined, children: undefined };
    return this.http.post<Employee>(`${this.baseUrl}/employees`, employeeToSend, { params });
  }

  updateEmployee(employee: Employee, recordChanges = true): Observable<Employee> {
    let params = new HttpParams();
    // When updating, we always specify the manager's status.
    // If employee.manager is an object, we use its ID to set/change the manager.
    // If employee.manager is null, it signifies that the manager should be removed.
    // The backend expects a 'null' string to process the removal.
    const managerId = employee.manager ? employee.manager.id : 'null';
    params = params.append('managerId', managerId).append('recordChanges', recordChanges.toString());
    // Create a clone without the manager to avoid issues with circular dependencies or object identity during JSON serialization.
    const employeeToSend = { ...employee, manager: undefined, children: undefined };
    return this.http.put<Employee>(`${this.baseUrl}/employees/${employee.id}`, employeeToSend, { params });
  }

  moveChildrenToManager(employeeId: string, managerId: string): Observable<Employee[]> {
    const params = new HttpParams().append('managerId', managerId);
    return this.http.put<Employee[]>(`${this.baseUrl}/employees/${employeeId}/children/manager`, null, { params });
  }

  deleteEmployee(empId: string, recordChanges = true): Observable<void> {
    const deleteUrl = `${this.baseUrl}/employees/${empId}`;
    return this.http.delete<void>(deleteUrl, {
      params: { recordChanges: recordChanges.toString() }
    });
  }

  deleteAllEmployees(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/employees/all`);
  }

}