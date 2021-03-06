package june.legency.env.activities;

import java.io.File;

import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import june.legency.env.R;
import june.legency.env.fragments.AMapFragment;
import june.legency.env.fragments.DuMapFragment;
import june.legency.env.fragments.GoogleMapFragment;
import june.legency.env.fragments.MainFragment;
import june.legency.env.fragments.TencentMapFragment;
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener, DuMapFragment.OnFragmentInteractionListener {

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton)findViewById(R.id.fab);
        Drawable addFab = MaterialDrawableBuilder.with(this) // provide a context
            .setIcon(MaterialDrawableBuilder.IconValue.PLUS_CIRCLE_OUTLINE) // provide an icon
            .setColor(Color.WHITE) // set the icon color
            .setSizeDp(25) // set the icon size
            .build();
        fab.setImageDrawable(addFab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show());

        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_main, new MainFragment())
            .commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment currentFragment = null;
        if (id == R.id.nav_main) {
            fab.show();
        } else {
            fab.hide();
        }
        if (id == R.id.nav_main) {
            currentFragment = new MainFragment();
        } else if (id == R.id.nav_amap) {
            // Handle the camera action
            currentFragment = new AMapFragment();
        } else if (id == R.id.nav_du_map) {
            currentFragment = new DuMapFragment();
        } else if (id == R.id.nav_t_map) {
            currentFragment = new TencentMapFragment();
        } else if (id == R.id.nav_g_map) {
            currentFragment = new GoogleMapFragment();
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }
        getSupportFragmentManager().beginTransaction().replace(R.id.content_main, currentFragment).commit();

        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public static void debugFolder(Application application) {
        File dir = application.getFilesDir();
        debugFile(dir);
    }

    private static void debugFile(File file) {
        Log.d("legency", "filename:" + file.getName());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                debugFile(file1);
            }
        }
    }
}
