package ch.lucaro.videosaliency;

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.PixelMath;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import ch.lucaro.videosaliency.util.BufferedImageUtil;
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

        Planar<GrayF32> output = null, salient = null, background = null, blurred = null;

        float blurRadius = 3f;
        int dilateRadius = 4;

        for (int i = 1; i <= 250; ++i){

            /* prepare small mask */

            File maskinFile = new File(maskInputDir, i + ".png");

            BufferedImage bimg = ImageIO.read(maskinFile);

            float[][] fimg = FloatArrayImageUtil.fromBufferedImage(bimg);
            FloatArrayImageUtil.forEachInplace(fimg, x -> {
                double d = Math.pow(x, 0.8);
                if (d < 0.5){
                    return 0;
                }
                return d;
            });

            fimg = FloatArrayImageUtil.dilate(fimg, dilateRadius);

            GrayF32 smallMask = GBlurImageOps.gaussian(FloatArrayImageUtil.toImageGray(fimg), null,-1, 5,null);


            /* load frame */

            File inFile = new File(imageInputDir, i + ".png");
            bimg = ConvertBufferedImage.stripAlphaChannel(ImageIO.read(inFile));

            Planar<GrayF32> inputF = ConvertBufferedImage.convertFrom(bimg, true, ImageType.pl(3, GrayF32.class));


            /*  prepare background */

            BufferedImage desaturated = BufferedImageUtil.adjustSatturation(bimg, 0.1f);

            Planar<GrayF32> inputDesaturated = ConvertBufferedImage.convertFrom(desaturated, true, ImageType.pl(3, GrayF32.class));

            if (blurred == null){
                blurred = inputF.createSameShape();
            }

            GBlurImageOps.gaussian(inputDesaturated, blurred,-1,(int)(Math.max(inputF.width, inputF.height) / 100f * blurRadius),null);


            /* mask foreground */

            if (salient == null){
                salient = inputF.createSameShape();
            }

            GrayF32 mask = new GrayF32(inputF.width, inputF.height);

            FDistort scaler = new FDistort(smallMask, mask);
            scaler.interp(InterpolationType.BICUBIC).scale();
            scaler.apply();

            GPixelMath.multiply(inputF, mask, salient);


            /* mask background */

            if (background == null){
                background = inputF.createSameShape();
            }

            GrayF32 invertedMask = mask.createSameShape();

            PixelMath.minus(1f, mask, invertedMask);

            GPixelMath.multiply(blurred, invertedMask, background);


            /* combine */

            if (output == null){
                output = inputF.createSameShape();
            }

            GPixelMath.add(background, salient, output);


            bimg = ConvertBufferedImage.convertTo(output, null, true);

            File outFile = new File(outputDir, i + ".png");
            ImageIO.write(bimg, "PNG", outFile);


        }

    }

}
