import { Component, Inject, ChangeDetectorRef } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { Todo } from '../shared/model/Todo';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AddTodoComponent } from './add-todo/add-todo.component';
import { CommonModule, NgIf } from '@angular/common'
@Component({
  selector: 'app-todo-list',
  imports: [CommonModule],
  templateUrl: './todo-list.component.html',
  styleUrl: './todo-list.component.scss'
})

export class TodoListComponent {
  onEdit(id: string) {
    for (let i = 0; i < this.todos.length; i++) {
      let item = this.todos[i];
      if (item.id == id) {
        const matDialogRef = this.dialog.open(AddTodoComponent, {
          width: '400px',
          disableClose: true,
          data: { ...item } // Pass a copy to avoid modifying original
        });

        matDialogRef.afterClosed().subscribe((result) => {
          if (result) {
            this.todos = this.todos.map(todo => todo.id === result.id ? result : todo);
            this.cdr.detectChanges();
          }
        });
        break;
      }
    }
  }

  constructor(private dialog: MatDialog, private cdr: ChangeDetectorRef) { }



  addTodo() {
    const matDialogRef = this.dialog.open(AddTodoComponent, {
      width: '400px',
      disableClose: true,
    });

    matDialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.todos.push(result);
        this.cdr.detectChanges();
      }
    });
  };



  showDivs() {
    this.divs = true;
  }
  showTable() {
    this.divs = false
  }
  divs: boolean = true;

  onDelete(todoId: string) {
    this.todos = this.todos.filter(todo => todo.id !== todoId);
    this.cdr.detectChanges();
  }



  public todos: Todo[] =
    [
      new Todo("TODO: Mow lawn 1", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 2", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 3", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 4", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 5", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 6", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 7", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 8", "Don't forget to mow a lawn", uuidv4()),
      new Todo("TODO: Mow lawn 9", "Don't forget to mow a lawn", uuidv4())
    ];

};



