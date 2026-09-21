import { Component, OnInit } from '@angular/core';
import { NestedTreeControl } from '@angular/cdk/tree';
import { MatTreeModule, MatTreeNestedDataSource } from '@angular/material/tree';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { Employee } from '../shared/model/Employee';
import { EmployeeService } from '../service/EmployeeService';
import { SpinnerComponent } from "../shared/spinner/spinner.component";
import { SpinnerService } from '../service/spinner.service';
import { forkJoin, Observable } from 'rxjs';

type HierarchyUndoAction = MoveEmployeeUndoAction | MoveChildrenUndoAction;

interface MoveEmployeeUndoAction {
  type: 'moveEmployee';
  employeeId: string;
  previousManagerId: string | null;
  newManagerId: string | null;
}

interface MoveChildrenUndoAction {
  type: 'moveChildren';
  children: Array<{
    employeeId: string;
    previousManagerId: string | null;
    newManagerId: string | null;
  }>;
}

@Component({
  selector: 'app-trees-of-employees',
  standalone: true,
  imports: [CommonModule, MatTreeModule, MatButtonModule,
    SpinnerComponent],
  templateUrl: './trees-of-employees.component.html',
  styleUrl: './trees-of-employees.component.scss'
})
export class TreesOfEmployeesComponent implements OnInit {
  title = 'Employee hierarchy';

  treeControl = new NestedTreeControl<Employee>(node => node.children);
  dataSource = new MatTreeNestedDataSource<Employee>();
  lowerTreeControl = new NestedTreeControl<Employee>(node => node.children);
  lowerDataSource = new MatTreeNestedDataSource<Employee>();
  allEmployees: Employee[] = [];
  selectedEmployee: Employee | null = null;
  selectedTargetManager: Employee | null = null;
  searchText = '';
  searchInputText = '';
  lowerSearchText = '';
  lowerSearchInputText = '';
  undoStack: HierarchyUndoAction[] = [];
  redoStack: HierarchyUndoAction[] = [];
  isUndoing = false;
  isRedoing = false;

  constructor(private employeeService: EmployeeService, private spinnerService: SpinnerService) { }

  ngOnInit(): void {
    this.loadTree();
  }

  loadTree(expandAfterLoad = false, selectedEmployeeId: string | null = null): void {
    this.spinnerService.show();
    this.employeeService.getRootEmployees().subscribe(employees => {
      this.allEmployees = employees;
      if (selectedEmployeeId) {
        this.selectedEmployee = this.findEmployeeById(selectedEmployeeId);
      } else {
        this.selectedEmployee = null;
      }
      this.selectedTargetManager = null;
      this.refreshDisplayedTree();
      this.refreshLowerTree();
      if (expandAfterLoad) {
        setTimeout(() => selectedEmployeeId ? this.expandToEmployee(selectedEmployeeId) : this.expandDisplayedTree());
      }
      this.spinnerService.hide();
    }, error => {
      this.spinnerService.hide();
      alert('Failed to load employee hierarchy: ' + this.getErrorMessage(error));
    });
  }

  hasChild = (_: number, node: Employee) => !!node.children && node.children.length > 0;

  selectEmployee(node: Employee): void {
    this.selectedEmployee = this.isSelected(node) ? null : this.findEmployeeById(node.id);
  }

  isSelected(node: Employee): boolean {
    return this.selectedEmployee?.id === node.id;
  }

  selectTargetManager(node: Employee): void {
    if (!this.isManager(node)) {
      return;
    }
    this.selectedTargetManager = this.isTargetManager(node) ? null : this.findEmployeeById(node.id);
  }

  isTargetManager(node: Employee): boolean {
    return this.selectedTargetManager?.id === node.id;
  }

  isLowerSearchMatch(node: Employee): boolean {
    const normalizedSearchText = this.lowerSearchText.trim().toLowerCase();
    return normalizedSearchText.length > 0 && [node.name, node.firstName, node.lastName]
      .some(value => value?.toLowerCase().includes(normalizedSearchText));
  }

  isManager(node: Employee): boolean {
    return node.hasManagerRights === true;
  }

  selectedEmployeeHasChildren(): boolean {
    return !!this.selectedEmployee?.children?.length;
  }

  canUndo(): boolean {
    return this.undoStack.length > 0 && !this.isUndoing && !this.isRedoing;
  }

