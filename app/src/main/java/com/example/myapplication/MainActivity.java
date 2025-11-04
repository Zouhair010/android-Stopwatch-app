package com.example.myapplication; // Declares the package where this class belongs.

import android.os.Bundle; // Imports the class for passing data between activities.
import android.widget.Button; // Imports the Button widget class (though not explicitly used by name, it's often inferred).
import android.widget.ListView; // Imports the ListView widget class for displaying a scrollable list.
import android.widget.TextView; // Imports the TextView widget class for displaying text.

import androidx.activity.EdgeToEdge; // Imports class for managing full-screen display (edge-to-edge).
import androidx.appcompat.app.AppCompatActivity; // Base class for activities that use the ActionBar feature.
import androidx.core.graphics.Insets; // Imports class for managing window insets (safe areas).
import androidx.core.view.ViewCompat; // Imports compatibility class for View functionality.
import androidx.core.view.WindowInsetsCompat; // Imports compatibility class for window insets.
import android.widget.Toast; // Imports the Toast class for displaying brief messages.

import java.util.ArrayList; // Imports the dynamic array (List) implementation.
import java.util.Calendar; // Imports the Calendar abstract class for date and time calculations (though GregorianCalendar is used).
import java.util.GregorianCalendar; // Imports the concrete class for calendar calculations.

import android.widget.ArrayAdapter; // Imports the adapter class to link data to a ListView.

// Main activity class, extending the base AppCompatActivity for an Android screen.
public class MainActivity extends AppCompatActivity {

    // Manages the execution of the stopwatch timer logic in the background.
    private Thread startThread ;
    // Manages the execution of the continuous date/time display in the background.
    private Thread dateTimeThread;
    // Tracks whether the stopwatch is currently running.
    private static boolean isStarted;
    // Tracks whether the stopwatch has been reset.
    private static boolean isReStarted;
    // Tracks whether the stopwatch is in a paused state.
    private static boolean isPaused;
    // Stores the current stopwatch time as a formatted string.
    private static String currentTime;
    // Stores the elapsed time in milliseconds when the stopwatch is paused. Essential for resuming correctly.
    private static long timeDifference = 0;
    // Data store for lap times and the adapter to link it to the ListView.
    private ListView listView;
    // The list that holds the lap time strings.
    private static ArrayList<String> dataList;
    // The adapter that connects dataList to the ListView.
    private static ArrayAdapter<String> adapter;

