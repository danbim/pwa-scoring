import { Component } from '@angular/core';

import { ContestService } from '../contest.service'
import { OnInit } from '@angular/core';
import { HeatId, ContestId, EliminationId } from '../domain';
import { Observable } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'heat-selection',
    templateUrl: './heat-selection.component.html',
    styleUrls: ['./heat-selection.component.css'],
    providers: [ContestService]
})
export class HeatSelectionComponent implements OnInit {
    eliminationId: EliminationId;
    contestId: ContestId;
    heatIds: Observable<HeatId[]>;

    constructor(
        private route: ActivatedRoute,
        private contestService: ContestService
    ) { }

    ngOnInit() {
        this.eliminationId = this.route.snapshot.paramMap.get('eliminationId');
        this.contestId = this.route.snapshot.paramMap.get('contestId');
        this.heatIds = this.contestService.getHeatIds()
    }
}
