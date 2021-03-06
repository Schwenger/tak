package simulator.logic

import simulator.GameState
import simulator.elements.Direction.Direction
import simulator.elements._
import simulator.PlayerColor.PlayerColor


object ActionExecutor {
  /**
    * Executes a given action in the given state.
    * @param action to be executed
    * @param old_state in which the action is executed
    * @param player who takes the action
    * @return the state after the execution
    */
  // NOTE: The action executor always copies the whole board which is extremely inefficient.
  // TODO: when handing a state to the player, record checksum or similar to avoid spurious behavior
  // When changing this: adapt tests
  def apply(action: Action, old_state: GameState, player: PlayerColor): GameState = {
    assert(ActionValidator(old_state, action)(player))
    val state = old_state.copy
    action match {
      case PlaceCapstone(pos) =>
        executePlace(state, pos, minion = false, Capstone(player))
      case PlaceMinion(pos) =>
        executePlace(state, pos, minion = true, Minion(player))
      case PlaceWall(pos) =>
        executePlace(state, pos, minion = true, Wall(player))
      case Slide(src, stones, dir) =>
        executeSlide(state, src, stones, dir)
      case Move(src, dir) =>
        executeMove(state, src, dir)
      case Surrender(_) =>
        state.surrender(player)
    }
    state
  }

  @inline private def executePlace(state: GameState, pos: Position, minion: Boolean, token: Token) = {
    state.removeToken(token.player, minion)
    state.setField(pos, token)
  }

  @inline private def executeMove(state: GameState, src: Position, dir: Direction) = {
    assert(state(src).isDefined)
    val res = _merge(state(src).get, state(dir(src)))
    state.setField(dir(src), res)
    state.clearField(src)
  }

  private def executeSlide(state: GameState, src: Position, stones: List[Int], dir: Direction) = state(src) match {
    case Some(Stack(content)) =>
      val stack = content
      state.setField(src, Tokenizer(stack.drop(stones.head)))
      _executeSlide(state, dir(src), stones.tail, dir, stack.take(stones.head))
    case _ => assert(false)
  }

  private def _executeSlide(state: GameState, placeAt: Position, stones: List[Int], dir: Direction, inHand: List[Token]): Unit = stones match {
    case x :: xs =>
      val newToken = _merge(Tokenizer(inHand.drop(x)).get, state(placeAt))
      state.setField(placeAt, newToken)
      _executeSlide(state, dir(placeAt), xs, dir, inHand.take(x))
    case Nil =>
      val newToken = _merge(Tokenizer(inHand).get, state(placeAt))
      state.setField(placeAt, newToken)
  }

  @inline private def _merge(top: Token, bot: Option[Token]) = bot match {
    case None => top
    case Some(t) => top :: t
  }
}
