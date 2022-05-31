package ru.vasilev.mlplayer.data;

public enum Genre {
    BLUES(new short[]{4, 3, 2, 3, 4}),
    CLASSICAL(new short[]{4, 3, 2, 3, 4}),
    COUNTRY(new short[]{4, 3, 2, 3, 4}),
    DISCO(new short[]{4, 3, 2, 3, 4}),
    HIPHOP(new short[]{4, 3, 2, 3, 4}),
    JAZZ(new short[]{2, 0, 3, 2, 2}),
    METAL(new short[]{2, -1, 2, -4, -4}),
    POP(new short[]{1, 3, 4, 2, 1}),
    REGGAE(new short[]{4, 3, 2, 3, 4}),
    ROCK(new short[]{4, 3, 2, 3, 4});
    private final short[] equalizer;

    Genre(short[] equalizer) {
        this.equalizer = equalizer;
    }

    public short[] getEqualizer() {
        return equalizer;
    }
}
