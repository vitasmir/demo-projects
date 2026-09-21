import { inject } from '@angular/core';
import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../confirmation-dialog';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const dialog = inject(MatDialog);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unknown error occurred!';
      if (error.error instanceof ErrorEvent) {
        // Client-side errors
        errorMessage = `Error: ${error.error.message}`;
      } else {
        // Server-side errors
        const payload = error.error as { message?: string; error?: string; detail?: string; path?: string } | null;
        const detail = payload?.message ?? payload?.detail ?? payload?.error ?? error.statusText ?? error.message;
        const path = payload?.path ? `\nPath: ${payload.path}` : '';
        errorMessage = `Error Code: ${error.status}\nMessage: ${detail}${path}`;
      }

      dialog.open(ConfirmationDialogComponent, {
        data: {
          title: 'Error',
          message: errorMessage,
          confirmText: 'OK',
        },
        hasBackdrop: false,
      });

      return throwError(() => new Error(errorMessage));
    })
  );
};
