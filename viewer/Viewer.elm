module Viewer exposing (..)

import Html exposing (div, text, Html, program, table, tr, td, br, b)
import Model exposing (..)
import Dict exposing (Dict)
import Decoder exposing (decodeHeatLiveStreamStateFromJson)

import Bootstrap.CDN as CDN
import Bootstrap.Grid as Grid
import Bootstrap.Table as Table

json = """
{
  "ended": false,
  "leader": "GER-123",
  "riderStates": {
    "GER-123": {
      "totalScore": 20.9,
      "scoreSheet": {
        "waveScores": [
          {
            "points": 9.9
          }
        ],
        "jumpScores": [
          {
            "jumpType": "backloop",
            "points": 7.6
          },
          {
            "jumpType": "tabletop",
            "points": 3.4
          }
        ]
      }
    },
    "GER-321": {
      "totalScore": 14.1,
      "scoreSheet": {
        "waveScores": [
          {
            "points": 8.7
          }
        ],
        "jumpScores": [
          {
            "jumpType": "backloop",
            "points": 5.4
          }
        ]
      }
    }
  },
  "heatId": "1-A",
  "started": true
}
"""

type Msg         = None
type alias Model = Result String HeatLiveStreamState

viewScoreSheet : ScoreSheet -> Html Msg
viewScoreSheet scoreSheet =
    let
        waveScoreToHtml = \ws -> text (toString ws.points)
        jumpScoreToHtml = \js -> text ((toString js.points) ++ "(" ++ (toString js.jumpType) ++ ")")
        waveScoresHtml = List.map waveScoreToHtml scoreSheet.waveScores
        jumpScoresHtml = List.map jumpScoreToHtml scoreSheet.jumpScores
    in
        div [] (waveScoresHtml ++ jumpScoresHtml)

viewRiderState: (RiderId, HeatLiveStreamRiderState) -> Html Msg
viewRiderState (riderId, riderState) =
    text (riderId ++ " => " ++ (toString riderState.totalScore))

viewRiderStates: HeatLiveStreamRiderStates -> List (Html Msg)
viewRiderStates riderStates =
    let
        toRow = \(riderId, riderState) ->
            Grid.row []
                    [ Grid.col [] [ (viewRiderState (riderId, riderState)) ] ]
    in
        List.map toRow (Dict.toList riderStates)

emptyRiderStates: List (Html Msg)
emptyRiderStates =
    [ Grid.row []
        [ Grid.col []
            [ text "..."
            ]
        ]
    ]

viewState : HeatLiveStreamState -> Html Msg
viewState state =
    let
        heatIdRow =
            Grid.row []
                [ Grid.col []
                    [ b [] [ (text state.heatId) ] ]
                ]
        riderStatesRows =
            (state.riderStates
                |> Maybe.map viewRiderStates
                |> Maybe.withDefault emptyRiderStates
            )
        headers =
            [ heatIdRow
            , (Grid.row []
                [ Grid.col [] [ text "Jumps" ]
                , Grid.col [] [ text "Waves" ]
                , Grid.col [] [ text "Total" ]
                , Grid.col [] [ text "Posn." ]
                ])
            ]

    in
        Grid.container [] (List.append headers riderStatesRows)

view : Model -> Html Msg
view model =
    case model of
        Ok state -> viewState state
        Err errorMsg -> text errorMsg

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    ( model, Cmd.none )

init : (Model, Cmd Msg)
init =
    case decodeHeatLiveStreamStateFromJson json of
        Ok heatState -> ( Ok heatState, Cmd.none )
        Err errorMsg -> ( Err errorMsg, Cmd.none )

main : Program Never Model Msg
main =
    program
        { init = init
        , view = view
        , update = update
        , subscriptions = \_ -> Sub.none
        }