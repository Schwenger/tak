package ai.search
import ai.evaluation.Evaluator
import simulator.GameState
import simulator.elements.Action
import simulator.PlayerColor.PlayerColor
import simulator.logic.ActionExecutor

import scala.util.Random

sealed trait Comp {
  def apply[R](l: Seq[R])(implicit order: Ordering[R]): R
  def unary_!(): Comp
}
object Min extends Comp {
  def apply[R](l: Seq[R])(implicit order: Ordering[R]) = l.min(order)
  def unary_!() = Max
}
object Max extends Comp {
  def apply[R](l: Seq[R])(implicit order: Ordering[R]) = l.max(order)
  def unary_!() = Min
}

object MinMax extends SearchStrategy {

  case class Node(from: Option[Action], prefAction: Option[Action], value: Double)

  /**
    * Applies the MinMax search strategy on the given state.
    * Returns the best action with respect to maximizing the eval function.
    *
    * @param state the state for which the best action is supposed to be computed
    * @param eval estimates the quality of a state. Should always result positive values.
    * @param actionSupplier supplies applicable actions for a given state and color.
    * @param depth depth 1 means only the immediately reachable states are taken into account
    * @param maxPlayer the player whose turn it is
    * @return best state w.r.t maximizing eval
    */
  override def apply(state: GameState, eval: Evaluator, actionSupplier: (GameState, PlayerColor) => Seq[Action],
                     depth: Int, maxPlayer: PlayerColor): Action = {

    implicit val order = Ordering.by[Node, Double](_.value)
    assert(depth > 0)
    /*
    Recursion step; calls itself until depth becomes 0.
    States are then evaluated and the maximizing/minimizing nodes gets passed upwards
    such that the best action w.r.t. eval for maxPlayer can be returned.
     */
    def run(state: GameState, lastAction: Option[Action], depth: Int, best: Comp, color: PlayerColor): Node = {
      assert(depth > 0 || lastAction.isDefined)
      lazy val availableActions = Random.shuffle(actionSupplier(state, color)) // we shuffle to make sure the supplier's order is irrelevant
      if (depth == 0 || availableActions.isEmpty) {
        Node(lastAction, None, eval(state))
      } else {
        def expandState(state: GameState, action: Action): GameState = ActionExecutor(action, state, color)
        def expandAction(current: Action) = run(expandState(state, current), Some(current), depth - 1, !best, !color)
        val expanded: Seq[Node] = availableActions map expandAction
        val opt = best(expanded)
        Node(from = lastAction, prefAction = opt.from, opt.value)
      }
    }
    run(state, None, depth, Max, maxPlayer).prefAction.get // cannot be empty since we assert depth > 0
  }
}
