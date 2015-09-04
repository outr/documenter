package com.outr.documenter

import java.io.File
import java.nio.file.Files

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Test extends App {
  val documenter = new Documenter(new File("generated"))
  documenter.add("introduction")
  documenter.add("getting_started")
  documenter.add("mapper")
}

class Documenter(outputDirectory: File) {
  private def Blocks = Documenter.blocks.map(_.name).mkString("|")
  private def BlockRegex = s"""\\[($Blocks) (.+?)\\]""".r

  outputDirectory.mkdirs()

  def add(name: String) = try {
    val resource = getClass.getClassLoader.getResource(s"$name.md")
    val source = Source.fromURL(resource)
    val enhancedMarkdown = try {
      source.mkString
    } finally {
      source.close()
    }
    val result = BlockRegex.replaceAllIn(enhancedMarkdown, (m: Regex.Match) => {
      val block = Documenter.block(m.group(1))
      val replacement = block.replace(m)
      Regex.quoteReplacement(replacement)
    })
    Files.write(new File(outputDirectory, s"$name.md").toPath, result.getBytes)
  } catch {
    case t: Throwable => throw new RuntimeException(s"Failed to process: $name", t)
  }
}

object Documenter {
  private var _blocks = Map.empty[String, BlockSupport]
  def blocks = _blocks.values

  register(PackageBlock)
  register(LinkBlock)
  register(ScalaBlock)
  register(FootnoteBlock)

  def block(name: String) = _blocks(name)

  def register(block: BlockSupport) = synchronized {
    _blocks += block.name -> block
  }
}

trait BlockSupport {
  def name: String

  final def replace(m: Regex.Match): String = try {
    var map = Map.empty[String, String]
    var key: String = null
    var open = false
    val b = new StringBuilder
    m.group(2).foreach {
      case '"' if open => {
        map += key -> b.toString()
        b.clear()
        open = false
      }
      case '"' => open = true
      case '=' if !open => {
        key = b.toString()
        b.clear()
      }
      case ' ' if !open => // Ignore spaces between
      case c => b.append(c)
    }
    convert(map)
  } catch {
    case t: Throwable => {
      throw new RuntimeException(s"Failed to replace ${m.group(0)} for block $name.", t)
    }
  }

  def convert(args: Map[String, String]): String
}

object PackageBlock extends BlockSupport {
  val name = "package"
  var current: Option[String] = None

  def convert(args: Map[String, String]) = {
    current = args("value") match {
      case "" => None
      case s => Option(s)
    }
    ""
  }
}

object LinkBlock extends BlockSupport {
  val name = "link"

  def convert(args: Map[String, String]) = "LINK"
}

object ScalaBlock extends BlockSupport {
  val name = "scala"

  var filename: Option[String] = None

  def file = new File(s"src/main/scala/${PackageBlock.current.getOrElse("").replaceAll("[.]", "/")}/${filename.get}.scala")

  def convert(args: Map[String, String]) = {
    args.get("filename") match {
      case Some(fn) => filename = Some(fn)
      case None if filename.isEmpty => throw new RuntimeException(s"Cannot use a Scala block without a filename being set first (must either be set in a previous block or the current block).")
      case None => // Use existing
    }
    args("type") match {
      case "imports" => imports()
      case "object" => handleObject()
      case "class" => handleClass()
      case "case class" => handleCaseClass()
      case "trait" => handleTrait()
      case "section" => section(args("section"))
      case t => throw new RuntimeException(s"Scala block type `$t` unknown.")
    }
  }

  private def forLines(f: String => Unit) = {
    val source = Source.fromFile(file)
    try {
      source.getLines().foreach(f)
    } finally {
      source.close()
    }
  }

