package ninja.bryansills.adwaita;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView mainText = (TextView) findViewById(R.id.hello_world);
        mainText.setText(new DoesThisWork().getShowThis());
    }
}