import { Component, Inject, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Todo } from '../../shared/model/Todo';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CommonModule, NgIf } from '@angular/common';
import { v4 as uuidv4 } from 'uuid';


@Component({
  selector: 'app-add-todo',
  imports: [FormsModule, CommonModule],
  templateUrl: './add-todo.component.html',
  styleUrl: './add-todo.component.scss'
})
export class AddTodoComponent implements OnInit {
  constructor(private dialogRef: MatDialogRef<AddTodoComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Todo) {

  }

  todoObject: Todo = { id: "", title: "", note: "" }

  ngOnInit() {
    if (this.data) {
      this.todoObject = { ...this.data }; // Clone the data to avoid direct mutation
    }
  }

  onSubmit(todoForm: NgForm) {
    if (!this.todoObject.id) {
      this.todoObject.id = uuidv4();
    }
    this.dialogRef.close(this.todoObject);
    return this.todoObject;
  }

  onClose() {
    this.dialogRef.close();
  }



}
