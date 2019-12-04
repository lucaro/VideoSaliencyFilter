package ch.lucaro.videosaliency;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.vitrivr.cineast.core.config.CacheConfig;
import org.vitrivr.cineast.core.config.DecoderConfig;
import org.vitrivr.cineast.core.data.frames.AudioFrame;
import org.vitrivr.cineast.core.data.frames.VideoFrame;
import org.vitrivr.cineast.core.data.raw.CachedDataFactory;
import org.vitrivr.cineast.core.extraction.decode.video.FFMpegVideoDecoder;
import org.vitrivr.cineast.core.extraction.decode.video.FFMpegVideoEncoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class Main {

    private static ImagePipeline imagePipeline;
    private static AudioPipeline audioPipeline;
    private static SaliencyMask saliencyMask;

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {

        if (args.length < 2){
            System.out.println("Expected parameters: <input video file>, <output video file>");
            System.exit(-1);
        }

        File inputFile = new File(args[0]);

        if (!inputFile.exists() || !inputFile.canRead()){
            System.out.println("Cannot access specified input file: " + inputFile.getAbsolutePath());
            System.exit(-1);
        }

        File outputFile = new File(args[1]);


        Config config = Config.getConfig();

        imagePipeline = new ImagePipeline(config);
        audioPipeline = new AudioPipeline(config);
        saliencyMask = new SaliencyMask();


        FFMpegVideoDecoder decoder = new FFMpegVideoDecoder();
        decoder.init(inputFile.toPath(), new DecoderConfig(), new CacheConfig());

        LinkedList<VideoFrame> frames = new LinkedList<>();

        final int totalFrameCount = decoder.count();
        int currentFrameCount = 0;

        VideoFrame frame = decoder.getNext();

        long startTime = System.currentTimeMillis();

        if (frame == null){
            return;
        }
        frames.add(frame);

        FFMpegVideoEncoder encoder = new FFMpegVideoEncoder(frame.getImage().getWidth(), frame.getImage().getHeight(), frame.getDescriptor().getFps(), 44100, outputFile.getAbsolutePath(), true);

        while ((frame = decoder.getNext()) != null){
            frames.add(frame);
            ++currentFrameCount;
            if (frames.size() >= 21){
                processBatch(frames, encoder);
                LOGGER.info(getTimeEstimate(totalFrameCount, currentFrameCount, startTime));
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

        LOGGER.info("Processed batch in {} seconds", (System.currentTimeMillis() - startTime) / 1000);

    }

    private static String getTimeEstimate(int totalFrameCount, int currentFrame, long startMillis){
        totalFrameCount = Math.max(totalFrameCount, 1);

        float progress = ((float) currentFrame) / totalFrameCount;
        int elapsed = (int) (System.currentTimeMillis() - startMillis) / 1000;

        int remaining = (int) (elapsed / progress);

        return String.format("Processed " + currentFrame + " of " + totalFrameCount + " frames, (%,.2f%%) remaining time: %02d:%02d:%02d", progress * 100f, remaining / 3600, (remaining % 3600) / 60, remaining % 60);
    }

}
