package com.example.myapplication; // Declares the package where this class belongs.

import android.os.Bundle; // Imports the class for passing data between activities.
import android.os.SystemClock;
import android.view.View;
import android.widget.Button; // Imports the Button widget class (though not explicitly used by name, it's often inferred).
import android.widget.ListView; // Imports the ListView widget class for displaying a scrollable list.
import android.widget.TextView; // Imports the TextView widget class for displaying text.

import androidx.activity.EdgeToEdge; // Imports class for managing full-screen display (edge-to-edge).
import androidx.appcompat.app.AppCompatActivity; // Base class for activities that use the ActionBar feature.
import androidx.core.graphics.Insets; // Imports class for managing window insets (safe areas).
import androidx.core.view.ViewCompat; // Imports compatibility class for View functionality.
import androidx.core.view.WindowInsetsCompat; // Imports compatibility class for window insets.
import android.widget.Toast; // Imports the Toast class for displaying brief messages.
import android.widget.ProgressBar;

import java.util.ArrayList; // Imports the dynamic array (List) implementation.
import java.util.Calendar; // Imports the Calendar abstract class for date and time calculations (though GregorianCalendar is used).
import java.util.GregorianCalendar; // Imports the concrete class for calendar calculations.

import android.widget.ArrayAdapter; // Imports the adapter class to link data to a ListView.

// Main activity class, extending the base AppCompatActivity for an Android screen.
public class MainActivity extends AppCompatActivity {

    // UI Elements
    private TextView screen; // Displays the current stopwatch time.
    private Button startBtn; // The "Start" or "Resume" button.
    private Button pauseBtn; // The "Pause" button.
    private Button restartBtn; // The "Restart" or "Reset" button.
    private Button lapBtn; // The "Lap" button to record a lap time.
    private ListView listView; // Displays the list of recorded lap times.
    private ProgressBar progressBar; // Visual indicator for the seconds part of the timer.

    // Stopwatch State Management
    private Thread startThread; // Background thread for running the stopwatch timer logic.
    private boolean isStarted; // Flag to indicate if the stopwatch is currently running.
    private boolean isPaused = false; // Flag to indicate if the stopwatch is paused.
    private boolean isReStarted = false; // Flag to indicate if the stopwatch has been reset.
    private long timeDifference = 0; // Stores the elapsed time in milliseconds when paused, for correct resume.

    // Data for Lap Times
    private String currentTime; // Stores the current stopwatch time as a formatted string.
    private ArrayList<String> dataList; // Holds the list of lap time strings.
    private ArrayAdapter<String> adapter; // Connects the dataList to the ListView for display.

    // Helper for ProgressBar
    private int progressSeconds = 0; // Stores the current seconds (0-59) for the progress bar.


