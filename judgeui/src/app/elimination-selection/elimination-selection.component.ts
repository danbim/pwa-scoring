import { Component } from '@angular/core';
import { ContestId } from '../domain';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-elimination-selection',
  templateUrl: './elimination-selection.component.html',
  styleUrls: ['./elimination-selection.component.css']
})
export class EliminationSelectionComponent {
  contestId: ContestId;

  constructor(
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.contestId = this.route.snapshot.paramMap.get('contestId');
  }
}

