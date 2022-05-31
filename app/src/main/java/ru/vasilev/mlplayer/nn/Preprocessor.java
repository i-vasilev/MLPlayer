package ru.vasilev.mlplayer.nn;

import static ru.vasilev.mlplayer.utils.DataConverter.getDoubles;

import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;

public class Preprocessor {

    public double[] preprocess(FileDescriptor resource) throws IOException {
        double[] doubles = mfcc(resource);
        return doubles;
}

    private double[] mfcc(FileDescriptor resource) throws IOException {
        int sampleRate = 22050;
        //set parameters, And these are same as the python librosa library
        //window size
        int bufferSize = 512;
        //the step of two frame
        int bufferOverlap = bufferSize - 256;
        int fmin = 30;
        int fmax = 3000;//(int) (sampleRate*0.5);
        int n_cep_mel = 64;
        int n_mels = 64;
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
                    if (k[1] > new Random().nextInt(300)) {
                        float[] mfccOutput = mfcc.getMFCC();
                        mfccOutput = Arrays.copyOfRange(mfccOutput, 0,
                                                        mfccOutput.length);
                        double[] mfccDouble = getDoubles(mfccOutput);
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
        }

    }

}