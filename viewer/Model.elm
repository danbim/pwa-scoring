module Model exposing (..)

import Dict exposing (Dict)

type JumpType = BackLoop | FrontLoop | TableTop | UnknownJumpType

type alias HeatId      = String
type alias RiderId     = String
type alias Points      = Float

type alias WaveScore   = { points : Points }
type alias JumpScore   = { jumpType : JumpType, points: Points }
type alias ScoreSheet  = { waveScores : List WaveScore, jumpScores: List JumpScore }

type alias HeatLiveStreamRiderStates = Dict RiderId HeatLiveStreamRiderState

type alias HeatLiveStreamRiderState =
    { totalScore : Points
    , scoreSheet : ScoreSheet
    }

type alias HeatLiveStreamState =
    { heatId : HeatId
    , started : Bool
    , ended: Bool
    , riderStates : Maybe HeatLiveStreamRiderStates
    , leader : Maybe RiderId
    }