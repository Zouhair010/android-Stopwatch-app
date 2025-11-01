package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.widget.ArrayAdapter;

public class MainActivity extends AppCompatActivity {

    // Manages the execution of the stopwatch timer logic in the background.
    public static Thread startThread ;
    public Thread dateTimeThread;
    // Tracks whether the stopwatch is currently running.
    public static boolean isStarted;
    // Tracks whether the stopwatch has been reset.
    public static boolean isReStarted;
    // Tracks whether the stopwatch is in a paused state.
    public static boolean isPaused;
    // Stores the current stopwatch time as a formatted string.
    public static String currentTime;
    // Stores the elapsed time in milliseconds when the stopwatch is paused. Essential for resuming correctly.
    public static long timeDifference = 0;
    // Data store for lap times and the adapter to link it to the ListView.
    public ListView listView;
    public static ArrayList<String> dataList;
    public static ArrayAdapter<String> adapter;

    private void showDateTime(TextView datetimeScreen) throws InterruptedException{;
        while (true){
            runOnUiThread(() -> datetimeScreen.setText(
                    new GregorianCalendar().getTime().toString()
            ));
//            datetimeScreen.setText(new GregorianCalendar().getTime().toString());
            Thread.sleep(1000);
        }
    }
    /**
     * Converts milliseconds into a time format HH:mm:ss.SSS.
     * @param millis The duration in milliseconds.
     * @return A string representing the time.
     */
    private static String millisToTime(long millis){
        long seconds = millis/1000;
        long remainingMillis = millis % 1000;
        long minutes = seconds/60;
        seconds %= 60;
        long hours = minutes/60;
        minutes %= 60;
        // Pad with leading zeros for a consistent format.
        String second = (seconds<10) ? "0"+seconds : ""+seconds;
        String minute = (minutes<10) ? "0"+minutes : ""+minutes;
        String hour = (hours<10) ? "0"+hours : ""+hours;
        String milli = (remainingMillis<100) ? (remainingMillis < 10 ? "00"+remainingMillis : "0"+remainingMillis) : ""+remainingMillis;
        // Return the formatted time string.
        return String.format("%s:%s:%s.%s", hour, minute, second, milli);
    }
    /**
     * Starts or resumes the stopwatch.
     * @throws InterruptedException if the thread is interrupted.
     */
    private static void start(TextView textView) throws InterruptedException {
        // Set state flags for running mode.
        isStarted = true;
        isPaused = false;
        isReStarted = false;
        // Calculate the effective start time.
        // If resuming, this subtracts the already elapsed time (timeDifference)
        // to ensure the timer continues from where it left off.
        long startTimeInMillis = System.currentTimeMillis()-timeDifference;
        // Main timer loop; continues as long as the stopwatch is started.
        while (isStarted) {
            // Calculate total elapsed time since start.
            currentTime = millisToTime(System.currentTimeMillis()-startTimeInMillis);
            // Update the UI with the new time.
//            runOnUiThread(() -> textView.setText(currentTime));
            textView.setText(currentTime);
            // Pause the thread for a short duration to prevent high CPU usage
            // and to create a periodic update cycle.
            Thread.sleep(100);
        }
        // After the loop ends, check if it was due to a restart or a pause.
        if (isReStarted){
            timeDifference = 0;
        } else {
            // If paused, store the elapsed time to resume from this point later.
            timeDifference = System.currentTimeMillis()-startTimeInMillis;
        }
    }
    /**
     * Pauses the stopwatch.
     */
    private static void pause() {
        // Set flags to stop the timer loop in the start() method.
        isStarted = false;
        isPaused = true;
    }
    /**
     * Stops and resets the stopwatch to zero.
     */
    private static void restart(TextView textView) {
        // Reset all state variables and the display to their initial values.
        isStarted = false;
        isReStarted = true;
        timeDifference = 0;
        currentTime = "00:00:00.000";
        textView.setText(currentTime);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Set the layout for this activity.
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dateTimeThread = new Thread(){@Override public void run(){ try {
            showDateTime(findViewById(R.id.datetimeScreen));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }}};
        dateTimeThread.start();

        // Initialize the list for lap times and its adapter for the ListView.
        listView = findViewById(R.id.listView);
        dataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        // Set a click listener for the Start button.
        findViewById(R.id.startBtn).setOnClickListener(v -> {
            // Only start if the stopwatch is not already running.
            if (!isStarted){
                startThread = new Thread(){@Override public void run(){ try {
                    // Run the stopwatch logic in a new background thread.
                    MainActivity.start(findViewById(R.id.screen));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }}};
                startThread.start();
            }
        });

        findViewById(R.id.pauseBtn).setOnClickListener(v ->{
            // Only pause if the stopwatch is currently running.
            if (isStarted) {
                pause();
                try{
                    // Wait for the stopwatch thread to finish its current execution cycle and terminate.
                    startThread.join();
                    startThread = null;
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        findViewById(R.id.restartBtn).setOnClickListener(v ->{
            // Restart is allowed if the stopwatch has been started or is currently paused.
            if (isPaused || isStarted) {
                restart(findViewById(R.id.screen));
                // Clear the lap times list and update the adapter.
                dataList.clear();
                adapter.notifyDataSetChanged();
                // If the stopwatch was running (not paused), we need to stop the thread.
                if (!isPaused){
                    try{
                        // Wait for the stopwatch thread to terminate.
                        startThread.join();
                        startThread = null;
                    }
                    catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        findViewById(R.id.lapBtn).setOnClickListener(v -> {
            if (currentTime!=null&&!currentTime.equals("00:00:00.000")) {
                dataList.add(currentTime);
                adapter.notifyDataSetChanged();
            }
        });
    }
}