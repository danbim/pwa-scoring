import { Component, OnInit, ChangeDetectorRef, OnDestroy } from "@angular/core";
import { HeatId, JudgeId, ContestId, EliminationId, HeatState } from "../domain";
import { ActivatedRoute } from "@angular/router";
import { MatDialog, MatDialogModule } from "@angular/material";
import { ScoringDialogData, ScoringDialog } from "./scoring-dialog";
import { Subscription, Observable } from "rxjs";
import { HeatService } from "../heat-service";
import { map } from 'rxjs/operators';

@Component({
    selector: 'heat',
    templateUrl: './heat.component.html',
    styleUrls: ['./heat.component.css'],
    providers: [MatDialogModule]
})
export class HeatComponent implements OnInit, OnDestroy {

    contestId: ContestId;
    eliminationId: EliminationId;
    heatId: HeatId;
    judgeId: JudgeId;

    heatState?: HeatState;
    heatStateStreamSubscription?: Subscription;

    constructor(
        private route: ActivatedRoute,
        public dialog: MatDialog,
        private heatService: HeatService,
        private ref: ChangeDetectorRef
    ) { }

    ngOnInit() {
        
        this.contestId = this.route.snapshot.paramMap.get('contestId');
        this.eliminationId = this.route.snapshot.paramMap.get('eliminationId');
        this.heatId = this.route.snapshot.paramMap.get('heatId');
        this.judgeId = this.route.snapshot.paramMap.get('judgeId');

        let self = this;
        this.heatStateStreamSubscription = this.heatService
            .heatStateStream(this.heatId)
            .subscribe({
                next: state => {
                    self.heatState = state;
                    self.ref.detectChanges();
                },
                error: err => {
                    console.error('Error on heatStateStream: ', err)
                }
            });
    }

    ngOnDestroy() {
        //this.heatStateStreamSubscription.unsubscribe();
    }

    heatNotStarted(): Boolean {
        return !this.heatState.started;
    }

    heatRunning(): Boolean {
        return this.heatState.started && !this.heatState.ended;
    }

    heatFinished(): Boolean {
        return this.heatState.started && this.heatState.ended;
    }

    startHeat(): void {
        this.heatService.startHeat(this.heatId).subscribe();
    }

    endHeat(): void {
        this.heatService.endHeat(this.heatId).subscribe();
    }

    openWaveScoringDialog(): void {
        this.openScoringDialog('wave');
    }

    openJumpScoringDialog(): void {
        this.openScoringDialog('jump');
    }

    openScoringDialog(waveOrJump: String): void {
        let data: ScoringDialogData = {
            waveOrJump: waveOrJump,
            contestId: this.contestId,
            eliminationId: this.eliminationId,
            heatId: this.heatId,
            judgeId: this.judgeId
        }
        const dialogRef = this.dialog.open(ScoringDialog, {
            data: data
        });

        dialogRef.afterClosed().subscribe(result => {
            console.log('The dialog was closed with result', result);
        });
    }
}
