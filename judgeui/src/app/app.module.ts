import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { MatGridListModule } from '@angular/material/grid-list';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AppComponent } from './app.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { HeatSelectionComponent } from './heat-selection/heat-selection.component';
import { WaveScoreFormComponent } from './wave-score-form/wave-score-form.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HeatComponent } from './heat/heat.component';
import { JumpScoreFormComponent } from './jump-score-form/jump-score-form.component';
import { HeatStateComponent } from './heat-state/heat-state.component';
import { NavigationComponent } from './navigation/navigation.component';
import { LayoutModule } from '@angular/cdk/layout';
import { MatToolbarModule, MatButtonModule, MatSidenavModule, MatIconModule, MatListModule, MatCardModule, MatMenuModule, MatDialog, MatDialogModule } from '@angular/material';
import { JudgeSelectionComponent } from './judge-selection/judge-selection.component';
import { ContestSelectionComponent } from './contest-selection/contest-selection.component';
import { EliminationSelectionComponent } from './elimination-selection/elimination-selection.component';
import { ScoringDialog } from './heat/scoring-dialog';

const appRoutes: Routes = [
  { path: '', redirectTo: 'contests', pathMatch: 'full' },
  { path: 'contests', component: ContestSelectionComponent },
  { path: 'contests/:contestId/eliminations', component: EliminationSelectionComponent },
  { path: 'contests/:contestId/eliminations/:eliminationId/heats', component: HeatSelectionComponent },
  { path: 'contests/:contestId/eliminations/:eliminationId/heats/:heatId/judges', component: JudgeSelectionComponent },
  { path: 'contests/:contestId/eliminations/:eliminationId/heats/:heatId/judges/:judgeId', component: HeatComponent },
  { path: '**', component: PageNotFoundComponent }
];

@NgModule({
  declarations: [
    AppComponent,
    HeatComponent,
    PageNotFoundComponent,
    HeatSelectionComponent,
    WaveScoreFormComponent,
    JumpScoreFormComponent,
    HeatStateComponent,
    NavigationComponent,
    JudgeSelectionComponent,
    ContestSelectionComponent,
    EliminationSelectionComponent,
    ScoringDialog
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    RouterModule.forRoot(
      appRoutes,
      // { enableTracing: true } // <-- debugging purposes only
    ),
    BrowserAnimationsModule,
    MatInputModule,
    MatSelectModule,
    MatGridListModule,
    LayoutModule,
    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatIconModule,
    MatListModule,
    MatCardModule,
    MatMenuModule,
    MatDialogModule
  ],
  entryComponents: [
    ScoringDialog
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
