package ninja.bryansills.adwaita.location;

import android.location.Location;

public interface LocationProvider {
    void addListener(Listener listener);

    public interface Listener {
        void onLocationChanged(Location location);
    }
}
