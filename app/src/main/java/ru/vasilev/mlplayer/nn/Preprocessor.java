package ru.vasilev.mlplayer.nn;

import android.util.Log;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;

public class Preprocessor {
    public void preprocess(String resource) throws IOException {
        int sampleRate = 44100;
        //set parameters, And these are same as the python librosa library
        //window size
        int bufferSize = 2048;
        //the step of two frame
        int bufferOverlap = bufferSize - 512;
        int fmin = 30;
        int fmax = 3000;//(int) (sampleRate*0.5);
        int n_cep_mel = 40;
        int n_mels = 128;
        List<float[]> mfccList = new ArrayList<>();
        try (InputStream is = new FileInputStream(resource)) {

            AudioDispatcher dispatcher = new AudioDispatcher(
                    new UniversalAudioInputStream(
                            is,
                            new TarsosDSPAudioFormat(
                                    sampleRate, 16, 1, true, true)
                    ),
                    bufferSize,
                    bufferOverlap);
            final MFCC mfcc = new MFCC(bufferSize, sampleRate, n_cep_mel, n_mels, fmin,
                                       fmax);
            dispatcher.addAudioProcessor(mfcc);
            dispatcher.addAudioProcessor(new AudioProcessor() {

                @Override
                public void processingFinished() {
                    Log.d("CXY", "finish");
                }

                @Override
                public boolean process(AudioEvent audioEvent) {
                    //fetchng MFCC array and removing the 0th index because its energy coefficient and florian asked to discard
                    float[] mfccOutput = mfcc.getMFCC();
                    mfccOutput = Arrays.copyOfRange(mfccOutput, 0,
                                                    mfccOutput.length);

                    mfccList.add(mfccOutput);
                    return true;
                }
            });
            dispatcher.run();// starts a new thread
            System.out.println(mfccList.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}