  canRedo(): boolean {
    return this.redoStack.length > 0 && !this.isRedoing && !this.isUndoing;
  }

  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchInputText = target.value;
  }

  onLowerSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.lowerSearchInputText = target.value;
  }

  searchEmployees(): void {
    const searchText = this.searchInputText.trim();

    if (searchText.length < 3) {
      this.searchText = '';
      this.refreshDisplayedTree();
      return;
    }

    this.searchText = searchText;
    this.refreshDisplayedTree();
  }

  clearSearch(): void {
    this.searchInputText = '';
    this.searchText = '';
    this.refreshDisplayedTree();
  }

  searchLowerEmployees(): void {
    const searchText = this.lowerSearchInputText.trim();

    if (searchText.length < 3) {
      this.lowerSearchText = '';
      this.refreshLowerTree();
      return;
    }

    this.lowerSearchText = searchText;
    this.refreshLowerTree();
  }

  clearLowerSearch(): void {
    this.lowerSearchInputText = '';
    this.lowerSearchText = '';
    this.refreshLowerTree();
  }

  expandDisplayedTree(): void {
    this.expandEmployees(this.dataSource.data);
  }

  expandLowerTree(): void {
    this.expandEmployees(this.lowerDataSource.data, this.lowerTreeControl);
  }

  canMoveToManager(manager: Employee): boolean {
    const selectedEmployee = this.selectedEmployee ? this.findEmployeeById(this.selectedEmployee.id) : null;
    const targetManager = this.findEmployeeById(manager.id);

    if (!selectedEmployee || !targetManager || !this.isManager(targetManager)) {
      return false;
    }

    if (this.wouldCreateManagerCycle(selectedEmployee, targetManager)) {
      return false;
    }

    // prevent assigning to the same manager (no-op)
    const currentManagerId = this.getManagerId(selectedEmployee);
    if (currentManagerId && currentManagerId === targetManager.id) {
      return false;
    }

    return true;
  }

  canMoveToSelectedTarget(): boolean {
    return !!this.selectedTargetManager && this.canMoveToManager(this.selectedTargetManager);
  }

  private wouldCreateManagerCycle(employee: Employee, proposedManager: Employee): boolean {
    return proposedManager.id === employee.id || this.isDescendantOf(proposedManager, employee);
  }

  moveSelectedAsChild(manager: Employee): void {
    const selectedEmployee = this.selectedEmployee ? this.findEmployeeById(this.selectedEmployee.id) : null;
    const targetManager = this.findEmployeeById(manager.id);

    if (!selectedEmployee || !targetManager || !this.canMoveToManager(targetManager)) {
      return;
    }

    const employeeToMove = { ...selectedEmployee, manager: targetManager, children: [] } as Employee;
    const undoAction: MoveEmployeeUndoAction = {
      type: 'moveEmployee',
      employeeId: selectedEmployee.id,
      previousManagerId: this.getManagerId(selectedEmployee),
      newManagerId: targetManager.id
    };
    this.spinnerService.show();
    this.employeeService.updateEmployee(employeeToMove).subscribe({
      next: () => {
        this.undoStack.push(undoAction);
        this.redoStack = [];
        this.reloadTreeAndSelectMovedEmployee(selectedEmployee.id);
      },
      error: error => this.handleMoveError(error)
    });
  }

  moveSelectedChildrenTo(manager: Employee): void {
    const selectedEmployee = this.selectedEmployee ? this.findEmployeeById(this.selectedEmployee.id) : null;
    const targetManager = this.findEmployeeById(manager.id);

    if (!selectedEmployee || !targetManager || !this.canMoveToManager(targetManager)) {
      return;
    }

    const undoAction: MoveChildrenUndoAction = {
      type: 'moveChildren',
      children: (selectedEmployee.children || []).map(child => ({
        employeeId: child.id,
        previousManagerId: this.getManagerId(child) || selectedEmployee.id,
        newManagerId: targetManager.id
      }))
    };

    this.spinnerService.show();
    this.employeeService.moveChildrenToManager(selectedEmployee.id, targetManager.id).subscribe({
      next: () => {
        if (undoAction.children.length > 0) {
          this.undoStack.push(undoAction);
          this.redoStack = [];
        }
        this.loadTree(true, selectedEmployee.id);
      },
      error: error => this.handleMoveError(error)
    });
  }

  undoLastHierarchyChange(): void {
    const undoAction = this.undoStack.pop();
    if (!undoAction || this.isUndoing) {
      return;
    }

    const undoRequest = this.createUndoRequest(undoAction);
    if (!undoRequest) {
      return;
    }

    this.isUndoing = true;
    this.spinnerService.show();
    undoRequest.subscribe({
      next: () => {
        // push original action into redo stack so it can be reapplied
        this.redoStack.push(undoAction);
        this.isUndoing = false;
        this.loadTree(true, this.getUndoSelectionId(undoAction));
      },
      error: error => {
        this.isUndoing = false;
        this.handleMoveError(error);
      }
    });
  }

  redoLastHierarchyChange(): void {
    const redoAction = this.redoStack.pop();
    if (!redoAction || this.isRedoing) {
      return;
    }

    const redoRequest = this.createRedoRequest(redoAction);
    if (!redoRequest) {
      return;
    }

    this.isRedoing = true;
    this.spinnerService.show();
    redoRequest.subscribe({
      next: () => {
        // after redoing, push it back to undo stack
        this.undoStack.push(redoAction);
        this.isRedoing = false;
        this.loadTree(true, this.getUndoSelectionId(redoAction));
      },
      error: error => {
        this.isRedoing = false;
        this.handleMoveError(error);
      }
    });
  }

  private reloadTreeAndSelectMovedEmployee(movedEmployeeId: string): void {
    this.searchInputText = '';
    this.searchText = '';
    this.loadTree(true, movedEmployeeId);
  }

  private createUndoRequest(undoAction: HierarchyUndoAction): Observable<Employee | Employee[]> | null {
    if (undoAction.type === 'moveEmployee') {
      return this.createMoveEmployeeUndoRequest(undoAction.employeeId, undoAction.previousManagerId);
    }

    const undoRequests = undoAction.children
      .map(child => this.createMoveEmployeeUndoRequest(child.employeeId, child.previousManagerId))
      .filter((request): request is Observable<Employee> => !!request);

    return undoRequests.length > 0 ? forkJoin(undoRequests) : null;
  }

  private createRedoRequest(redoAction: HierarchyUndoAction): Observable<Employee | Employee[]> | null {
    if (redoAction.type === 'moveEmployee') {
      return this.createMoveEmployeeUndoRequest(redoAction.employeeId, redoAction.newManagerId);
    }

    const redoRequests = redoAction.children
      .map(child => this.createMoveEmployeeUndoRequest(child.employeeId, child.newManagerId))
      .filter((request): request is Observable<Employee> => !!request);

    return redoRequests.length > 0 ? forkJoin(redoRequests) : null;
  }

  private createMoveEmployeeUndoRequest(employeeId: string, managerId: string | null): Observable<Employee> | null {
    const employee = this.findEmployeeById(employeeId);
    const manager = managerId ? this.findEmployeeById(managerId) : null;

    if (!employee || (managerId && !manager)) {
      return null;
    }

    return this.employeeService.updateEmployee({
      ...employee,
      manager,
      managerId,
      children: []
    } as Employee);
  }

  private getUndoSelectionId(undoAction: HierarchyUndoAction): string | null {
    if (undoAction.type === 'moveEmployee') {
      return undoAction.employeeId;
    }

    return undoAction.children[0]?.employeeId || null;
  }

  private getManagerId(employee: Employee): string | null {
    return employee.managerId || employee.manager?.id || null;
  }

  private isDescendantOf(possibleDescendant: Employee, possibleAncestor: Employee): boolean {
    return this.getDescendantIds(possibleAncestor).has(possibleDescendant.id);
  }

  private getDescendantIds(employee: Employee): Set<string> {
    const descendantIds = new Set<string>();
    const pendingEmployees = [...(employee.children || [])];

    while (pendingEmployees.length > 0) {
      const currentEmployee = pendingEmployees.pop();
      if (!currentEmployee) {
        continue;
      }
      descendantIds.add(currentEmployee.id);
      pendingEmployees.push(...(currentEmployee.children || []));
    }

    return descendantIds;
  }

  private refreshDisplayedTree(): void {
    const normalizedSearchText = this.searchText.trim().toLowerCase();
    const displayedEmployees = normalizedSearchText
      ? this.filterEmployees(this.allEmployees, normalizedSearchText)
      : this.allEmployees;

    this.dataSource.data = [...displayedEmployees];
    this.treeControl.dataNodes = displayedEmployees;

    if (normalizedSearchText) {
      setTimeout(() => this.expandDisplayedTree());
    }
  }

  private refreshLowerTree(): void {
    const normalizedSearchText = this.lowerSearchText.trim().toLowerCase();
    const managerEmployees = this.filterManagers(this.allEmployees);
    const displayedEmployees = normalizedSearchText
      ? this.filterEmployees(managerEmployees, normalizedSearchText)
      : managerEmployees;

    this.lowerDataSource.data = [...displayedEmployees];
    this.lowerTreeControl.dataNodes = displayedEmployees;

    if (normalizedSearchText) {
      setTimeout(() => this.expandLowerSearchResults(displayedEmployees, normalizedSearchText));
    }
  }

  private expandLowerSearchResults(employees: Employee[], normalizedSearchText: string): void {
    this.lowerTreeControl.collapseAll();

    const expandMatchingPaths = (currentEmployees: Employee[]): boolean => {
      let containsMatch = false;

      for (const employee of currentEmployees) {
        const employeeMatches = [employee.name, employee.firstName, employee.lastName]
          .some(value => value?.toLowerCase().includes(normalizedSearchText));
        const childContainsMatch = expandMatchingPaths(employee.children || []);

        if (employeeMatches || childContainsMatch) {
          this.lowerTreeControl.expand(employee);
          containsMatch = true;
        }
      }

      return containsMatch;
    };

    expandMatchingPaths(employees);
  }

  private filterManagers(employees: Employee[]): Employee[] {
    return employees.reduce<Employee[]>((managers, employee) => {
      if (employee.hasManagerRights !== true) {
        return managers;
      }

      managers.push({
        ...employee,
        children: this.filterManagers(employee.children || [])
      });
      return managers;
    }, []);
  }

  private getSelectedEmployee(): Employee | null {
    return this.selectedEmployee ? this.findEmployeeById(this.selectedEmployee.id) : null;
  }

  private expandEmployees(
    employees: Employee[],
    control: NestedTreeControl<Employee> = this.treeControl
  ): void {
    for (const employee of employees) {
      control.expand(employee);
      this.expandEmployees(employee.children || [], control);
    }
  }

  private expandToEmployee(employeeId: string): void {
    this.treeControl.collapseAll();
    const employeePath = this.findEmployeePath(employeeId, this.dataSource.data);
    for (const employee of employeePath) {
      this.treeControl.expand(employee);
    }
  }

  private findEmployeePath(employeeId: string, employees: Employee[]): Employee[] {
    for (const employee of employees) {
      if (employee.id === employeeId) {
        return [employee];
      }

      const childPath = this.findEmployeePath(employeeId, employee.children || []);
      if (childPath.length > 0) {
        return [employee, ...childPath];
      }
    }

    return [];
  }

  private filterEmployees(employees: Employee[], normalizedSearchText: string): Employee[] {
    return employees.reduce<Employee[]>((filteredEmployees, employee) => {
      const filteredChildren = this.filterEmployees(employee.children || [], normalizedSearchText);
      const employeeMatches = [employee.name, employee.firstName, employee.lastName]
        .some(value => value?.toLowerCase().includes(normalizedSearchText));

      if (employeeMatches || filteredChildren.length > 0) {
        filteredEmployees.push({
          ...employee,
          children: employeeMatches ? employee.children || [] : filteredChildren
        } as Employee);
      }

      return filteredEmployees;
    }, []);
  }

  private findEmployeeById(employeeId: string): Employee | null {
    const pendingEmployees = [...this.allEmployees];
    while (pendingEmployees.length > 0) {
      const employee = pendingEmployees.pop();
      if (!employee) {
        continue;
      }
      if (employee.id === employeeId) {
        return employee;
      }
      pendingEmployees.push(...(employee.children || []));
    }
    return null;
  }

  private handleMoveError(error: unknown): void {
    this.spinnerService.hide();
    alert('Failed to move employee in hierarchy: ' + this.getErrorMessage(error));
  }

  private getErrorMessage(error: any): string {
    return error?.error?.message || error?.message || 'Unknown error';
  }




}
