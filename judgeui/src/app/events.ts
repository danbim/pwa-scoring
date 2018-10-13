import { WaveScore, JumpScore, HeatId, RiderId } from "./domain";

export interface WaveScoredEvent {
    heatId: HeatId;
    riderId: RiderId;
    waveScore: WaveScore;
}

export interface JumpScoredEvent {
    heatId: HeatId;
    riderId: RiderId;
    jumpScore: JumpScore;
}