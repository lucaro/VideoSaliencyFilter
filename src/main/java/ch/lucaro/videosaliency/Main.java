package ch.lucaro.videosaliency;


import ch.lucaro.videosaliency.util.FloatArrayImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Main {

    public static void main(String[] args) throws IOException {

        File maskInputDir = new File("mask");
        File imageInputDir = new File("frames");
        File outputDir = new File("tmp");

       ImagePipeline pipeline = new ImagePipeline(4, 3f, .1f);

        for (int i = 1; i <= 250; ++i){

            /* prepare small mask */

            File maskinFile = new File(maskInputDir, i + ".png");

            BufferedImage bimg = ImageIO.read(maskinFile);

            float[][] mask = FloatArrayImageUtil.fromBufferedImage(bimg);

            bimg = ImageIO.read(new File(imageInputDir, i + ".png"));

            BufferedImage out = pipeline.process(bimg, mask);


            File outFile = new File(outputDir, i + ".png");
            ImageIO.write(out, "PNG", outFile);


        }

    }

}
