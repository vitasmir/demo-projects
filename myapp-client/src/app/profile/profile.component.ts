import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, OnInit, Renderer2 } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import DataTable, { Config } from 'datatables.net-dt';
import { Observable } from 'rxjs';
import { AddUserComponent } from '../add-user/add-user.component';
import { UserService } from '../service/UserService';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../shared/confirmation-dialog';
import { User } from '../shared/model/User';

declare global {
  interface Window {
    context: any;
  }
}

@Component({
  selector: 'app-profile',
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, AfterViewInit {

  constructor(private userService: UserService, private dialog: MatDialog, private renderer: Renderer2) { }
  ngAfterViewInit(): void {
  }

  ngOnInit(): void {
    window.context = this;

    let dtOptions: Config = {
      pagingType: 'full_numbers',
      pageLength: 10,
      rowId: "id",
      /* select: {
         style: 'api',
         info: false,
       },*/
      columns: [
        { title: 'ID', data: 'id', visible: false },
        {
          title: 'First Name', data: 'firstName', orderable: true
        },
        {
          title: 'Last Name', data: 'lastName', orderable: true
        },
        { title: 'Username', data: 'username' },
        { title: 'Email', data: 'email' },
        { title: 'Age', data: 'age' },
        {
          title: 'Actions',
          orderable: false,
          data: null,
          render: (data: any, type: any, row: any) => {

            return '<button type="button" style="width: 100%" class="btn btn-primary editButton" name="editButton" onclick="window.context.editUser(\'' + data.id + '\')">EDIT</button>&nbsp;' +
              '<button type="button" style="width: 100%" class="btn btn-danger deleteButton" name="deleteButton" onclick="window.context.deleteUser(\'' + data.id + '\')">DELETE</button>';
          }
        }
      ],
      drawCallback: function () {
        // window.context.initializeDataTable();
      },

    };
    var table = new DataTable($('#userTable'), dtOptions);
    window.context.table = table;
    this.getUsers();
  }

  /*
    getUsers(): User[] {
      this.userService.getUsers().subscribe((res) => {
        return res;
      }, (error) => {
        alert(JSON.stringify("err=" + JSON.stringify(error)));
      });
      return [];
    }

      */

  editUser(u: string) {
    if (u != null) {
      let userObs: Observable<User> = this.userService.getUser(u);
      if (userObs != null) {
        userObs.subscribe({
          next: (user) => {
            const dialogRef = this.dialog.open(AddUserComponent, {
              width: '400px',
              disableClose: true,
              data: { mode: 'edit', data: user }
            });
            dialogRef.afterClosed().subscribe(
              (result) => {
                if (result && u) {
                  this.userService.updateUser(u, result).subscribe({
                    next: (updatedUser) => {
                      const dataArray = window.context.table.data();
                      for (let i = 0; i < dataArray.length; i++) {
                        if (dataArray[i].id === updatedUser.id) {
                          const row = dataArray.row(i);
                          row.data(updatedUser);
                          break;
                        }
                      }
                    },
                    error: (err: any) => alert('Failed to update user: ' + JSON.stringify(err))
                  });
                }
              }
            )
          },
          error: (err: any) => alert('Failed to get user: ' + JSON.stringify(err))
        });
      }
    }
  }

  getUsers(): User[] {
    let users: User[] = [];
    const t = window.context.table;
    this.userService.getUsers().subscribe({
      next: (res: User[]) => {
        t.rows.add(res);
        t.draw();
        users = users.concat(res);
      },
      error: (error: any) => {
        alert('Failed to get users: ' + JSON.stringify(error));
      }
    });
    return users;
  }


  addUser() {
    const dialogRef = this.dialog.open(AddUserComponent, {
      width: '400px',
      disableClose: true,
      data: { mode: 'add', data: {} }
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result === null || result.id === null) {
        return;
      }
      const user = new User(
        result.id,
        result.firstName,
        result.lastName,
        result.username,
        result.email,
        result.age
      );
      window.context.addUserData(user);
    })

  }

  updateUser(id: string, user: User) {
    const dialogRef = this.dialog.open(AddUserComponent, {
      data: {
        width: '400px',
        disableClose: true,
        mode: 'edit',
        data: user
      }
    });
    dialogRef.afterClosed().subscribe((editedUser) => {
      const table = window.context.table;
      const tableRows = table.rows().data();
      for (let i = 0; i < tableRows.length; i++) {
        if (tableRows[i].id === id) {
          tableRows[i] = editedUser;
          table.draw();
          break;
        }
      }
    })
  }

  deleteUser(id: string) {
    this.userService.deleteUser(id).subscribe({
      next: () => {
        const table = window.context.table;
        table.row("#" + id).remove().draw();
      },
      error: (error: any) => {
        alert('Failed to delete user: ' + JSON.stringify(error));
      }
    });
  }

  addUserData(u: User): void {
    this.userService.createUser(u).subscribe({
      next: (res: User) => {
        u.id = res.id;
        const t = window.context.table;
        t.row.add(u).draw();
      },
      error: (error: any) => alert('Failed to create user: ' + JSON.stringify(error))
    });
  }
}
