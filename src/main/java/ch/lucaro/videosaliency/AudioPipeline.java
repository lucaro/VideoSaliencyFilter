package ch.lucaro.videosaliency;

import org.vitrivr.cineast.core.data.frames.AudioDescriptor;
import org.vitrivr.cineast.core.data.frames.AudioFrame;
import org.vitrivr.cineast.core.util.dsp.fft.FFT;
import org.vitrivr.cineast.core.util.dsp.fft.windows.RectangularWindow;
import org.vitrivr.cineast.core.util.dsp.filter.frequency.BandpassFilter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioPipeline {

    private AudioFrame buffer = null;

    private final int bufferSize;
    private final float minFrequency, maxFrequency;

    private FFT fft = new FFT();
    private RectangularWindow window = new RectangularWindow();

    public AudioPipeline(){
        this.bufferSize = 1024;
        this.minFrequency = 100;
        this.maxFrequency = 2000;
    }

    public List<AudioFrame> process(AudioFrame input){
        if (input == null && buffer == null){
            return Collections.emptyList();
        }

        if (buffer == null){
            buffer = new AudioFrame(input);
        } else if (input != null){
            buffer.append(input);
        }
        ArrayList<AudioFrame> frames = new ArrayList<>(buffer.numberOfSamples() / bufferSize);

        while (buffer.numberOfSamples() > bufferSize){
            frames.add(processFrame(buffer.split(bufferSize)));
        }

        if (input == null){ //flush
            frames.add(processFrame(buffer));
        }

        return frames;
    }

    private AudioFrame processFrame(AudioFrame frame){
        BandpassFilter filter = new BandpassFilter(minFrequency, maxFrequency, (int) frame.getSamplingrate());
        fft.forward(getMeanSamplesAsDouble(frame), frame.getSamplingrate(), window);
        fft.applyFilter(filter);
        return fromSamples(fft.inverse(), (int) frame.getSamplingrate());

    }


    private static double[] getMeanSamplesAsDouble(AudioFrame frame) {
        double[] samples = new double[frame.numberOfSamples()];
            for (int sample = 0; sample < frame.numberOfSamples(); sample++) {
                samples[sample] = frame.getMeanSampleAsDouble(sample);
            }
        return samples;
    }

        private static AudioFrame fromSamples(double[] samples, int sampleRate) {

            ByteBuffer byteBuf = ByteBuffer.allocate(2 * samples.length);
            for (int i = 0; i < samples.length; ++i) {
                short s = (short) (samples[i] * Short.MAX_VALUE);
                byteBuf.put((byte) ((s) & 0xff));
                byteBuf.put((byte) ((s >> 8) & 0xff));
            }
            return new AudioFrame(0, 0, byteBuf.array(), new AudioDescriptor(sampleRate, 1, samples.length / sampleRate));
        }

}