    // Method to continuously update a TextView with the current date and time.
    private void showDateTime(TextView datetimeScreen) throws InterruptedException{;
        while (true){ // Infinite loop for continuous updates.
            // Updates the UI on the main thread with the current time.
            runOnUiThread(() -> datetimeScreen.setText(
                    new GregorianCalendar().getTime().toString()
            ));
            Thread.sleep(1000); // Pauses for 1 second before the next update.
        }
    }
    /**
     * Converts milliseconds into a time format HH:mm:ss.SSS.
     * @param millis The duration in milliseconds.
     * @return A string representing the time.
     */
    private String millisToTime(long millis){
        long seconds = millis/1000; // Calculate total seconds.
        long remainingMillis = millis % 1000; // Calculate remaining milliseconds.
        long minutes = seconds/60; // Calculate total minutes.
        seconds %= 60; // Calculate remaining seconds (0-59).
        long hours = minutes/60; // Calculate total hours.
        minutes %= 60; // Calculate remaining minutes (0-59).
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
     * @throws InterruptedException if the thread is interrupted.
     */
    private void play(TextView textView) throws InterruptedException {
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
            // Calculate total elapsed time since the effective start time.
            currentTime = millisToTime(System.currentTimeMillis()-startTimeInMillis);
            // Update the UI with the new time. Must run on the UI thread.
            runOnUiThread(() -> textView.setText(currentTime));
            //textView.setText(currentTime); // Alternative non-UI-thread call.
            // Pause the thread for a short duration to prevent high CPU usage
            // and to create a periodic update cycle (10 updates per second).
            Thread.sleep(100);
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
    private static void pause() {
        // Set flags to stop the timer loop in the play() method.
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
        timeDifference = 0; // Reset accumulated time difference.
        currentTime = "00:00:00.000"; // Reset displayed time string.
        textView.setText(currentTime); // Update the TextView with the reset time.
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

        // Initialize and start the background thread for the date/time display.
        dateTimeThread = new Thread(){@Override public void run(){ try {
            // Call the method to continuously update the time TextView
            showDateTime(findViewById(R.id.datetimeScreen));
        } catch (InterruptedException e) {
            // Print the stack trace if the thread is interrupted
            e.printStackTrace();
        }}};
        dateTimeThread.start();

        // Initialize the list for lap times and its adapter for the ListView.
        listView = findViewById(R.id.listView); // Get the ListView from the layout.
        dataList = new ArrayList<>(); // Initialize the data list.
        // Initialize the adapter to link dataList to listView, using a simple layout.
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter); // Set the adapter on the ListView.

        // Set a click listener for the Start button.
        findViewById(R.id.startBtn).setOnClickListener(v -> {
            // Only start if the stopwatch is not already running.
            if (!isStarted){
                // Initialize and start the background thread for the stopwatch.
                startThread = new Thread(){@Override public void run(){ try {
                    // Call the method to continuously update the stopwatch TextView
                    play(findViewById(R.id.screen));
                } catch (InterruptedException e) {
                    // Print the stack trace if the thread is interrupted
                    e.printStackTrace();
                }}};
                startThread.start(); // Begin the stopwatch thread execution.
                // Display a brief confirmation message.
                Toast.makeText(this, "GO!!!!!", Toast.LENGTH_SHORT).show();
            }
        });

        // Set a click listener for the Pause button.
        findViewById(R.id.pauseBtn).setOnClickListener(v ->{
            // Only pause if the stopwatch is currently running.
            if (isStarted) {
                pause(); // Set the flag to stop the thread's loop.
                try{
                    // Wait for the stopwatch thread to finish its current execution cycle and terminate.
                    startThread.join();
                    startThread = null; // Clear the thread reference.
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                // Display a brief confirmation message.
                Toast.makeText(this, "the stop watch paussed.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set a click listener for the Restart button.
        findViewById(R.id.restartBtn).setOnClickListener(v ->{
            // Restart is allowed if the stopwatch has been started or is currently paused.
            if (isPaused || isStarted) {
                restart(findViewById(R.id.screen)); // Reset the stopwatch state and display.
                // Clear the lap times list and update the adapter.
                dataList.clear();
                adapter.notifyDataSetChanged();
                // If the stopwatch was running (not paused), we need to stop the thread.
                if (!isPaused){
                    try{
                        // Wait for the stopwatch thread to terminate.
                        startThread.join();
                        startThread = null; // Clear the thread reference.
                    }
                    catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                // Display a brief confirmation message.
                Toast.makeText(this, "the stop watch restarted.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set a click listener for the Lap button.
        findViewById(R.id.lapBtn).setOnClickListener(v -> {
            // Only record a lap if the current time is not null and not zero.
            if (currentTime!=null&&!currentTime.equals("00:00:00.000")) {
                dataList.add(currentTime); // Add the current time to the lap list.
                adapter.notifyDataSetChanged(); // Tell the ListView to refresh with the new data.
            }
        });
    }

    @Override
// Called when the activity is finishing (e.g., user presses back or system destroys it).
// This method is essential for cleaning up resources, particularly background threads.
    protected void onDestroy(){
        super.onDestroy();
        // Check if the date/time thread exists and is running.
        if (dateTimeThread != null && dateTimeThread.isAlive()){
            dateTimeThread.interrupt(); // Signal the thread to stop.
            try {
                // Wait for the thread to finish gracefully (up to 1 second).
                dateTimeThread.join(1000);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt(); // Restore the interrupt status.
            }
        }
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
        dateTimeThread = null; // Clear the reference.
        startThread = null; // Clear the reference.
    }
}