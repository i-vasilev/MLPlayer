package ru.vasilev.mlplayer.utils;

import androidx.annotation.NonNull;

public abstract class DataConverter {
    @NonNull
    public static double[] getDoubles(float[] mfccOutput) {
        double[] mfccDouble = new double[mfccOutput.length];
        for (int i = 0; i < mfccOutput.length; i++) {
            mfccDouble[i] = mfccOutput[i];
        }
        return mfccDouble;
    }
}
