package ru.vasilev.mlplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.stream.IntStream;

import ru.vasilev.mlplayer.data.Genre;

public class EqualizerActivity extends AppCompatActivity {
    private Genre genre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);
        findViewById(R.id.save).setOnClickListener(this::save);
        String[] strings = Arrays.stream(Genre.values())
                                 .map(Enum::name)
                                 .toArray(String[]::new);
        ArrayAdapter<Object> formArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                                                                   R.layout.genre,
                                                                   strings);
        Spinner spinner = (Spinner) findViewById(R.id.genre);
        spinner.setAdapter(formArrayAdapter);

        Intent intent = getIntent();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                load(Genre.values()[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        genre = (Genre) intent.getExtras()
                                    .get("genre");
        load(genre);
        int genre_index = IntStream.range(0, Genre.values().length)
                                   .filter(i -> genre.equals(Genre.values()[i]))
                                   .findFirst() // first occurrence
                                   .orElse(-1);
        spinner.setSelection(genre_index);
    }

    private void save(View view) {
        byte[] bytes = new byte[5];
        bytes[0] = (byte) ((SeekBar) findViewById(R.id.seekBar60)).getProgress();
        bytes[1] = (byte) ((SeekBar) findViewById(R.id.seekBar100)).getProgress();
        bytes[2] = (byte) ((SeekBar) findViewById(R.id.seekBar1k)).getProgress();
        bytes[3] = (byte) ((SeekBar) findViewById(R.id.seekBar3k)).getProgress();
        bytes[4] = (byte) ((SeekBar) findViewById(R.id.seekBar12k)).getProgress();
        getIntent().putExtra("genre", genre);
        finish();
    }

    private void load(Genre genre) {
        this.genre = genre;
        ((SeekBar) findViewById(R.id.seekBar60)).setProgress(genre.getEqualizer()[0]);
        ((SeekBar) findViewById(R.id.seekBar100)).setProgress(genre.getEqualizer()[1]);
        ((SeekBar) findViewById(R.id.seekBar1k)).setProgress(genre.getEqualizer()[2]);
        ((SeekBar) findViewById(R.id.seekBar3k)).setProgress(genre.getEqualizer()[3]);
        ((SeekBar) findViewById(R.id.seekBar12k)).setProgress(genre.getEqualizer()[4]);
    }
}