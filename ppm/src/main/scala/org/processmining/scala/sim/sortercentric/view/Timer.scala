package org.processmining.scala.sim.sortercentric.view

import javax.swing.Timer

object ScalaTimer {
  def apply(interval: Int, repeats: Boolean = true)(op: => Unit): Timer ={
    val timeOut = new javax.swing.AbstractAction() {
      def actionPerformed(e: java.awt.event.ActionEvent) = op
    }
    val t = new javax.swing.Timer(interval, timeOut)
    t.setRepeats(repeats)
    t.start()
    t
  }
}
