package com.outr.documenter

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SectionSupport {
  protected def section[R](name: String)(f: => R): R = {
    try {
      f
    } catch {
      case t: Throwable => throw new RuntimeException(s"Section: $name failed in $getClass", t)
    }
  }
  protected def sectionNoExec[R](name: String)(f: => R): Unit = {
    // Nothing to do
  }
}