  private def forBlock(isStart: String => Boolean, isEnd: String => Boolean, process: String => Option[String] = (s: String) => Some(s), includeStart: Boolean = true, includeEnd: Boolean = true) = {
    var started = false
    var ended = false
    val lines = ListBuffer.empty[String]
    def proc(line: String) = process(line) match {
      case Some(l) => lines += l
      case None => // Ignore
    }
    var lineNumber = 0
    forLines { line =>
      try {
        if (!ended && !started && isStart(line)) {
          started = true
          if (includeStart) {
            proc(line)
          }
        } else if (!ended && started && isEnd(line)) {
          ended = true
          if (includeEnd) {
            proc(line)
          }
        } else if (started && !ended) {
          proc(line)
        }
      } catch {
        case t: Throwable => throw new RuntimeException(s"Unable to process line: [$line] (line number: $lineNumber)", t)
      }
      lineNumber += 1
    }
    lines.toList
  }

  private def scalaBlock(lines: List[String]) =
    s"""```scala
       |${lines.mkString("\n")}
       |```
     """.stripMargin

  def imports() = {
    val imports = forBlock(
      (s: String) => s.startsWith("import"),
      (s: String) => s.startsWith("class") || s.startsWith("object"),
      (s: String) => if (s.startsWith("import")) Some(s) else None,
      includeEnd = false)
    scalaBlock(imports)
  }

  private def trimmingProcessor(length: => Int) = (line: String) => Some(if (line.length > length) line.substring(length) else line)

  def handleObject() = {
    var spacing = ""
    val lines = forBlock((s: String) => {
      val b = s.trim.startsWith(s"object ${filename.get}")
      if (b) {
        spacing = s.substring(0, s.indexOf('o'))
      }
      b
    }, (s: String) => s == s"$spacing}", trimmingProcessor(spacing.length))
    scalaBlock(lines)
  }

  def handleClass() = {
    var spacing = ""
    val lines = forBlock((s: String) => {
      val b = s.trim.startsWith(s"class ${filename.get}")
      if (b) {
        spacing = s.substring(0, s.indexOf('c'))
      }
      b
    }, (s: String) => s == s"$spacing}", trimmingProcessor(spacing.length))
    scalaBlock(lines)
  }

  def handleCaseClass() = {
    var spacing = ""
    val lines = forBlock((s: String) => {
      val b = s.trim.startsWith(s"case class ${filename.get}")
      if (b) {
        spacing = s.substring(0, s.indexOf('c'))
      }
      b
    }, (s: String) => s == s"$spacing}", trimmingProcessor(spacing.length))
    scalaBlock(lines)
  }

  def handleTrait() = {
    var spacing = ""
    val lines = forBlock((s: String) => {
      val b = s.trim.startsWith(s"trait ${filename.get}")
      if (b) {
        spacing = s.substring(0, s.indexOf('t'))
      }
      b
    }, (s: String) => s == s"$spacing}", trimmingProcessor(spacing.length))
    scalaBlock(lines)
  }

  def section(name: String) = {
    var spacing = ""
    var length = -1
    val trimmer = (line: String) => {
      val s = if (length == -1 && line.trim.startsWith("section")) {
        line.substring(spacing.length)
      } else if (length != -1 && line == s"$spacing}") {
        line.trim
      } else {
        if (length == -1) {
          val Regex = "(\\W*)(.+)".r
          line match {
            case Regex(whitespace, other) => length = whitespace.length
          }
        }
        if (line.length > length) {
          line.substring(length)
        } else {
          line
        }
      }
      Some(s)
    }
    val lines = forBlock(
      isStart = (s: String) => {
        val b = s.trim.startsWith(s"""section("$name"""")
        if (b) {
          spacing = s.substring(0, s.indexOf('s'))
        }
        b
      },
      isEnd = (s: String) => s == s"$spacing}",
      trimmer,
      includeStart = false,
      includeEnd = false
    )
    scalaBlock(lines)
  }
}

object FootnoteBlock extends BlockSupport {
  val name = "footnote"

  def convert(args: Map[String, String]) = "FOOTNOTE"
}