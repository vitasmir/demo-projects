import {
    HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { SpinnerService } from '../../service/spinner.service';

export const spinnerInterceptor: HttpInterceptorFn = (req, next) => {
    const spinnerService = inject(SpinnerService);
    spinnerService.show();

    return next(req).pipe(
        finalize(() => {
            spinnerService.hide();
        })
    );
};