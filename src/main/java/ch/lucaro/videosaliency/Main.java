package ch.lucaro.videosaliency;


import ch.lucaro.videosaliency.util.FloatArrayImageUtil;
import org.vitrivr.cineast.core.config.CacheConfig;
import org.vitrivr.cineast.core.config.DecoderConfig;
import org.vitrivr.cineast.core.data.frames.VideoFrame;
import org.vitrivr.cineast.core.data.raw.images.MultiImage;
import org.vitrivr.cineast.core.extraction.decode.video.FFMpegVideoDecoder;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;


public class Main {

    private static ImagePipeline pipeline = new ImagePipeline(5, 3f, .1f);
    private static SaliencyMask saliencyMask;
    private static File outputDir = new File("tmp");

    private static int frameCounter = 0;

    public static void main(String[] args) throws IOException {

        System.setProperty("TF_CPP_MIN_LOG_LEVEL", "3");

        saliencyMask = new SaliencyMask();


        FFMpegVideoDecoder decoder = new FFMpegVideoDecoder();
        decoder.init(Path.of("query.mp4"), new DecoderConfig(), new CacheConfig());

        LinkedList<MultiImage> images = new LinkedList<>();

        VideoFrame frame;

        while ((frame = decoder.getNext()) != null){
            images.add(frame.getImage());
            if (images.size() >= 21){
                processBatch(images);
                while (images.size() > 5){
                    images.poll();
                }
            }
        }
        processBatch(images);

        saliencyMask.close();

    }

    private static void processBatch(List<MultiImage> images){

        long startTime = System.currentTimeMillis();

        float[][][] masks = saliencyMask.process(images);

        for (int i = 0; i < masks.length; ++i){
            BufferedImage out = pipeline.process(images.get(i).getBufferedImage(), masks[i]);
            try {
                ImageIO.write(out, "PNG", new File(outputDir, (frameCounter++) + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("---> Processed batch in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");

    }

}
