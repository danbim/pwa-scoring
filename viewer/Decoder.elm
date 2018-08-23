module Decoder exposing (..)

import Model exposing (..)
import Json.Decode exposing (..)

heatIdDecoder : Decoder HeatId
heatIdDecoder = string

riderIdDecoder : Decoder RiderId
riderIdDecoder = string

pointsDecoder : Decoder Points
pointsDecoder = float

stringToJumpType : String -> JumpType
stringToJumpType str =
    case String.toLower str of
        "backloop" -> BackLoop
        "frontloop" -> FrontLoop
        "tabletop" -> TableTop
        _ -> UnknownJumpType

jumpTypeDecoder : Decoder JumpType
jumpTypeDecoder =
    map stringToJumpType string

jumpScoreDecoder : Decoder JumpScore
jumpScoreDecoder =
    map2 JumpScore
        (field "jumpType" jumpTypeDecoder)
        (field "points" pointsDecoder)

waveScoreDecoder : Decoder WaveScore
waveScoreDecoder =
    map WaveScore
        (field "points" pointsDecoder)

scoreSheetDecoder : Decoder ScoreSheet
scoreSheetDecoder =
    map2 ScoreSheet
        (field "waveScores" (list waveScoreDecoder))
        (field "jumpScores" (list jumpScoreDecoder))

heatLiveStreamRiderStateDecoder : Decoder HeatLiveStreamRiderState
heatLiveStreamRiderStateDecoder =
    map2 HeatLiveStreamRiderState
        (field "totalScore" pointsDecoder)
        (field "scoreSheet" scoreSheetDecoder)

heatLiveStreamRiderStatesDecoder : Decoder HeatLiveStreamRiderStates
heatLiveStreamRiderStatesDecoder =
    dict heatLiveStreamRiderStateDecoder

heatLiveStreamStateDecoder : Decoder HeatLiveStreamState
heatLiveStreamStateDecoder =
    map5 HeatLiveStreamState
        (field "heatId" string)
        (field "started" bool)
        (field "ended" bool)
        (field "riderStates" (maybe heatLiveStreamRiderStatesDecoder))
        (field "leader" (maybe string))

decodeHeatLiveStreamStateFromJson : String -> Result String HeatLiveStreamState
decodeHeatLiveStreamStateFromJson =
    decodeString heatLiveStreamStateDecoder