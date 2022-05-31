package ru.vasilev.mlplayer;

import static ru.vasilev.mlplayer.R.id.genre;
import static ru.vasilev.mlplayer.R.id.openEqualizer;
import static ru.vasilev.mlplayer.R.id.openFile;
import static ru.vasilev.mlplayer.R.id.play_pause;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.vasilev.mlplayer.data.Genre;
import ru.vasilev.mlplayer.ml.Model;
import ru.vasilev.mlplayer.nn.Preprocessor;

public class MainActivity extends AppCompatActivity {
    public static final int PERMISSION_EXTERNAL_STORAGE = 1;
    private MediaPlayer mediaPlayer;
    private Genre genre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPlayer = new MediaPlayer();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        findViewById(openFile).setOnClickListener(this::clickBtn);
        findViewById(play_pause).setOnClickListener(this::clickBtn);
        findViewById(openEqualizer).setOnClickListener(this::clickBtn);
    }

    private void openFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");

        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, 1);
    }

    private void clickBtn(View view) {
        switch (view.getId()) {
            case openFile:
                ActivityCompat.requestPermissions(this,
                                                  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                  PERMISSION_EXTERNAL_STORAGE);

                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, "Select a file"), 123);
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
                intent = new Intent(getApplicationContext(), EqualizerActivity.class);
                intent.putExtra("genre", genre);
                startActivity(intent);
                break;
        }
    }

    private void openSong(AssetFileDescriptor fileName) {
        try {
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            mediaPlayer.stop();

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            initLayout(fileName);
            Runnable task = () -> {
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setProgress(mediaPlayer.getCurrentPosition());
                TextView currentPosition = findViewById(R.id.current_position);
                currentPosition.setText(getFormattedTime(mediaPlayer.getCurrentPosition()));
            };


            Preprocessor preprocessor = new Preprocessor();
            double[] preprocess = preprocessor.preprocess(fileName.getFileDescriptor());
            float[] a = new float[preprocess.length];
            for (int i = 0; i < preprocess.length; i++) {
                a[i] = (float) preprocess[i];
            }
//            float[][] a = preprocessor.preprocess(fileName.getFileDescriptor());
            genre = getGenre(fileName);
            TextView genreTextView = findViewById(R.id.genre);
            if (genre != null) {
                genreTextView.setText(genre.toString());
            }else {
                try {
                    Model model = Model.newInstance(getApplicationContext());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 64, 173, 1}, DataType.FLOAT32);
                    inputFeature0.loadArray(a);

                    // Runs model inference and gets result.
                    Model.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                    int maxAt = 0;
                    float[] array = outputFeature0.getFloatArray();
                    for (int i = 0; i < array.length; i++) {
                        maxAt = array[i] > array[maxAt] ? i : maxAt;
                    }

                    genreTextView.setText(Genre.values()[maxAt].toString());

                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Ошибка")
                           .setMessage("Ошибка нейросети")
                           .setIcon(R.drawable.open_file)
                           .setPositiveButton("ОК", (dialog, id) -> {
                               // Закрываем окно
                               dialog.cancel();
                           });
                    builder.create()
                           .show();
                }
            }

//            org.tensorflow.lite.support.audio.TensorAudio.create(AudioFormat.ENCODING_MP3, )
//            AudioClassifier classifier =
//                    AudioClassifier.createFromFileAndOptions(getApplicationContext(), "model.tflite", options);
//            TensorAudio inputTensorAudio = classifier.createInputTensorAudio();
//            inputTensorAudio.load(a);

            scheduledExecutorService.scheduleAtFixedRate(task, 0, 500, TimeUnit.MILLISECONDS);
            mediaPlayer.start();
        } catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Ошибка")
                   .setMessage("Ошибка при загрузке аудиофайла")
                   .setIcon(R.drawable.open_file)
                   .setPositiveButton("ОК", (dialog, id) -> {
                       // Закрываем окно
                       dialog.cancel();
                   });
            builder.create()
                   .show();
        }
    }

    private String getFormattedTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("mm:ss");
        Date date = new Date(time);
        return dateFormat.format(date);
    }

    private void initLayout(AssetFileDescriptor fileName) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(fileName.getFileDescriptor());
        setImage(metadataRetriever);
        String artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        TextView viewById = findViewById(R.id.songname);
        viewById.setText(String.format("%s - %s", artist, title));
        ProgressBar currentPosition = findViewById(R.id.progressBar);
        int max = Integer.parseInt(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        currentPosition.setMax(max);
        TextView finish = findViewById(R.id.finish);
        finish.setText(getFormattedTime(max));
//        genreTextView.setText(genre);
    }

    @NonNull
    private Genre getGenre(AssetFileDescriptor fileName) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(fileName.getFileDescriptor());
        String genre = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        try {
            return Genre.valueOf(genre.toUpperCase());
        } catch (Exception e) {
            return null;
        }
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
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            try {
                AssetFileDescriptor assetFileDescriptor = getApplicationContext().getContentResolver()
                                                                                 .openAssetFileDescriptor(selectedfile, "r");
                openSong(assetFileDescriptor);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}