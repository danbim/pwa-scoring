import { ContestId, EliminationId, HeatId, JudgeId } from "../domain";
import { MatDialogRef, MAT_DIALOG_DATA } from "@angular/material";
import { Inject, Component } from "@angular/core";

@Component({
    selector: 'scoring-dialog',
    templateUrl: 'scoring-dialog.html'
})
export class ScoringDialog {

    constructor(
        public dialogRef: MatDialogRef<ScoringDialog>,
        @Inject(MAT_DIALOG_DATA) public data: ScoringDialogData
    ) { }

    onClickAbort(): void {
        this.dialogRef.close();
    }

}

export interface ScoringDialogData {

    waveOrJump: String;

    contestId: ContestId;
    eliminationId: EliminationId;
    heatId: HeatId;
    judgeId: JudgeId;
}