package org.scala_sbt.test1

import android.app.Activity
import android.os.Bundle
import android.widget.{Button,TextView}
import TypedResource._

class MainActivity extends Activity {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    val text:TextView = this.findView(TR.text)
    val button:Button = this.findView(TR.button)
    setContentView(new TextView(this) {
      setText("hello, world")
    })
  }
}
