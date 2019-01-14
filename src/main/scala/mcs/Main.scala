package mcs

import cats.Show
import cats.data.StateT
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import mcs.Prng.Seed
import mcs.samegame.SameGame

object Main extends IOApp {
  private val position  = data.Games.jsGames10
  private val score     = SameGame.score(position)
  private val gameState = GameState(playedMoves = List.empty[samegame.Position], score = score, position = position)

  private implicit val showResult: Show[GameState[samegame.Position, samegame.Game, Int]] = Interpreters.showGameState

  private def putStrLn[T: Show](t: T): IO[Unit] = IO(println(show"$t"))

  val resultState: IO[Unit] = {
    implicit val logger: Logger[StateT[IO, SearchState[samegame.Position, samegame.Game, Int, Seed], ?]] =
      Interpreters.loggerState

    val interpreter  = Interpreters.gameState()
    val initialState = SearchState(Seed(23426347523L), gameState, None, None)
    for {
      result <- Programs.nestedMonteCarlo(2, 2, interpreter).runS(initialState)
      _      <- putStrLn(result.gameState)(Interpreters.showGameStateAsJsFunctionCalls)
    } yield ()
  }

  val resultIORef: IO[Unit] = {
    val initialState                = SearchState((), gameState, None, None)
    implicit val logger: Logger[IO] = Interpreters.loggerIORef
    for {
      interpreter <- Interpreters.gameIORef(initialState)
      result      <- Programs.nestedMonteCarlo(2, 2, interpreter) *> interpreter.gameState
      _           <- putStrLn(result)
    } yield ()
  }

  def run(args: List[String]): IO[ExitCode] =
    // resultIORef.as(ExitCode.Success)
    resultState.as(ExitCode.Success)
}
