package org.scala_sbt.test1

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity extends Activity {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(new TextView(this) {
      setText("hello, world")
    })
  }
}