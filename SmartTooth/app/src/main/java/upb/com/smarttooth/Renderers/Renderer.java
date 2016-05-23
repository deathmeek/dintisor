package upb.com.smarttooth.Renderers;

import android.view.MenuItem;
import android.view.View;

public interface Renderer {
    void render(View rootView);

    boolean onOptionsItemSelected(MenuItem item);
}
