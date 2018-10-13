import { HeatId, Judge, Contest } from './domain';
import { Injectable, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
    providedIn: 'root'
})
export class ContestService {

    constructor(private httpClient: HttpClient) {}

    getContests(): Observable<Contest[]> {
        // TODO retrieve contests from server
        return Observable.create(observer => {
            observer.next([
                { id: "1", name: "2018 Morocco" },
                { id: "2", name: "2018 Gran Canaria" },
                { id: "3", name: "2018 Fuerteventura"},
                { id: "4", name: "2018 Tenerife" },
                { id: "5", name: "2018 Sylt" }
            ]);
        });
    }

    getHeatIds(): Observable<HeatId[]> {
        let url = `http://localhost:8080/contest/heats`;
        console.log("Requesting heatIds from ", url);
        return this.httpClient
            .get<HeatId[]>(url)
            .pipe(catchError(this.handleError))
    }

    getJudges(heatId: HeatId): Observable<Judge[]> {
        // TODO retrieve judges from server
        return Observable.create(observer => {
            observer.next([
                { id: "1", name: "Daniel" },
                { id: "2", name: "Rich" },
                { id: "3", name: "Kurosh"},
                { id: "4", name: "Graham" }
            ]);
        });
    }

    // TODO code-duplication with heat-service.ts
    private handleError(error: HttpErrorResponse) {
        if (error.error instanceof ErrorEvent) {
            // A client-side or network error occurred. Handle it accordingly.
            console.error('An error occurred:', error.error.message);
        } else {
            // The backend returned an unsuccessful response code.
            // The response body may contain clues as to what went wrong,
            console.error(
                `Backend returned code ${error.status}, ` +
                `body was: ${error.error}`);
        }
        // return an observable with a user-facing error message
        return throwError(
            'Something bad happened; please try again later.');
    };
}
