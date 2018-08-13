package org.processmining.scala.viewers.spectrum.view

import java.awt.Color

trait Palette {
  def getDefaultFontColor(): Color = Color.black

  def getDefaultGridColor(): Color

  def getClazzColor(clazz: Int, a: Int): Color

  def getBackgroundTracesColor(): Color = Color.lightGray

  def getId: Int
}

object Palette {
  val Transparant = 150
  val NotTransparent = 255

}

class DefaultPalette extends Palette {

  override def getDefaultGridColor() = Color.gray

  override def getClazzColor(clazz: Int, a: Int) =
    clazz match {
      case 0 => new Color(255, 200, 0, a)
      case 1 => new Color(0, 155, 0, a)
      case 2 => new Color(0, 0, 255, a)
      case 3 => new Color(0, 255, 255, a)
      case 4 => new Color(255, 0, 0, a)
      case 5 => new Color(255, 0, 255, a)
      case 6 => new Color(255, 175, 175, a)
      case 7 => new Color(128, 128, 128, a)
      case 8 => new Color(142, 38, 43, a)
      case 9 => new Color(0, 0, 0, a)
      case _ => {
        val s = clazz - 9
        new Color(Math.min(255, s * 10), Math.min(255, s * 5), Math.max(0, 255 - s * 10), a)
      }

    }

  override def getId = 2
}

class OriginalPalette extends Palette {

  override def getDefaultGridColor() = Color.gray

  override def getClazzColor(clazz: Int, a: Int) =
    clazz match {
      case 0 => new Color(0, 155, 0, a)
      case 1 => new Color(0, 0, 255, a)
      case 2 => new Color(0, 255, 255, a)
      case 3 => new Color(255, 200, 0, a)
      case 4 => new Color(255, 0, 0, a)
      case 5 => new Color(255, 0, 255, a)
      case 6 => new Color(255, 175, 175, a)
      case 7 => new Color(128, 128, 128, a)
      case 8 => new Color(142, 38, 43, a)
      case 9 => new Color(0, 0, 0, a)
      case _ => {
        val s = clazz - 9
        new Color(Math.min(255, s * 10), Math.min(255, s * 5), Math.max(0, 255 - s * 10), a)
      }

    }

  override def getId = 0
}


class Bw5Palette extends Palette {

  override def getDefaultGridColor() = Color.black

  override def getClazzColor(clazz: Int, a: Int) =
    clazz match {
      case 0 => new Color(0x45, 0x75, 0xb4, a)
      case 1 => new Color(0x91, 0xbf, 0xdb, a)
      case 2 => new Color(0xe0, 0xf3, 0xf8, a)
      case 3 => new Color(0xf3, 0xe0, 0x90, a)
      case 4 => new Color(0xfc, 0x8d, 0x59, a)
      case 5 => new Color(0xd7, 0x30, 0x27, a)
      case 6 => new Color(255, 175, 175, a)
      case 7 => new Color(128, 128, 128, a)
      case 8 => new Color(142, 38, 43, a)
      case 9 => new Color(0, 0, 0, a)
      case _ => {
        val s = clazz - 9
        new Color(Math.min(255, s * 10), Math.min(255, s * 5), Math.max(0, 255 - s * 10), a)
      }

    }

  override def getId = 1

}

class BwQ4Palette extends Palette {

  override def getDefaultGridColor() = Color.black

  override def getClazzColor(clazz: Int, a: Int) =
    clazz match {
      case 0 => new Color(0x45, 0x75, 0xb4, a)
      case 1 => new Color(0x91, 0xbf, 0xdb, a)
      case 2 => new Color(0xf3, 0xe0, 0x90, a)
      case 3 => new Color(0xfc, 0x8d, 0x59, a)
      case _ => {
        val s = clazz - 3
        new Color(Math.min(255, s * 10), Math.min(255, s * 5), Math.max(0, 255 - s * 10), a)
      }
    }

  //override def getBackgroundTracesColor: Color = new Color(0xe2, 0xf5, 0xfa)
  override def getBackgroundTracesColor: Color = Color.gray

  override def getId = 3
}

