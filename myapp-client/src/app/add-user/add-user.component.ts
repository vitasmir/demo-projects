import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule, NgForm } from '@angular/forms';
import { User } from '../shared/model/User';
import { v4 as uuidv4 } from 'uuid';


@Component({
  selector: 'app-add-user',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule],
  templateUrl: './add-user.component.html',
  styleUrls: ['./add-user.component.scss']
})
export class AddUserComponent {

  userObject: any;


  onSubmit(userForm: NgForm) {
    this.onSave();
  }

  constructor(
    private dialogRef: MatDialogRef<AddUserComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) {
    this.userObject = data.data;
  }

  onSave() {
    this.dialogRef.close(this.userObject as User);
  }

  onCancel() {
    this.dialogRef.close();
  }
}
