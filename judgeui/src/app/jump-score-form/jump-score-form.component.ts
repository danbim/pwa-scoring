import { Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { HeatId, RiderId, JumpType, JumpTypes, Points } from '../domain';
import { HeatService } from '../heat-service';
import { Observable } from 'rxjs';

@Component({
    selector: 'jump-score-form',
    templateUrl: './jump-score-form.component.html',
    styleUrls: ['./jump-score-form.component.css'],
    providers: [ HeatService ]
})
export class JumpScoreFormComponent implements OnInit {

    @Input() heatId: HeatId;

    riderIds: Observable<RiderId[]>;
    jumpTypes: JumpType[];
    jumpScoreForm: FormGroup;

    constructor(private heatService: HeatService) {}

    ngOnInit() {
        this.riderIds = this.heatService.getRiderIds(this.heatId);
        this.jumpTypes = JumpTypes;
        this.jumpScoreForm = new FormGroup({
            riderId: new FormControl('', [
                Validators.required
            ]),
            jumpType: new FormControl('', [
                Validators.required
            ]),
            points: new FormControl('0', [
                Validators.required,
                Validators.min(0),
                Validators.max(12)
            ])
        });
    }

    get riderId(): RiderId {
        return this.jumpScoreForm.get('riderId').value;
    }

    get jumpType(): JumpType {
        return this.jumpScoreForm.get('jumpType').value;
    }

    get points(): Points {
        return +(this.jumpScoreForm.get('points').value);
    }

    get jumpScore() {
        return {
            jumpType : this.jumpType,
            points: this.points
        }
    }

    get createUUID() {
        return "test";
    }

    onSubmit() {
        this.heatService
            .scoreJump(this.heatId, this.riderId, this.jumpScore)
            .subscribe(_ => console.log(`Jump score submitted`));
    }
}
