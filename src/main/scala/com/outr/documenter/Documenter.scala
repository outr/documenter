package com.outr.documenter

import scala.io.Source
import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Test extends App {
  val documenter = new Documenter
  documenter.add("getting_started")
}

class Documenter {
  private def Blocks = Documenter.blocks.map(_.name).mkString("|")
  private def BlockRegex = s"""\\[($Blocks) (.+?)\\]""".r

  def add(name: String) = {
    val resource = getClass.getClassLoader.getResource(s"$name.md")
    val source = Source.fromURL(resource)
    val enhancedMarkdown = try {
      source.mkString
    } finally {
      source.close()
    }
    val result = BlockRegex.replaceAllIn(enhancedMarkdown, (m: Regex.Match) => {
      val block = Documenter.block(m.group(1))
      block.replace(m)
    })
//    println(result)
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

  final def replace(m: Regex.Match): String = {
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

  def convert(args: Map[String, String]) = {
    args.get("filename") match {
      case Some(fn) => filename = Some(fn)
      case None if filename.isEmpty => throw new RuntimeException(s"Cannot use a Scala block without a filename being set first (must either be set in a previous block or the current block).")
      case None => // Use existing
    }
    args("type") match {
      case "imports" => imports()
      case "object" => obj()
      case "section" => section(args("section"))
      case t => throw new RuntimeException(s"Scala block type `$t` unknown.")
    }
    "SCALA"
  }

  def imports() = {
    // TODO: handle
  }

  def obj() = {
    // TODO: handle
  }

  def section(name: String) = {
    // TODO: handle
  }
}

object FootnoteBlock extends BlockSupport {
  val name = "footnote"

  def convert(args: Map[String, String]) = "FOOTNOTE"
}