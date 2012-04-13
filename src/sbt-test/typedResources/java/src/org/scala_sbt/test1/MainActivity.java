package org.scala_sbt.test1;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
  private final TypedViewHolder folder = TypedViewHolder.getInstance(this);

  @Override 
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView text = folder.findView(TR.text);
    Button button = folder.findView(TR.button);
    setContentView(new TextView(this) {{
      setText("hello, world");
    }});
  }
}
