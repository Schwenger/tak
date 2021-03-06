package interfaces.opponent

import ai.evaluation.{AbstractEvaluator, DefaultInterpolator}
import ai.players.{MinMaxPlayer, RandomPlayer}
import simulator.Player
import simulator.PlayerColor.PlayerColor

abstract class Opponent(val name: String, vit: Int, str: Int, int: Int, dex: Int) {
  val overall: Int = (vit + str + int + dex) / 4

  val empty = "\u2606"
  val full = "\u2605"
  val max = 5

  def pad(name: String) =
    name.padTo(Opponent.maxNameLength + 1, ' ')

  def characterization: String =
    s"${pad(name)} (VIT: ${prop(vit)}, STR: ${prop(str)}, INT: ${prop(int)}, DEX: ${prop(dex)}, ALL: ${prop(overall)})"

  private def prop(v: Int): String = full * v + empty * (max - v)

  def toPlayer(color: PlayerColor, boardSize: Int): Player

  protected def respEval: AbstractEvaluator = DefaultInterpolator.respective(this.str)

}

object Opponent {
  val all: List[Opponent] = List(
    Caterpillar, Auri, Wisely, LeftShark, Orc, Kvothe, Jesus, Paul
  ).sortBy(_.overall)
  def maxNameLength: Int = (all map (_.name.length)).max
}

object Caterpillar extends Opponent("The Very Hungry Caterpiller", 4, 1, 1, 1){
  override def toPlayer(color: PlayerColor, boardSize: Int) = new RandomPlayer(color, boardSize)
}
object Auri extends Opponent("Auri", 1, 1, 2, 5){
  override def toPlayer(color: PlayerColor, boardSize: Int) =
    new MinMaxPlayer(color, respEval(color), depth = 2, boardSize)
}
object Wisely extends Opponent("Wisely", 2, 1, 5, 3){
  override def toPlayer(color: PlayerColor, boardSize: Int) =
    new MinMaxPlayer(color, respEval(color), depth = 3, boardSize)
}
object LeftShark extends Opponent("Left Shark", 4, 3, 1, 2) {
  override def toPlayer(color: PlayerColor, boardSize: Int) =
    new MinMaxPlayer(color, respEval(color), depth = 2, boardSize)
}
object Orc extends Opponent("Orc", 4, 5, 1, 2){
  override def toPlayer(color: PlayerColor, boardSize: Int) =
    new MinMaxPlayer(color, respEval(color), depth = 2, boardSize)
}
object Kvothe extends Opponent("Kvothe", 3, 4, 5, 4){
  override def toPlayer(color: PlayerColor, boardSize: Int) =
    new MinMaxPlayer(color, respEval(color), depth = 3, boardSize)
}
object Jesus extends Opponent("Jesus H. Christ", 5, 5, 4, 5){
  override def toPlayer(color: PlayerColor, boardSize: Int) =
    new MinMaxPlayer(color, respEval(color), depth = 3, boardSize)
}
object Paul extends Opponent("Wolfgang J. Paul", 5, 5, 5, 5){
  override def toPlayer(color: PlayerColor, boardSize: Int) =
    new MinMaxPlayer(color, respEval(color), depth = 3, boardSize)
}
