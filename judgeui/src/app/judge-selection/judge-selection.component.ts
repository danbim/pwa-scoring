import { Component } from '@angular/core';
import { HeatId, Judge, ContestId, EliminationId } from '../domain';
import { ActivatedRoute } from '@angular/router';
import { ContestService } from '../contest.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-judge-selection',
  templateUrl: './judge-selection.component.html',
  styleUrls: ['./judge-selection.component.css']
})
export class JudgeSelectionComponent {
  contestId: ContestId;
  eliminationId: EliminationId;
  heatId: HeatId;
  judges: Observable<Judge[]>;

  constructor(
    private route: ActivatedRoute,
    private contestService: ContestService
  ) { }

  ngOnInit() {
    this.contestId = this.route.snapshot.paramMap.get('contestId');
    this.eliminationId = this.route.snapshot.paramMap.get('eliminationId');
    this.heatId = this.route.snapshot.paramMap.get('heatId');
    this.judges = this.contestService.getJudges(this.heatId);
  }
}

