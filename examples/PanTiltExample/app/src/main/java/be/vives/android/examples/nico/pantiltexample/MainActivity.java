package be.vives.android.examples.nico.pantiltexample;

// Partial credits go to http://www.vogella.com/tutorials/AndroidTouch/article.html

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements OrientationChangeListener {
    private boolean leftIsHeld;
    private boolean rightIsHeld;

    private boolean isStopped;

    private int left_speed;
    private int right_speed;

    private ImageView leftControl;
    private ImageView rightControl;

    private Rect rectLeft;
    private Rect rectRight;

    // SparseArrays map integers to Objects. Like HashMap but more performant
    private SparseArray<Point> mActivePointers;

    private Orientation mOrientation;
    private long lastTimeUpdate;
    private int refreshDelay = 250;         // This should be an option !

    // Device placement
    private static int MAX_PITCH = -60;
    private static int MIN_PITCH = -20;
    private static int PITCH_RANGE = MAX_PITCH - MIN_PITCH;

    // Actual speed
    private static int MAX_SPEED = 150;
    private static int MIN_SPEED = -150;
    private static int SPEED_RANGE = MAX_SPEED - MIN_SPEED;

    // Device placement
    private static int MAX_ROLL = 40;
    private static int MIN_ROLL = -40;
    private static int ROLL_RANGE = MAX_ROLL - MIN_ROLL;

    // Speed control for thumper itself
    private static int MAX_TURN = 100;
    private static int MIN_TURN = -100;
    private static int TURN_RANGE = MAX_TURN - MIN_TURN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mOrientation = new Orientation((SensorManager)getSystemService(Activity.SENSOR_SERVICE), getWindow().getWindowManager(), this);

        mActivePointers = new SparseArray<Point>();

        // Following some helpful data to have for touch
        // Get location of controls
        // We need to switch left and right because motors are connected wrong
        rightControl = ((ImageView)findViewById(R.id.imgRight));
        leftControl = ((ImageView)findViewById(R.id.imgLeft));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // We cant do this in the onCreate as the Views have not been placed yet
        int[] l = new int[2];
        leftControl.getLocationOnScreen(l);
        rectLeft = new Rect(l[0], l[1], l[0] + leftControl.getWidth(), l[1] + leftControl.getHeight());

        rightControl.getLocationOnScreen(l);
        rectRight = new Rect(l[0], l[1], l[0] + rightControl.getWidth(), l[1] + rightControl.getHeight());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    protected void onResume() {
        super.onResume();
        mOrientation.startListening(this);

        // Needs implementation - Create RetroFit instance here

        isStopped = true;
        left_speed = 0;
        right_speed = 0;
        leftIsHeld = false;
        rightIsHeld = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientation.stopListening();

        // Kill the Thumper
        left_speed = 0;
        right_speed = 0;
        sendThumperSpeed();
        isStopped = true;
    }

    private void calculateSpeeds(float pitch, float roll) {
        // Remark !
        // I have no idea if the code below is correct and works as it should
        // Use at own risk

        // We need to rescale the pitch to our speed range first

        // Limit pitch
        pitch = Math.max(MAX_PITCH, pitch);
        pitch = Math.min(MIN_PITCH, pitch);

        double pitch_mult = ((pitch - MIN_PITCH) / PITCH_RANGE);		// Between 0 and 1
        int base_speed = (int)((pitch_mult * SPEED_RANGE) + MIN_SPEED);

        // Now we need to add turn control

        // Limit roll
        roll = Math.min(MAX_ROLL, roll);
        roll = Math.max(MIN_ROLL, roll);

        double roll_mult = ((roll - MIN_ROLL) / ROLL_RANGE);			// Between 0 and 1
        int base_turn = (int)((roll_mult * TURN_RANGE) + MIN_TURN);

        left_speed = base_speed + base_turn;
        right_speed = base_speed - base_turn;

        // Make sure limits are not exceeded
        left_speed = Math.min(MAX_SPEED, left_speed);
        left_speed = Math.max(MIN_SPEED, left_speed);
        right_speed = Math.min(MAX_SPEED, right_speed);
        right_speed = Math.max(MIN_SPEED, right_speed);
    }

    @Override
    public void onOrientationChanged(float pitch, float roll) {
        if (leftIsHeld && rightIsHeld) {
            // Indicate driving control active
            isStopped = false;

            ((TextView)findViewById(R.id.txtPitch)).setText(Float.toString(pitch));
            ((TextView)findViewById(R.id.txtRoll)).setText(Float.toString(roll));

            // Calculate left and right speed here
            calculateSpeeds(pitch, roll);

            // We need to check if refreshDelay is met
            // Orientation delay is not consistent !
            long currentTime = System.currentTimeMillis();
            long time_delta = currentTime - lastTimeUpdate;
            if (time_delta >= refreshDelay) {
                // NEEDS IMPLEMENTATION
                // Send speed of left and right to thumper

                lastTimeUpdate = System.currentTimeMillis();
            }
        } else {
            if (!isStopped) {
                // TODO
                // Stop the thumper here


                isStopped = true;

            }
        }
    }

    private void checkForHolds() {
        leftIsHeld = false;
        rightIsHeld = false;

        for (int size = mActivePointers.size(), i = 0; i < size; i++) {
            Point point = mActivePointers.valueAt(i);
            if (point != null) {
                if (rectLeft.contains(point.x, point.y)) {
                    leftIsHeld = true;
                } else if (rectRight.contains(point.x, point.y)) {
                    rightIsHeld = true;
                }
            }
        }

        isStopped = (left_speed == 0 && right_speed == 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // get pointer index from the event object
        int pointerIndex = event.getActionIndex();

        // get pointer ID
        int pointerId = event.getPointerId(pointerIndex);

        // get masked (not specific to a pointer) action
        int maskedAction = event.getActionMasked();

        switch (maskedAction) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                // We have a new pointer. Lets add it to the list of pointers

                Point point = new Point();
                point.x = (int)event.getX(pointerIndex);
                point.y = (int)event.getY(pointerIndex);
                mActivePointers.put(pointerId, point);
                break;
            }
            case MotionEvent.ACTION_MOVE: { // a pointer was moved
                for (int size = event.getPointerCount(), i = 0; i < size; i++) {
                    Point point = mActivePointers.get(event.getPointerId(i));
                    if (point != null) {
                        point.x = (int)event.getX(i);
                        point.y = (int)event.getY(i);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                mActivePointers.remove(pointerId);
                break;
            }
        }

        // Check which ones are held
        checkForHolds();

        // Visual indication
        if (leftIsHeld) {
            leftControl.setImageResource(R.drawable.ic_hold_true);
        } else {
            leftControl.setImageResource(R.drawable.ic_hold_false);
        }

        if (rightIsHeld) {
            rightControl.setImageResource(R.drawable.ic_hold_true);
        } else {
            rightControl.setImageResource(R.drawable.ic_hold_false);
        }

        return true;
    }

    public void sendThumperSpeed() {
        // Needs implementation
    }
}