    /**
     * Converts milliseconds into a time format HH:mm:ss.SSS.
     * @param millis The duration in milliseconds.
     * @return A string representing the time.
     */
    private String millisToTime(long millis){
        int seconds = (int) millis/1000; // Calculate total seconds.
        int remainingMillis = (int) millis % 1000; // Calculate remaining milliseconds.
        int minutes = seconds/60; // Calculate total minutes.
        seconds %= 60; // Calculate remaining seconds (0-59).
        int hours = minutes/60; // Calculate total hours.
        minutes %= 60; // Calculate remaining minutes (0-59).
        progressSeconds = seconds; // Update the seconds for the progress bar.
        // Pad with leading zeros for a consistent format.
        String second = (seconds<10) ? "0"+seconds : ""+seconds; // Format seconds with leading zero if needed.
        String minute = (minutes<10) ? "0"+minutes : ""+minutes; // Format minutes with leading zero if needed.
        String hour = (hours<10) ? "0"+hours : ""+hours; // Format hours with leading zero if needed.
        // Format milliseconds with leading zeros (000-999).
        String milli = (remainingMillis<100) ? (remainingMillis < 10 ? "00"+remainingMillis : "0"+remainingMillis) : ""+remainingMillis;
        // Return the formatted time string.
        return String.format("%s:%s:%s.%s", hour, minute, second, milli);
    }
    /**
     * Starts or resumes the stopwatch.
     */
    private void play() {
        // Set state flags for running mode.
        isPaused = false;
        isStarted = true;
        // Calculate the effective start time.
        // If resuming, this subtracts the already elapsed time (timeDifference)
        // to ensure the timer continues from where it left off.
        long startTimeInMillis = System.currentTimeMillis()-timeDifference;
        // Main timer loop; continues as long as the stopwatch is started.
        while (isStarted) {
            // Calculate total elapsed time since the effective start time.
            currentTime = millisToTime(System.currentTimeMillis()-startTimeInMillis);
            // Update the UI with the new time. Must run on the UI thread.
            runOnUiThread(() -> {
                screen.setText(currentTime);
                progressBar.setProgress(progressSeconds, true); // Update the progress bar to reflect the current second (0-59).
            });
            // Pause the thread for a short duration to prevent high CPU usage
            // and to create a periodic update cycle (10 updates per second).
            SystemClock.sleep(100);
        }
        // After the loop ends, check if it was due to a restart or a pause.
        if (isReStarted){
            timeDifference = 0; // Reset accumulated time difference if restarted.
        } else {
            // If paused, store the elapsed time to resume from this point later.
            timeDifference = System.currentTimeMillis()-startTimeInMillis;
        }
    }
    /**
     * Pauses the stopwatch.
     */
    private void pause() {
        // Set flags to stop the timer loop in the play() method.
        isPaused = true;
        isReStarted = false;
        isStarted = false;
        try{
            // Wait for the stopwatch thread to finish its current execution cycle and terminate.
            startThread.join();
            startThread = null; // Clear the thread reference.
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    /**
     * Stops and resets the stopwatch to zero.
     */
    private void restart() {
        isReStarted = true;
        isStarted = false;
        // A restart can only happen when the stopwatch is paused.
        if (isPaused) {
            // Reset all state variables and the display to their initial values.
            timeDifference = 0; // Reset accumulated time difference.
            currentTime = "00:00:00.000"; // Reset displayed time string.
            screen.setText(currentTime); // Update the TextView with the reset time.
            // Clear the lap times list and update the adapter.
            dataList.clear();
            adapter.notifyDataSetChanged();
            progressBar.setProgress(0,true); // Reset the progress bar to the beginning.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Enable edge-to-edge display.
        // Set the layout for this activity from the XML resource file.
        setContentView(R.layout.activity_main);
        // Handle window insets (like status bars) for the main view.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        screen = findViewById(R.id.screen);
        startBtn = findViewById(R.id.startBtn);
        pauseBtn = findViewById(R.id.pauseBtn);
        restartBtn = findViewById(R.id.restartBtn);
        lapBtn = findViewById(R.id.lapBtn);
        // Set initial visibility of buttons.
        pauseBtn.setVisibility(View.GONE);
        lapBtn.setVisibility(View.GONE);
        // Initialize the ProgressBar. Its max value should be set to 59 or 60 in the layout XML.
        progressBar = findViewById(R.id.progressBar);
        // Initialize the list for lap times and its adapter for the ListView.
        listView = findViewById(R.id.listView); // Get the ListView from the layout.
        dataList = new ArrayList<>(); // Initialize the data list.
        // Initialize the adapter to link dataList to listView, using a simple layout.
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter); // Set the adapter on the ListView.

        // Set a click listener for the Start button.d
        startBtn.setOnClickListener(v -> {
            // Initialize and start the background thread for the stopwatch.
            startThread = new Thread(){@Override public void run(){
                play();
            }};
            startThread.start(); // Begin the stopwatch thread execution.
            // Display a brief confirmation message.
            Toast.makeText(this, "GO!!!!!", Toast.LENGTH_SHORT).show();

            startBtn.setVisibility(View.GONE);
            pauseBtn.setVisibility(View.VISIBLE);
            restartBtn.setVisibility(View.GONE);
            lapBtn.setVisibility(View.VISIBLE);
        });

        // Set a click listener for the Pause button.
        pauseBtn.setOnClickListener(v ->{
            pause();
            // Display a brief confirmation message.
            Toast.makeText(this, "the stop watch paussed.", Toast.LENGTH_SHORT).show();

            pauseBtn.setVisibility(View.GONE);
            startBtn.setVisibility(View.VISIBLE);
            lapBtn.setVisibility(View.GONE);
            restartBtn.setVisibility(View.VISIBLE);
        });

        // Set a click listener for the Restart button.
        restartBtn.setOnClickListener(v ->{
            restart();
            // Display a brief confirmation message.
            Toast.makeText(this, "the stop watch restarted.", Toast.LENGTH_SHORT).show();
        });

        // Set a click listener for the Lap button.
        lapBtn.setOnClickListener(v -> {
            dataList.add(currentTime); // Add the current time to the lap list.
            adapter.notifyDataSetChanged(); // Tell the ListView to refresh with the new data.
        });
    }

    @Override
// Called when the activity is finishing (e.g., user presses back or system destroys it).
// This method is essential for cleaning up resources, particularly background threads.
    protected void onDestroy(){
        super.onDestroy();
        // Check if the stopwatch thread exists and is running.
        if (startThread != null && startThread.isAlive()){
            startThread.interrupt(); // Signal the thread to stop.
            try {
                // Wait for the thread to finish gracefully (up to 1 second).
                startThread.join(1000);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt(); // Restore the interrupt status.
            }
        }
        startThread = null; // Clear the reference.
    }
}