package ru.vasilev.mlplayer;

import static ru.vasilev.mlplayer.R.id.openEqualizer;
import static ru.vasilev.mlplayer.R.id.openFile;
import static ru.vasilev.mlplayer.R.id.play_pause;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import ru.vasilev.mlplayer.nn.Preprocessor;

public class MainActivity extends AppCompatActivity {
    public static final int PERMISSION_EXTERNAL_STORAGE = 1;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        findViewById(openFile).setOnClickListener(this::clickBtn);
        findViewById(play_pause).setOnClickListener(this::clickBtn);
        findViewById(openEqualizer).setOnClickListener(this::clickBtn);
    }

    private void clickBtn(View view) {
        switch (view.getId()) {
            case openFile:
                ActivityCompat.requestPermissions(this,
                                                  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                  PERMISSION_EXTERNAL_STORAGE);
                OpenFileDialog openFileDialog = new OpenFileDialog(MainActivity.this)
                        .setOpenDialogListener(this::openSong);
                openFileDialog.show();
                break;
            case play_pause:
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                    } else {
                        mediaPlayer.start();
                    }
                }
                break;
            case openEqualizer:
                Intent intent = new Intent(getApplicationContext(), EqualizerActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void openSong(String fileName) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            initLayout(fileName);
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            Runnable task = () -> {
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setProgress(mediaPlayer.getCurrentPosition());
                TextView currentPosition = findViewById(R.id.current_position);
                currentPosition.setText(getFormattedTime(mediaPlayer.getCurrentPosition()));
            };
            new AndroidFFMPEGLocator(this);
//            AudioClassifier.AudioClassifierOptions options =
//                    AudioClassifier.AudioClassifierOptions.builder()
//                                                          .setBaseOptions(BaseOptions.builder()
//                                                                                     .useGpu()
//                                                                                     .build())
//                                                          .setMaxResults(1)
//                                                          .build();
////            org.tensorflow.lite.support.audio.TensorAudio.create(AudioFormat.ENCODING_MP3, )
//            AudioClassifier classifier =
//                    AudioClassifier.createFromFileAndOptions(getApplicationContext(), "model.tflite", options);
//            TensorAudio inputTensorAudio = classifier.createInputTensorAudio();
//            inputTensorAudio.load();

            Preprocessor preprocessor = new Preprocessor();
            preprocessor.preprocess(fileName);

            scheduledExecutorService.scheduleAtFixedRate(task, 0, 500, TimeUnit.MILLISECONDS);
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFormattedTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("mm:ss");
        Date date = new Date(time);
        return dateFormat.format(date);
    }

    private void initLayout(String fileName) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(fileName);
        setImage(metadataRetriever);
        String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String genre = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        TextView viewById = findViewById(R.id.songname);
        viewById.setText(String.format("%s - %s", artist, title));
        ProgressBar currentPosition = findViewById(R.id.progressBar);
        int max = Integer.parseInt(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        currentPosition.setMax(max);
        TextView finish = findViewById(R.id.finish);
        finish.setText(getFormattedTime(max));
        TextView genreTextView = findViewById(R.id.genre);
        genreTextView.setText(genre);
    }

    private void setImage(MediaMetadataRetriever metadataRetriever) {
        byte[] embeddedPicture = metadataRetriever.getEmbeddedPicture();
        if (embeddedPicture != null && embeddedPicture.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length);
            ImageView image = findViewById(R.id.album_art);
            image.setImageBitmap(Bitmap.createScaledBitmap(bmp, image.getWidth(), image.getHeight(), false));
        } else {
            ImageView image = findViewById(R.id.album_art);
            image.setImageResource(R.drawable.musicnote);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}