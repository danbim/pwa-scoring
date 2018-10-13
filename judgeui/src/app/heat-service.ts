import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { HeatId, RiderId, WaveScore, JumpScore, HeatState, Contestants } from './domain';
import { WaveScoredEvent, JumpScoredEvent } from './events';

const httpOptions = {
    headers: new HttpHeaders({
        'Content-Type': 'application/json'
    })
}

@Injectable({
    providedIn: 'root'
})
export class HeatService {

    constructor(
        private httpClient: HttpClient
    ) { }

    getRiderIds(heatId: HeatId): Observable<RiderId[]> {
        let url = `http://localhost:8080/contest/heats/${heatId}/contestants`;
        console.log("Retrieving contestants from ", url);
        return this.httpClient
            .get<Contestants>(url)
            .pipe(map(contestants => contestants.riderIds))
            .pipe(catchError(this.handleError));
    }

    scoreWave(heatId: HeatId, riderId: RiderId, waveScore: WaveScore): Observable<WaveScoredEvent> {
        let url = `http://localhost:8080/contest/heats/${heatId}/waveScores/${riderId}`;
        console.log("Submitting wave score ", waveScore, " to ", url);
        return this.httpClient
            .post<WaveScoredEvent>(url, waveScore, httpOptions)
            .pipe(catchError(this.handleError));
    }

    scoreJump(heatId: HeatId, riderId: RiderId, jumpScore: JumpScore): Observable<JumpScoredEvent> {
        let url = `http://localhost:8080/contest/heats/${heatId}/jumpScores/${riderId}`;
        console.log("Submitting jump score ", jumpScore, " to ", url);
        return this.httpClient
            .post<JumpScoredEvent>(url, jumpScore, httpOptions)
            .pipe(catchError(this.handleError));
    }

    heatStateStream(heatId: HeatId): Observable<HeatState> {
        return Observable.create(observer => {
            const eventSource = new EventSource(`http://localhost:8080/contest/heats/${heatId}`);
            eventSource.onmessage = x => {
                if (x.data != '') {
                    let heatState: HeatState = JSON.parse(x.data);
                    observer.next(heatState);
                }
            }
            eventSource.onerror = x => {
                observer.error(x);
            }

            return () => {
                eventSource.close();
            };
        });
    }

    startHeat(heatId: HeatId): Observable<void> {
        let url = `http://localhost:8080/contest/heats/${heatId}?startHeat=true`
        console.log("Starting heat ", heatId)
        return this.httpClient
            .put<void>(url, "")
            .pipe(catchError(this.handleError))
    }

    endHeat(heatId: HeatId): Observable<void> {
        let url = `http://localhost:8080/contest/heats/${heatId}?endHeat=true`
        console.log("Ending heat ", heatId)
        return this.httpClient
            .put<void>(url, null)
            .pipe(catchError(this.handleError))
    }

    private handleError(error: HttpErrorResponse) {
        if (error.error instanceof ErrorEvent) {
            // A client-side or network error occurred. Handle it accordingly.
            console.error('An error occurred:', error.error.message);
        } else {
            // The backend returned an unsuccessful response code.
            // The response body may contain clues as to what went wrong,
            console.error(
                `Backend returned code ${error.status}, ` +
                `body was: `, error.error);
        }
        // return an observable with a user-facing error message
        return throwError(
            'Something bad happened; please try again later.');
    };
}
