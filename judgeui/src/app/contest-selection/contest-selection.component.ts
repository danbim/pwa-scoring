import { Component } from '@angular/core';
import { Contest } from '../domain';
import { ContestService } from '../contest.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-contest-selection',
  templateUrl: './contest-selection.component.html',
  styleUrls: ['./contest-selection.component.css']
})
export class ContestSelectionComponent {
  contests: Observable<Contest[]>;
  
  constructor(private contestService: ContestService) {}

  ngOnInit() {
      this.contests = this.contestService.getContests();
  }
}

