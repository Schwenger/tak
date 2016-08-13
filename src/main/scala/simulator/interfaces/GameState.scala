package simulator.interfaces

import simulator.interfaces.PlayerColor.{Black, PlayerColor, Red}
import simulator.interfaces.game_elements.{Position, Stack, Token}

import scala.collection.mutable

class GameState private (val size: Int, initializer: (Int, Int) => Option[Token]) {

  def this(size: Int) = {
    this(size, (x,y) => None)
  }

  // Static Data
  val numTokens = Map(4 -> (15, 0), 5 -> (20, 1), 6 -> (30, 1), 7 -> (40, 2), 8 -> (50, 2))

  // Dynamic Data
  private val board = mutable.Seq.tabulate(size, size)(initializer)
  private var tokens: PlayerMapping[(Int, Int)] = PlayerMapping(numTokens(size), numTokens(size))

  // Board Information
  def apply(pos: Position): Option[Token] = this(pos.x, pos.y)

  def apply(x: Int, y: Int): Option[Token] = board(x)(y)

  def dominatedBy(pos: Position, color: PlayerColor) = this(pos) exists (_.player == color)

  // Analytical Information (caller convenience)
  def freeFields: Int = (board map (_.count(_.isEmpty))).sum

  def domination: PlayerMapping[Int] =
    fold(PlayerMapping(0,0))((accu, field) =>
      field.map(_.player) match {
        case Some(Red) => PlayerMapping(accu.red + 1, accu.black)
        case Some(Black) => PlayerMapping(accu.red, accu.black + 1)
        case None => accu
      }
    )

  def fold[T](init: T)(f: (T, Option[Token]) => T) = board.foldLeft(init)((accu, row) => row.foldLeft(accu)(f))

  // Player Information
  def minionsLeft(player: PlayerColor): Int = tokens(player)._1

  def capstonesLeft(player: PlayerColor): Int = tokens(player)._2

  // Changing State
  def removeToken(player: PlayerColor, minion: Boolean) = {
    tokens = tokens.map((old, color) => (color == player, minion) match {
      case (true, true)  => (old._1 - 1, old._2)
      case (true, false) => (old._1, old._2 - 1)
      case _ => old
    })
  }

  def setField(pos: Position, token: Token): Unit = board(pos.x)(pos.y) = Some(token)
  def setField(pos: Position, token: Option[Token]): Unit = token match {
    case None => clearField(pos)
    case Some(t) => setField(pos, t)
  }
  def clearField(pos: Position) = board(pos.x)(pos.y) = None

  def checksum = {
    def b2i(b: Boolean): Int = if(b) 1 else 0
    def token2int(token: Token): Int = token match {
      case Stack(content) => content.foldLeft(0)((acc, t) => acc + token2int(t))
      case t => b2i(t.streetable) + b2i(t.worthAPoint) + b2i(t.blocking) + b2i(t.crushable) + b2i(t.crushing)
    }
    def aggr(accu: (Int, Int), token: Option[Token]): (Int, Int) =
      (accu._1 + token.map(token2int).getOrElse(1) * _primes(accu._2), accu._2 + 1)
    fold((0, 0))(aggr)
  }

  private val _primes = 2 #:: Stream.from(3, 2).filter(_isPrime)

  private def _isPrime(n: Int): Boolean =
      _primes.takeWhile(p => p * p <= n).forall(n % _ != 0)

  // Static Information

  def minionsPerPlayer: Int = numTokens(size)._1

  def capstonesPerPlayer: Int = numTokens(size)._2

  def copy: GameState = new GameState(size, (x,y) => board(x)(y))

  override def toString: String = {
    def abbrev(str: Any): String = str.toString.replace("Minion", "M").replace("Capstone","C").replace("Stack", "S").replace("Wall", "W").replace("Red", "R").replace("Black", "B")
    def pad(size: Int, msg: Any): String = s"%${size}s" format abbrev(msg)
    val fieldSize = 16 // must be even
    def token2str(token: Option[Token]) = token.map(pad(fieldSize, _)).getOrElse(" " * fieldSize)
    val horKey = "|   |" + (for(i <- 0 until size) yield (" " * (fieldSize/2)) + i + (" " * (fieldSize/2))).mkString("|") + "|   |"
    val horBorder = "|---|" + "-" * ((fieldSize + 1) * size + (size - 1)) + "|---|"
    val emptyLine = "|   |" + (for(i <- 0 until size) yield " " * (fieldSize + 1)).mkString("|") + "|   |"
    val borderTemp = for {
      y <- (0 until size).reverse
      x <- 0 until size
    } yield (if(x == 0) s"| $y " else " ") + "|" + token2str(board(x)(y)) + (if(x == size-1) if(y == 0) s" | $y |" else s" | $y |\n$emptyLine\n" else "")
    val borderStr = borderTemp.mkString
    Seq(horBorder, horKey, horBorder, emptyLine, borderStr, emptyLine, horBorder, horKey, horBorder).mkString("\n")
  }
}
