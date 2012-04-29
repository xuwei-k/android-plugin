package android_plugin_test.hoge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
  private final TypedViewHolder folder = TypedViewHolder.getInstance(this);

  @Override 
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Button button = folder.findView(TR.button);
    final TextView text = folder.findView(TR.text);
    text.setText("hello, world");
    setContentView(TR.layout.sample_activity.id);
  }
}
