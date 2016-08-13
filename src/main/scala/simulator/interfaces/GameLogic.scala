package simulator.interfaces

import simulator.interfaces.PlayerColor.PlayerColor
import simulator.interfaces.game_elements._
import simulator.interfaces.game_elements.ActionKind.ActionKind
import simulator.interfaces.game_elements.Direction.Direction
object GameLogic {

  /**
    * Decides whether a given State is terminal. Returns the winning player, if there is one.
    * @param state for which this method decides whether it is terminal.
    * @return The winning player, if any.
    */
  def gameOver(state: GameState) : Option[PlayerColor] = {
    val horizontal = hasStreet_impl1(state, Direction.Right, i => Position(0, i))
    val vertical = hasStreet_impl1(state, Direction.Up, i => Position(i, 0))
    (horizontal.red || vertical.red, horizontal.black || vertical.black) match {
      case (true, false) => Some(PlayerColor.Red)
      case (false, true) => Some(PlayerColor.Black)
      case (true, true) => throw new IllegalStateException("This case must not occur.")
      case (false, false) => None
    }
  }

//  def hasStreet_impl2(state: GameState): PlayerMapping[Boolean] = {
//    case class Node(x: Int, y: Int){
//      val neigh: List[Node] = Nil
//      var seen = false
//    }
//
//    def generateNodes(player: PlayerColor) =
//      Seq.tabulate(size, size)((x, y) => if(state.dominatedBy(Position(x,y), player)) Some(Node(x,y)) else None)
//
//    // We can eradicate one of those directions.
//    def addNeighbour(board: Seq[Seq[Option[Node]]], posA: Position, posB: Position) = {
//      board(posA.x)(posA.y) map (n => n.neigh :+ board(posB.x)(posB.y).getOrElse(Nil))
//      board(posB.x)(posB.y) map (n => n.neigh :+ board(posA.x)(posA.y).getOrElse(Nil))
//    }
//
//    def DFS(open: List[Node], goal: Node => Boolean): Boolean = {
//      if(open.isEmpty) {
//        false
//      } else if(open.head.seen) {
//        DFS(open.tail, goal)
//      } else if(goal(open.head)) {
//        true
//      } else {
//        open.head.seen = true
//        DFS(open.head.neigh ::: open.tail, goal)
//      }
//    }
//
//    val boardRed = generateNodes(PlayerColor.Red)
//    val boardBlack = generateNodes(PlayerColor.Black)
//
//    for {x <- 0 until state.size
//         y <- 0 until state.size
//    }{
//      val pos = Position(x, y)
//      // we go up right since (0,0) is bottom left
//      for(dir <- Seq(Direction.Up, Direction.Right)){
//        val neigh = dir(pos)
//        if(validPos(state.size, neigh)) {
//          addNeighbour(boardRed, pos, neigh)
//          addNeighbour(boardBlack, pos, neigh)
//        }
//      }
//    }
//  }

  def hasStreet_impl1(state: GameState, dir: Direction, initial: Int => Position): PlayerMapping[Boolean] = {
    val (orth1, orth2) = dir.orth

    def nextStep(open: Seq[Position]): Seq[Position] =
      open collect { case pos if state.dominatedBy(dir(pos), PlayerColor.Red) => dir(pos) }

    def expand(open: Seq[Position], color: PlayerColor): Seq[Position] = {
      def move(pos: Position, dir: Direction): Seq[Position] = {
        if (validPos(state.size, pos) && state.dominatedBy(pos, color))
          move(dir(pos), dir) :+ pos
        else
          Nil
      }
      open.foldLeft(Nil: Seq[Position]) { (accu: Seq[Position], pos: Position) =>
        val move1 = move(pos, orth1)
        val move2 = move(pos, orth2)
        move1 ++ move2 ++ accu
      }.distinct
    }
    def checkBoard(color: PlayerColor) = {
      var open: Seq[Position] = for {i <- 0 until state.size
           if state.dominatedBy(initial(i), PlayerColor.Red)} yield initial(i)
      for(i <- 1 until state.size){
        open = expand(nextStep(open), color)
      }
      open
    }
    val red = checkBoard(PlayerColor.Red)
    val black = checkBoard(PlayerColor.Black)
    PlayerMapping(red.isEmpty, black.isEmpty)
  }


  /**
    * Collects the Actions a given Player can apply in the given State. Returns an empty list if there is none.
    * @param state for which the applicable Actions are collected.
    * @param color of the player for which Actions are collected.
    * @return list of applicable actions
    */
  def availableActions(state: GameState)(implicit color: PlayerColor): List[Action] = Nil

  /**
    * Collects the Actions of a given Kind a given Player can apply in the given State. Returns an empty list if there is none.
    * @param state for which the applicable Actions are collected.
    * @param actionKind the kind of Actions which are collected.
    * @param color of the player for which Actions are collected.
    * @return list of applicable actions
    */
  def availableActions(state: GameState, actionKind: ActionKind)(implicit color: PlayerColor): List[Action] = Nil

  /**
    * Decides whether a given Action is applicable in a given State for a given Player.
    * @param state in which the action might be applicable
    * @param action which is checked for being applicable.
    * @param color of the player.
    * @return
    */
  def isValid(state: GameState, action: Action)(implicit color: PlayerColor): Boolean = action match {
    case PlaceCapstone(dest) => validPos(state.size ,dest) && state(dest).isEmpty && state.capstonesLeft(color) > 0
    case PlaceMinion(dest, _) => validPos(state.size, dest) && state(dest).isEmpty && state.minionsLeft(color) > 0
    case PlaceWall(dest) => validPos(state.size, dest) && state(dest).isEmpty && state.minionsLeft(color) > 0
    case Slide(src, stones, dir) => validSlide(state, src, stones, dir)
  }

  // TODO iterative -> recursive
  private def validSlide(state: GameState, src: Position, stones: List[Int], dir: Direction)(implicit player: PlayerColor): Boolean = {
    if(!state.dominatedBy(src, player))
      return false
    if(stones.head > state.size)
      return false
    if(!(stones == stones.sortBy(-_).distinct)) // checks for strictly monotonic decreasing
      return false
    if(stones.min <= 0)
      return false
    if(!validPos(state.size, src))
      return false

    var inHand = state(src).get match {
      case Stack(content) => content
      case x => List(x)
    }
    inHand = inHand.take(stones.head)

    var dest = dir(src)
    if(inHand.length < stones.head)
      return false
    for(n <- stones.tail) {
      val top = Tokenizer(inHand.drop(n)).get
      if(!validPos(state.size, dest) || state(dest).isDefined && !_stackable(top, state(dest).get))
        return false
      inHand = inHand.take(n)
      dest = dir(dest)
    }
    if(!validPos(state.size, dest) || state(dest).isDefined && !_stackable(Tokenizer(inHand).get, state(dest).get))
      return false

    true

  }

  @inline private def _stackable(top: Token, bot: Token): Boolean = !bot.blocking || top.crushing && bot.crushable
  @inline private def validPos(bound: Int, pos: Position) = (0 until bound contains pos.x) && (0 until bound contains pos.y)


}
