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
import android.widget.ArrayAdapter;

public class MainActivity extends AppCompatActivity {

    // Thread that runs the stopwatch timer
    public static Thread startThread ;
    // Flag to indicate if the stopwatch is running
    public static boolean isStarted;
    // Flag to indicate if the stopwatch is restarted
    public static boolean isReStarted;
    // Flag to indicate if the stopwatch is paused
    public static boolean isPaused;
    // Holds the current time as a string, used for lap times
    public static String currentTime;
    // Stores the elapsed time in milliseconds. Crucial for pausing and resuming.
    public static long timeDifference = 0;
    // the textview as screen of the stopwatch
    public TextView textView;
    public ListView listView;
    public Button start;
    public Button restart;
    public Button pause;
    public Button lap;
    public static ArrayList<String> dataList;
    public static ArrayAdapter<String> adapter;
    /**
     * Converts milliseconds into a time format HH:mm:ss.SSS.
     * @param millis The duration in milliseconds.
     * @return A string representing the time.
     */
    private static String millisToTime(long millis){
        long seconds = millis/1000;
        millis %= 1000;
        long minutes = seconds/60;
        seconds %= 60;
        long hours = minutes/60;
        minutes %= 60;
        String second = (seconds<10) ? "0"+seconds : ""+seconds;
        String minute = (minutes<10) ? "0"+minutes : ""+minutes;
        String hour = (hours<10) ? "0"+hours : ""+hours;
        String milli = (millis<100) ? "0"+millis : ""+millis;
        return String.format("%s:%s:%s.%s", hour, minute, second, milli);
    }
    /**
     * Starts or resumes the stopwatch.
     * @throws InterruptedException if the thread is interrupted.
     */
    private static void start(TextView textView) throws InterruptedException {

        isStarted = true;
        isPaused = false;
        isReStarted = false;
        // Calculate the effective start time.
        // If resuming, this subtracts the already elapsed time (timeDifference)
        // to ensure the timer continues from where it left off.
        long startTimeInMillis = System.currentTimeMillis()-timeDifference;
        // Loop as long as the stopwatch is running
        while (isStarted) {
            // Calculate total elapsed time and update the display and helper variables.
            currentTime = millisToTime(System.currentTimeMillis()-startTimeInMillis);
            textView.setText(currentTime);
            // Pause the thread for a short duration to prevent high CPU usage
            // and to create a periodic update cycle.
            Thread.sleep(100);
        }
        if (isReStarted){
            timeDifference = 0;
        }
        else{
            timeDifference = System.currentTimeMillis()-startTimeInMillis;
        }
    }
    /**
     * Pauses the stopwatch.
     */
    public static void pause() {
        // Set flags to stop the timer loop in the start() method.
        isStarted = false;
        isPaused = true;
    }
    /**
     * Stops and resets the stopwatch to zero.
     */
    public static void restart(TextView textView) {
        // Reset all state variables and the display to their initial values.
        isStarted = false;
        isReStarted = true;
        timeDifference = 0;
        currentTime = "00:00:00.000";
        textView.setText("");
        textView.setText(currentTime);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // XML elements
        textView = findViewById(R.id.screen);
        listView = findViewById(R.id.listView);
        // ArrayList, Adapter
        dataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        // Buttons
        start = findViewById(R.id.startBtn);
        start.setOnClickListener(v -> {
            if (!isStarted){
                startThread = new Thread(){@Override public void run(){ try {
                    MainActivity.start(textView);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }}};
                startThread.start();
            }
        });

        pause = findViewById(R.id.pauseBtn);
        pause.setOnClickListener(v ->{
            if (isStarted) {
                pause();
                try{
                    startThread.join();
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        restart = findViewById(R.id.restartBtn);
        restart.setOnClickListener(v ->{
            // Action is performed only if stopwatch was started or is paused
            if (isPaused || isStarted) {
                restart(textView);
                dataList.clear();
                adapter.notifyDataSetChanged();
                if (!isPaused){
                    try{
                        startThread.join();
                    }
                    catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        lap = findViewById(R.id.lapBtn);
        lap.setOnClickListener(v -> {
            if (!currentTime.equals("00:00:00.000")) {
                dataList.add(currentTime);
                adapter.notifyDataSetChanged();
            }
        });
    }
}