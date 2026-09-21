import { Routes } from '@angular/router';
import { DataBindingComponent } from './data-binding/data-binding.component';
import { DataTables2Component } from './data-tables2/data-tables2.component';
import { DataTables3Component } from './data-tables3/data-tables3.component';
import { EmployeeBackupComponent } from './employee-backup/employee-backup.component';
import { ProfileComponent } from './profile/profile.component';
import { TodoListComponent } from './todo-list/todo-list.component';
import { TreesOfEmployeesComponent } from './trees-of-employees/trees-of-employees.component';
import { WebsocketsChatComponent } from './websockets-chat-component/websockets-chat-component';
import { StocksComponent } from './stocks/stocks.component';
import { BrokerComponent } from './broker/broker.component';
import { BoatRentalCalendarComponent } from './boat-rental/boat-rental-calendar.component';
import { BoatRentalAdminComponent } from './boat-rental/boat-rental-admin.component';
import { TaskManagementComponent } from './task-management/task-management.component';
import { ProjectManagementComponent } from './task-management/pages/project-management-page/project-management.component';
import { AccountManagementComponent } from './task-management/pages/account-management-page/account-management.component';
import { ProjectAccessManagementComponent } from './task-management/pages/project-access-management-page/project-access-management.component';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
    { path: 'datatable1', component: DataTables2Component, canActivate: [authGuard] },
    { path: 'datatable2', component: DataTables3Component, canActivate: [authGuard] },
    { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
    { path: 'todo-list', component: TodoListComponent, canActivate: [authGuard] },
    { path: 'data-binding', component: DataBindingComponent, canActivate: [authGuard] },
    { path: 'trees-of-employees', component: TreesOfEmployeesComponent, canActivate: [authGuard] },
    { path: 'employee-backup', component: EmployeeBackupComponent, canActivate: [authGuard] },
    { path: 'chat', component: WebsocketsChatComponent, canActivate: [authGuard] },
    { path: 'akcie', component: StocksComponent, canActivate: [authGuard] },
    { path: 'broker', component: BrokerComponent, canActivate: [authGuard] },
    { path: 'boat-rental', component: BoatRentalCalendarComponent, canActivate: [authGuard] },
    { path: 'boat-rental-admin', component: BoatRentalAdminComponent, canActivate: [authGuard] },
 /*   { path: 'projects', redirectTo: 'task-management/projects', pathMatch: 'full' }, */
    { path: 'task-management/accounts', component: AccountManagementComponent },
    {
        path: 'task-management',
        canActivate: [authGuard],
        children: [
            { path: '', component: TaskManagementComponent },
            { path: 'projects', component: ProjectManagementComponent },
            { path: 'project-access', component: ProjectAccessManagementComponent }
        ]
    },
    { path: '', redirectTo: 'datatable1', pathMatch: 'full' }
];
