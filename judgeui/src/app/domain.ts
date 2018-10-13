export type ContestId = String;
export type EliminationId = String;
export type HeatId = String;
export type RiderId = String;
export type JudgeId = String;
export type Points = Number;

export interface WaveScore {
    points: Points;
}

export enum JumpType {
    BackLoop = "BackLoop",
    FrontLoop = "FrontLoop",
    TableTop = "TableTop"
}

export let JumpTypes = [
    JumpType.BackLoop,
    JumpType.FrontLoop,
    JumpType.TableTop
]

export interface JumpScore {
    jumpType: JumpType;
    points: Points;
}

export interface ScoreSheet {
    waveScores: WaveScore[];
    jumpScores: JumpScore[];
}

export interface HeatState {
    heatId: HeatId;
    started: Boolean;
    ended: Boolean;
    scoreSheets?: Record<string /* RiderId */, ScoreSheet>;
    totalScores?: Record<string /* RiderId */, Points>;
    leader?: RiderId;
}

export interface Contestants {
    riderIds: RiderId[];
}

export interface Judge {
    id: JudgeId;
    name: String;
}

export interface Contest {
    id: ContestId;
    name: String;
}