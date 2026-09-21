import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SpinnerService } from '../../service/spinner.service';
import { Observable } from 'rxjs';
@Component({
    selector: 'app-spinner',
    standalone: true,
    imports: [CommonModule, MatProgressSpinnerModule],
    templateUrl: './spinner.component.html',
    styleUrls: ['./spinner.component.css'],
})
export class SpinnerComponent {
    isLoading$: Observable<boolean>;
    constructor(private spinnerService: SpinnerService) {
        this.isLoading$ = this.spinnerService.isLoading;
    }
}