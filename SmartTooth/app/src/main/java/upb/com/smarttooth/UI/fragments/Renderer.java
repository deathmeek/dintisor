package upb.com.smarttooth.UI.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public interface Renderer {
    View onCreateView(LayoutInflater inflater, ViewGroup container,
                      Bundle savedInstanceState);

    boolean onOptionsItemSelected(MenuItem item);

    String getTitle();

    int getMenu();
}
