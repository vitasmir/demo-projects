import { Component, Inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ColorSketchModule } from 'ngx-color/sketch';
import { ColorEvent } from 'ngx-color';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-color-picker-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ColorSketchModule, MatButtonModule, MatDialogModule],
  templateUrl: './chat.colorpicker.component.html',
  styleUrls: ['./chat.colorpicker.component.scss']
  ,changeDetection: ChangeDetectionStrategy.OnPush
})
export class ColorPickerComponent {
  public color: string = '#ff0000';

  constructor(private dialogRef: MatDialogRef<ColorPickerComponent>,
              @Inject(MAT_DIALOG_DATA) public data: string | undefined,
              private cdr: ChangeDetectorRef) {
    if (data) {
      this.color = data;
    }
  }

  changeColorCompleted($event: ColorEvent): void {
    // Update color but DO NOT close on each mouse move/change event.
    // Closing will be performed explicitly via the "Use color" button (onSubmit).
    this.color = $event.color.hex;
    // mark for check since we use OnPush
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    this.dialogRef.close(this.color);
  }

  onClose(): void {
    this.dialogRef.close(null);
  }
}
