package org.vitrivr.cineast.core.util.dsp.filter.frequency;

import org.apache.commons.math3.complex.Complex;
import org.vitrivr.cineast.core.util.dsp.fft.FFTUtil;

public class BandpassFilter implements  FrequencyDomainFilterInterface {

    private final float minFrequency, maxFrequency;
    private final int sampleRate;

    public BandpassFilter(float minFrequency, float maxFrequency, int sampleRate){
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        this.sampleRate = sampleRate;
    }

    @Override
    public Complex[] filter(Complex[] fftbins) {
        Complex[] copy = new Complex[fftbins.length];
        for (int i = 0; i < fftbins.length; ++i){
            copy[i] = new Complex(fftbins[i].getReal(), fftbins[i].getImaginary());
        }
        return filterInPlace(copy);
    }

    @Override
    public Complex[] filterInPlace(Complex[] fftbins) {
        int startIndex = FFTUtil.binIndex(minFrequency, fftbins.length, sampleRate);
        for (int i = 0; i <= startIndex; ++i){
            fftbins[i] = Complex.ZERO;
        }

        int endIndex = FFTUtil.binIndex(maxFrequency, fftbins.length, sampleRate);
        for (int i = endIndex; i < fftbins.length; ++i){
            fftbins[i] = Complex.ZERO;
        }
        return fftbins;
    }
}
