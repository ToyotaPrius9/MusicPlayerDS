                                                                                                                          package main.musicplayer;

import javafx.scene.control.*;
import javafx.scene.control.Button;

import java.util.*;

import java.io.File;


import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javafx.application.Platform;

public class controller implements Initializable {
    @FXML
    private Pane pane; // Define window
    @FXML
    private Label songLabel; // Define song name
    @FXML
    private Button playButton, pauseButton, backButton, nextButton, createPlayList, refreshView, sortAlphabeticalOrder, sortReverseAlphabeticalOrder, sortShuffle, like; //Define basic actions
    @FXML
    private ProgressBar songProgressBar; // Define the music progress bar
    @FXML
    private TextField searchBar; // For the search bar


    private Map<String, File> songMap; //For search data structure
    private ObservableList<String> playlistNames; // For playlist
    private Map<String, List<File>> playlists; // For playlist

    @FXML
    private ListView<File> songList; // To display the list of songs

    @FXML
    private ListView<String> playList; // To display the list of playlist names

    @FXML
    private ListView<String> favList; // To display liked songs


    private DoubleProperty progress; // The amount of progress, which would power the progress bar

    private File directory; // Just to read the music file
    private File[] files; // To put all music files in this array/list thing
    private List<File> songs; // To put all songs in a list
    private int songNumber; // Song index

    private ObservableList<File> filteredSongs; //For song updates to UI

    private Timer timer; // Timer, to keep track of music progress
    private TimerTask task; // For the tasks of the timer
    private boolean running; // Run status

    private Clip clip; // For some preset music capabilities

    private SortedList<File> sortedSongs; // displaying sorted order of songs to UI

