import { Component, OnInit, Input, OnDestroy, Output, ChangeDetectorRef } from '@angular/core';
import { Subscription, Observable } from "rxjs";
import { HeatService } from '../heat-service';
import { HeatId, HeatState, ScoreSheet } from '../domain';

@Component({
    selector: 'heat-state',
    templateUrl: './heat-state.component.html',
    providers: [
        HeatService
    ]
})
export class HeatStateComponent {

    @Input() heatId: HeatId;
    @Input() heatState: HeatState;

    renderWaveScores(scoreSheet: ScoreSheet): String {
        return scoreSheet.waveScores.map(s => s.points).join(', ');
    }

    renderJumpScores(scoreSheet: ScoreSheet): String {
        return scoreSheet.jumpScores.map(s => s.points + ' (' + s.jumpType + ')').join(', ');
    }
}