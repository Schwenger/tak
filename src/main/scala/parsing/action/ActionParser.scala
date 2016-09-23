package parsing.action

import scala.util.Try

import simulator.interfaces.game_elements.{Action => GameAction}

object ActionParser {
  def apply(cmd: String): Try[GameAction] = {
    Lexer(cmd) flatMap (Parser(_)) flatMap (Compiler(_))
  }

}