    private HashMap<File, String> likedSongs; // Hashmap for storing liked songs


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) { // Function for reading all songs

        songs = new ArrayList<>(); // Make an array for containing songs
        songMap = new HashMap<>(); // Make a HashMap containing songs
        filteredSongs = FXCollections.observableArrayList(songMap.values()); // Take info from the HashMap, and use them here to display
        sortedSongs = new SortedList<>(filteredSongs, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        songList.setItems(sortedSongs);

        likedSongs = new HashMap<>();
        favList.setItems(FXCollections.observableArrayList(likedSongs.values()));



        directory = new File("music"); // the folder/file in this directory called "music" will be read
        files = directory.listFiles(); // make up the list
        sortedSongs = new SortedList<>(filteredSongs, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        songList.setItems(sortedSongs);

        if (files != null) { // if the file isn't empty, add the contents in them to "songs"
            for (File file : files) {
                songs.add(file);
                songMap.put(file.getName(), file);
                System.out.println(songs); // just to make sure that it reads all, the musics in the folder should show in terminal

            }
        }

        initializeClip(); // preset function to prepare to play audio when needed

        songLabel.setText(songs.get(songNumber).getName()); // Display name of music about to be played

        // Start the timer, or stop the timer, depending on when music is stopped or playing
        LineListener lineListener = event -> {
            if (event.getType() == LineEvent.Type.START) {
                startTimer();
            } else if (event.getType() == LineEvent.Type.STOP) {
                stopTimer();
                songProgressBar.setProgress(0);
            }
        };

        clip.addLineListener(lineListener); //Synchronizing progress bar with music

        progress = new SimpleDoubleProperty(0); // Progress is 0 by default, since music has not played yet
        songProgressBar.progressProperty().bind(progress); // When music starts, sync it with the progress bar


        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {// listener for the text of the searchBar
            filteredSongs.clear();// Clear the filteredSongs list, so that the searched ones would show later

            // Search for songs in the songMap based on the search query
            for (Map.Entry<String, File> entry : songMap.entrySet()) {
                String songName = entry.getKey(); //obtain the name of the song from the HashMap's key
                File songFile = entry.getValue(); //obtain the file of the song from the HashMap's value

                if (songName.toLowerCase().contains(newValue.toLowerCase())) {// if searched input matches song:
                    filteredSongs.add(songFile); // Display em
                }
            }


        });

        playlistNames = FXCollections.observableArrayList();
        playlists = new HashMap<>();

        playList.setItems(playlistNames);
        // When a song from list is clicked, Play it!
        songList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // if clicked once
                File selectedSong = songList.getSelectionModel().getSelectedItem(); // read the selected song
                if (selectedSong != null) { // if the selected song is not empty:
                    stopTimer(); // stop the timer
                    clip.stop(); // stop the music
                    songNumber = songs.indexOf(selectedSong); // update the song number
                    initializeClip(); // prepare the music
                    playMedia(); // play the music
                    startTimer(); // start the timer
                    songLabel.setText(selectedSong.getName()); // update the name of current playing music


                }
            }
        });

        // Context menu for playlist
        ContextMenu playlistContextMenu = new ContextMenu(); // For UI rightclick
        ContextMenu songContextMenu = new ContextMenu(); // For UI rightclick
        MenuItem addToPlaylistItem = new MenuItem("Add to Playlist"); // When rightclicked, the option displays with that message
        songContextMenu.getItems().add(addToPlaylistItem); // add the item of which you selected into the playlist
        songList.setContextMenu(songContextMenu); // changes the songList display to show playlist items

        playList.setOnMouseClicked(event -> { // when mouse is clicked:
            if (event.getButton() == MouseButton.SECONDARY) { // if it's a rightclick:
                playlistContextMenu.show(playList, event.getScreenX(), event.getScreenY()); //popup the context menu
            } else {
                playlistContextMenu.hide(); // if not, don't show it
            }
        });

        addToPlaylistItem.setOnAction(event -> { // an action for when adding to a playlist
            File selectedSong = songList.getSelectionModel().getSelectedItem(); // read the song which has been selected
            if (selectedSong != null) { // if the song exists:
                TextInputDialog dialog = new TextInputDialog(); //open up a dialogue box
                dialog.setTitle("Add to Playlist"); // title the dialogue box
                dialog.setHeaderText(null); // no header
                dialog.setContentText("Enter the name of the playlist:"); // display message to ask for which playlist to add

                Optional<String> result = dialog.showAndWait(); // keep the dialogue up
                result.ifPresent(playlistName -> {
                    if (playlists.containsKey(playlistName)) { // if playlist exists
                        List<File> playlist = playlists.get(playlistName); // get the name of the playlist
                        playlist.add(selectedSong); // add the selected song into the playlist
                    } else {
                        // Playlist does not exist, create a new playlist and add the song to it
                        List<File> playlist = new ArrayList<>();
                        playlist.add(selectedSong);
                        playlists.put(playlistName, playlist);
                        playlistNames.add(playlistName);
                    }
                });
            }
        });

        playList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // if clicked once
                String selectedPlaylist = playList.getSelectionModel().getSelectedItem(); // get the name of the playlist
                if (selectedPlaylist != null) { // if playlist exists
                    List<File> playlist = playlists.get(selectedPlaylist); //initiate playlist
                    songList.setItems(FXCollections.observableArrayList(playlist)); // display playlist music into songList
                }
            }
        });


    }


    @FXML
    private void refreshSongList(ActionEvent event) {
        songList.setItems(sortedSongs); // Set the song list to display all the songs
        playList.getSelectionModel().clearSelection(); // Clear the playlist selection
    }


    private void initializeClip() { //extra things to make the song work
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(songs.get(songNumber)); // Prepare for Audio!
            clip = AudioSystem.getClip(); // Steady the Audio!
            clip.open(audioInputStream); // Play the Audio!
        } catch (UnsupportedAudioFileException | IOException |
                 LineUnavailableException e) { // If error, don't stop! but rather print the error to terminal anyhow
            e.printStackTrace();// preset function to print errors
        }
    }

    public void playMedia() { // To play da musik!
        if (clip != null && !clip.isRunning()) { //if clip isn't empty and isn't running, start the music and the timer
            clip.start(); // Start the musik
            startTimer(); // Start the timer
        }
    }

    public void pauseMedia() { // To stop da musik :(
        if (clip != null && clip.isRunning()) { // if clip not empty and IS running:
            clip.stop(); // Stop the musik
            stopTimer(); // stop the timer
        }
    }

    public void nextMedia() { // To play da next musik!
        if (clip != null && clip.isRunning()) { // if clip not empty and IS running:
            clip.stop(); // Stop the musik
            stopTimer(); // stop the timer

        }

        int currentIndex = sortedSongs.indexOf(songs.get(songNumber));
        int nextIndex = (currentIndex + 1) % sortedSongs.size();
        File nextSong = sortedSongs.get(nextIndex);

        songNumber = songs.indexOf(nextSong);
        initializeClip();
        playMedia();
        startTimer();
        songLabel.setText(nextSong.getName());

    }

    public void backMedia() {
        if (clip != null && clip.isRunning()) { // Play the previous musik
            clip.stop(); // Stop the musik
            stopTimer(); // Stop the timer


        }

        int currentIndex = sortedSongs.indexOf(songs.get(songNumber));
        int previousIndex = (currentIndex - 1 + sortedSongs.size()) % sortedSongs.size();
        File previousSong = sortedSongs.get(previousIndex);

        songNumber = songs.indexOf(previousSong);
        initializeClip();
        playMedia();
        startTimer();
        songLabel.setText(previousSong.getName());

    }

    public void startTimer() { // Starting the timer, kinda complex function, had to be like this since javafxMedia method keeps giving errors
        if (timer == null) { // if the timer is empty:
            timer = new Timer(); // make the timer
            task = new TimerTask() {
                public void run() { // define the task of the timer
                    long position = clip.getMicrosecondPosition(); // get the microsecond value of the current playing point of music
                    long duration = clip.getMicrosecondLength(); // get the microsecond value of the entire duration of the music
                    double newProgress = (double) position / duration; // divide em both and store it to a variable called newProgress

                    long positionInSeconds = position / 1_000_000; // Convert to seconds from microseconds
                    long durationInSeconds = duration / 1_000_000;

                    System.out.println("Position: " + positionInSeconds + " seconds"); // just to print in the terminal to see if its working correctly
                    System.out.println("Duration: " + durationInSeconds + " seconds");

                    Platform.runLater(() -> { // i need to use this runLater thing cuz JavaFX is mean to me and loves giving me errors, this fights the error
                        progress.set(newProgress); // to keep updating the song progress variable
                    });
                }
            };

            timer.scheduleAtFixedRate(task, 0, 1000); // Schedule the task to run every 1000 microsecond, or 1 second
        }
    }


    public void stopTimer() { // function to stop the timer, this is way less complex
        if (timer != null) { // if timer is not empty:
            timer.cancel(); // cancel da timer
            timer = null; // make da timer 0
            task = null; // make da task empty
        }
    }

    @FXML
    private void handleCreatePlaylist(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(); // Create a dialog box for user input
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter the name of the playlist:");

        Optional<String> result = dialog.showAndWait(); // Show the dialog and wait for user input
        result.ifPresent(playlistName -> { // If the user provided a playlist name:
            if (!playlistNames.contains(playlistName)) { // Check if the playlist name is not already in use
                List<File> playlist = new ArrayList<>(); // Create a new playlist
                playlists.put(playlistName, playlist); // Add the playlist to the playlists map
                playlistNames.add(playlistName); // Add the playlist name to the playlistNames list
            }
        });
    }

    @FXML
    private void sortAlphabeticalOrder(ActionEvent event) {
        sortedSongs.setComparator(Comparator.comparing(File::getName));
        songList.setItems(sortedSongs);
    }

    @FXML
    private void sortReverseAlphabeticalOrder(ActionEvent event) {
        sortedSongs.setComparator(Comparator.comparing(File::getName).reversed());
        songList.setItems(sortedSongs);

    }


    @FXML
    private void sortShuffle(ActionEvent event) {
        List<File> shuffledSongs = new ArrayList<>(songs);
        Collections.shuffle(shuffledSongs);
        sortedSongs = new SortedList<>(FXCollections.observableArrayList(shuffledSongs));
        songList.setItems(sortedSongs);
    }





    private long getMusicDuration(File file) {
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            Map<?, ?> properties = fileFormat.properties();
            String key = "duration";
            if (properties.containsKey(key)) {
                return (long) properties.get(key) / 1000; // Convert to seconds
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    @FXML
    private void likeMedia(ActionEvent event) {
        File currentSong = songs.get(songNumber); // Get the current song
        String currentSongName = currentSong.getName(); // Get the current song's name
        likedSongs.put(currentSong, currentSongName); // Put the current song and irs name to liked songs


        favList.setItems(FXCollections.observableArrayList(likedSongs.values())); // Update the favList ListView
    }
}