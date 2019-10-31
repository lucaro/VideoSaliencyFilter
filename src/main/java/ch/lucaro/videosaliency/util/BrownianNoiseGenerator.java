package ch.lucaro.videosaliency.util;

import java.util.Random;

public class BrownianNoiseGenerator {

    private final Random random;
    private final double amplitude;
    private double lastSample = 0;


    public BrownianNoiseGenerator(double amplitude) {
        this.amplitude = amplitude;
        this.random = new Random(Double.doubleToRawLongBits(amplitude));
    }

    private synchronized double next(){
        lastSample = (lastSample + random.nextDouble() - 0.5f) * 0.99;
        if (lastSample > 1f){
            lastSample = 1f;
        } else if (lastSample < -1f){
            lastSample = -1f;
        }
        return lastSample;
    }

    public double nextSample(){
        return (this.amplitude * next());
    }

}
