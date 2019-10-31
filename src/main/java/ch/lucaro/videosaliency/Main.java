package ch.lucaro.videosaliency;


import org.vitrivr.cineast.core.config.CacheConfig;
import org.vitrivr.cineast.core.config.DecoderConfig;
import org.vitrivr.cineast.core.data.frames.AudioFrame;
import org.vitrivr.cineast.core.data.frames.VideoFrame;
import org.vitrivr.cineast.core.data.raw.CachedDataFactory;
import org.vitrivr.cineast.core.extraction.decode.video.FFMpegVideoDecoder;
import org.vitrivr.cineast.core.extraction.decode.video.FFMpegVideoEncoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class Main {

    private static ImagePipeline imagePipeline = new ImagePipeline(5, 3f, .1f);
    private static AudioPipeline audioPipeline = new AudioPipeline();
    private static SaliencyMask saliencyMask;


    public static void main(String[] args) throws IOException {

        System.setProperty("TF_CPP_MIN_LOG_LEVEL", "3");

        saliencyMask = new SaliencyMask();


        FFMpegVideoDecoder decoder = new FFMpegVideoDecoder();
        decoder.init(Path.of("query.mp4"), new DecoderConfig(), new CacheConfig());

        LinkedList<VideoFrame> frames = new LinkedList<>();

        VideoFrame frame = decoder.getNext();

        if (frame == null){
            return;
        }
        frames.add(frame);

        FFMpegVideoEncoder encoder = new FFMpegVideoEncoder(frame.getImage().getWidth(), frame.getImage().getHeight(), (int)frame.getDescriptor().getFps(), 44100, "out.mp4", true);

        while ((frame = decoder.getNext()) != null){
            frames.add(frame);
            if (frames.size() >= 21){
                processBatch(frames, encoder);
                while (frames.size() > 5){
                    frames.poll();
                }
            }
        }
        processBatch(frames, encoder);

        saliencyMask.close();
        encoder.close();

    }

    private static void processBatch(List<VideoFrame> frames, FFMpegVideoEncoder encoder){

        long startTime = System.currentTimeMillis();

        float[][][] masks = saliencyMask.process(frames.stream().map(VideoFrame::getImage).collect(Collectors.toList()));

        for (int i = 0; i < masks.length; ++i){
            BufferedImage out = imagePipeline.process(frames.get(i).getImage().getBufferedImage(), masks[i]);

            encoder.add(CachedDataFactory.DEFAULT_INSTANCE.newInMemoryMultiImage(out));

        }

        for (VideoFrame frame: frames){
            if(frame.getAudio().isPresent()){
                for(AudioFrame audioFrame : audioPipeline.process(frame.getAudio().get())) {
                    encoder.add(audioFrame);
                }
            }
        }

        System.out.println("---> Processed batch in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");

    }

}
