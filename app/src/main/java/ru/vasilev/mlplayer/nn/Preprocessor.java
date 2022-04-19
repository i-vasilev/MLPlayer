package ru.vasilev.mlplayer.nn;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;
import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class Preprocessor {
    public void convertToMp3(String resource, Context context) {
        File file = new File(resource);
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                Log.d("converter", "CONVERTED");
            }

            @Override
            public void onFailure(Exception error) {
                Log.e("converter", "failed", error);
            }
        };
        AndroidAudioConverter.with(context)
                             .setFile(file)
                             .setFormat(AudioFormat.WAV)
                             .setCallback(callback)
                             .convert();
    }

    public double[] preprocess(FileDescriptor resource) throws IOException {
        int sampleRate = 44100;
        //set parameters, And these are same as the python librosa library
        //window size
        int bufferSize = 2048;
        //the step of two frame
        int bufferOverlap = bufferSize - 512;
        int fmin = 30;
        int fmax = 3000;//(int) (sampleRate*0.5);
        int n_cep_mel = 64;
        int n_mels = 128;
        final double[][] mfccList = new double[173][];
        try (InputStream is = new FileInputStream(resource)) {

            AudioDispatcher dispatcher = new AudioDispatcher(
                    new UniversalAudioInputStream(
                            is,
                            new TarsosDSPAudioFormat(
                                    sampleRate, 16, 1, true, true)
                    ),
                    bufferSize,
                    bufferOverlap);
            final int[] k = {0, 0};
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
                    if (k[1] > 200) {
                        float[] mfccOutput = mfcc.getMFCC();
                        mfccOutput = Arrays.copyOfRange(mfccOutput, 0,
                                                        mfccOutput.length);
                        double[] mfccDouble = new double[mfccOutput.length];
                        for (int i = 0; i < mfccOutput.length; i++) {
                            mfccDouble[i] = mfccOutput[i];
                        }
                        mfccList[k[0]++] = mfccDouble;
                        if (k[0] == 173)
                            dispatcher.stop();
                    } else {
                        k[1]++;
                    }
                    return true;
                }
            });
            dispatcher.run();// starts a new thread

            return Arrays.stream(mfccList)
                         .flatMapToDouble(Arrays::stream)
                         .toArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new double[0];
    }

}