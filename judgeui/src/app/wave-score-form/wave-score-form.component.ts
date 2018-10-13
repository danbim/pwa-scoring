import { Component, Input, OnInit, Inject } from '@angular/core';
import { FormControl, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ContestId, HeatId, RiderId, WaveScore, Points } from '../domain';
import { HeatService } from '../heat-service';
import { Observable } from 'rxjs';

@Component({
    selector: 'wave-score-form',
    templateUrl: './wave-score-form.component.html',
    styleUrls: ['./wave-score-form.component.css'],
    providers: [ HeatService ]
})
export class WaveScoreFormComponent implements OnInit {

    @Input() heatId: HeatId;

    riderIds: Observable<RiderId[]>;
    waveScoreForm: FormGroup;

    constructor(private heatService: HeatService) {}

    ngOnInit() {
        this.riderIds = this.heatService.getRiderIds(this.heatId);
        this.waveScoreForm = new FormGroup({
            riderId: new FormControl('', [
                Validators.required
            ]),
            points: new FormControl('0.0', [
                Validators.required,
                Validators.min(0),
                Validators.max(12)
            ])
        });
    }

    get riderId(): AbstractControl {
        return this.waveScoreForm.get('riderId');
    }

    get points(): AbstractControl {
        return this.waveScoreForm.get('points');
    }

    get waveScore(): WaveScore {
        return {
            // TODO this here return null if given a comma-separated number instead of dot-separated
            points : +(this.points.value)
        }
    }

    onSubmit() {
        this.heatService
            .scoreWave(this.heatId, this.riderId.value, this.waveScore)
            .subscribe(_ => console.log(`Wave score submitted`));
    }
}
