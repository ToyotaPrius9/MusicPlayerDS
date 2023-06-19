module main.musicplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.kordamp.bootstrapfx.core;

    requires java.desktop; // for sound in javax, since sound from javaFX media keeps giving me errors, package cant be accessed.

    opens main.musicplayer to javafx.fxml;
    exports main.musicplayer;
}