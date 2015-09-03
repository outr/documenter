package com.outr.documenter

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SectionSupport {
  protected def section[R](name: String, invoke: Boolean = true)(f: => R) = {
    f
  }
}