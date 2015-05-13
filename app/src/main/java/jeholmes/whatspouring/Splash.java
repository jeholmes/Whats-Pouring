package jeholmes.whatspouring;

/**
 * Created by Jon on 12/05/2015.
 * Loading screen
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Splash extends Activity implements Runnable
{

    Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash);

        mThread = new Thread(this);

        mThread.start();
    }

    @Override
    public void run()
    {
        try
        {
            Thread.sleep(5000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            startActivity(new Intent(Splash.this, MapsActivity.class));

            finish();
        }
    }

}
