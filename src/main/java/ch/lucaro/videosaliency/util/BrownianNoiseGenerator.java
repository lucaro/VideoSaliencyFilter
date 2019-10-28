package ch.lucaro.videosaliency.util;

import java.util.Random;

public class BrownianNoiseGenerator {

    private final Random random;
    private final short amplitude;
    private float lastSample = 0;


    public BrownianNoiseGenerator(short amplitude) {
        this.amplitude = amplitude;
        this.random = new Random(amplitude);
    }

    private synchronized float next(){
        lastSample += (random.nextFloat() * 2f - 1f);
        if (lastSample > 1f){
            lastSample = 1f;
        } else if (lastSample < -1f){
            lastSample = -1f;
        }
        return lastSample;
    }

    private short nextSample(){
        return (short) (this.amplitude * next());
    }

    public short[] getSamples(int sampleCount){
        if (sampleCount <= 0){
            return new short[0];
        }
        short[] samples = new short[sampleCount];

        for (int i = 0; i < sampleCount; ++i){
            samples[i] = nextSample();
        }

        return samples;
    }

}
