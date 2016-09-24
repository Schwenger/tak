package interfaces

import ai.players.UserPlayer
import interfaces.opponent.{Jesus, Opponent}
import simulator.interfaces.PlayerColor.{Black, PlayerColor, Red}
import simulator.interfaces.{Player, PlayerColor, PlayerMapping}

import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.Random

class CLI extends Interface {

  val rulesURL = "cheapass.com/sites/default/files/TakBetaRules3-10-16.pdf"

  override def greet(): Unit = {

    println("Welcome to Tak!")
    println("We invite you immerse in the wonderful world of strategy, schemings, and the art of building a street.")

    println()

    ruleDialog()

    println()

    val boardSize = boardSizeDialog

    println()

    val numHumans = numPlayerDialog

    println()

    val players = selectPlayers(numHumans)

    super.setPlayers(players.red(boardSize), players.black(boardSize))

    println()
    println("You're ready to go. Have fun!")

  }

  override def play(): Unit = ???

  override def rematch: Boolean = ???

  override def summary(): Unit = ???

  override def bye(): Unit = ???

  private def boardSizeDialog: Int = {
    println("How large should the board be?")
    println("We can play on boards of size 2 up to 8") //TODO Retrieve values dynamically from GameState or any config.
    val giveup = "Well, let's just settle with 4x4."
    def validSize(s: String): Option[Int] = {
      val digitsOnly = s.length == 1 && s.charAt(0).isDigit
      if(digitsOnly) {
        val size = s.toInt
        if (size >= 2 && size <= 8)
          Some(size)
        else
          None
      } else {
        val split = s.split("x") map (_.trim()) filter (_.length == 0)
        if (split.length != 3 || split(0) != split(2) || split(1).toLowerCase() != "x")
          None
        else
          Some(split(0).toInt)
      }
    }

    val pf: PartialFunction[String, Int] = {
      case s if validSize(s).isDefined => validSize(s).get
    }

    readNTimes(giveup, 4, pf)
  }

  private def selectPlayers(numHumans: Int): PlayerMapping[Int => Player] = numHumans match {
    case 0 =>
      println("So you want to watch, huh?")
      println("Well, so be it.")
      println("First choose who plays as Red.")
      val opponentR = opponentDialog
      val red = (size: Int) => opponentR.toPlayer(Red, size)
      println("Now choose who plays as Black.")
      val opponentB = opponentDialog
      val black = (size: Int) => opponentB.toPlayer(Black, size)
      PlayerMapping(red, black)
    case 1 =>
      val human = colorDialog
      val humanPlayer = (size:Int) => new UserPlayer(human, size)
      val opp = opponentDialog
      val oppPlayer = (size: Int) => opp.toPlayer(!human, size)
      human match {
        case Red => PlayerMapping(humanPlayer, oppPlayer)
        case Black => PlayerMapping(oppPlayer, humanPlayer)
      }
    case 2 =>
      println("Neat, so I can lean back and enjoy.")
      val red = (size: Int) => new UserPlayer(PlayerColor.Red, size)
      val black = (size: Int) => new UserPlayer(PlayerColor.Black, size)
      PlayerMapping(red, black)
  }

  private def ruleDialog(): Unit = {
    println("Are you familiar with the rules?")
    val giveUpMsg = "Oh, f*** it, just take the damn link."
    if(readYesNo(giveUpMsg, fallback = false)){
      println("Splendid!")
    } else {
      println("Make yourself comfortable with the rule on the following page:")
      println(rulesURL)
    }
  }

  private def opponentDialog: Opponent = {
    println("It's time to select your opponent. Choose wisely!")
    @tailrec def printOpp(opps: List[Opponent] = Opponent.all, item: Char = 'a'): Unit = opps match {
      case o :: os =>
        println(s"$item) ${o.characterization}")
        printOpp(os, (item + 1).toChar)
      case Nil => ()
    }
    println()
    printOpp()
    println()
    // TODO buggy.
    val fallback = Opponent.all(Random.nextInt(Opponent.all.length))
    val giveup = s"Well, I think ${fallback.name} is most suited in this case. Let's stick with that."
    def qualified(s: String) =
      Range(1, Opponent.all.length).find(c => s.equalsIgnoreCase(c.toString) || s.equalsIgnoreCase(c + ")"))
    val pf: PartialFunction[String, Int] = {
      case s if qualified(s).isDefined => qualified(s).get
    }
    val ix = readNTimes[Int](giveup, Opponent.all.indexOf(fallback), pf)
    Opponent.all(ix)
  }

  private def colorDialog: PlayerColor = {
    println("What color would you fancy?")
    val pf: PartialFunction[String, PlayerColor] = {
      case "red" | "r" | "not black" => PlayerColor.Red
      case "black" | "b" | "not red" => PlayerColor.Black
    }
    readNTimes("Ehr... You are Red.", PlayerColor.Red, pf)
  }

  private def numPlayerDialog: Int = {
    println("How many humans are there?")
    readNumber("I don't understand your language. I'll just assume there are five humans.", 5, None)

    println("How many of those want to play?")

    @tailrec def read: Int = {
      val num = readNumber("Sign, whatever. One of you wants to play. No discussion.", 1, Some("C'mon give me a number. DIGITS!!!"))
      if(num < 0 || num > 2) {
        println("Haha, very phunny you old joke cookie.")
        println("Now, seriously, how many?")
        read
      } else num
    }
    read
  }

  private def readNumber(giveup: String, fallback: Int, comeAgain: Option[String]): Int = {
    val pf: PartialFunction[String, Int] = {
      case s if s.length > 0 && (s forall (_.isDigit)) => s.toInt
    }
    if(comeAgain.isDefined)
      readNTimes(giveup, fallback, pf, comeAgain = comeAgain.get)
    else
      readNTimes(giveup, fallback, pf)
  }

  private def readYesNo(giveup: String, fallback: Boolean): Boolean = {
    val pf: PartialFunction[String, Boolean] = {
      case "true" | "yea" | "yes" | "y" | "ja" | "j" | "si" | "sure" | "yeah" => true
      case "false" | "no" | "nay" | "nope" | "n" | "nein" | "nah" => false
    }
    readNTimes(giveup, fallback, pf)
  }

  @tailrec @inline private def readNTimes[T](giveup: String, fallback: T, pf: PartialFunction[String, T], n: Int = 3,
                                             comeAgain: String = "I didn't quite catch that. Come again."): T = {
    val in = StdIn.readLine().trim().toLowerCase()
    if(n == 0) {
      println(giveup)
      fallback
    } else if(pf.isDefinedAt(in)) {
      pf(in)
    } else {
      println(comeAgain)
      readNTimes(giveup, fallback, pf, n = n - 1, comeAgain)
    }
  }

}
