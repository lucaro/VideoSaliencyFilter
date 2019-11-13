package ch.lucaro.videosaliency;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.framework.ConfigProto;
import org.tensorflow.framework.GPUOptions;
import org.vitrivr.cineast.core.color.ReadableRGBContainer;
import org.vitrivr.cineast.core.data.raw.images.MultiImage;
import org.vitrivr.cineast.core.util.LogHelper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SaliencyMask {

    private final Graph graph = new Graph();
    private Session session;

    private final Tensor<Float> RNNmask_inTensor;
    private final Tensor<Float> RNNmask_hTensor;

    private static final Logger LOGGER = LogManager.getLogger();

    public SaliencyMask(){
        graph.importGraphDef(load());
        ConfigProto config = ConfigProto.newBuilder()
                .setAllowSoftPlacement(true)
                .setGpuOptions(GPUOptions.newBuilder().setPerProcessGpuMemoryFraction(0.5))
                .build();
        this.session = new Session(graph, config.toByteArray());

        float[][][][][] RNNmask_in = new float[1][28][28][128][8];
        float[][][][][] RNNmask_h = new float[1][28][28][128][8];
        for (int i = 0; i < RNNmask_h[0].length; ++i){
            for (int j = 0; j < RNNmask_h[0][i].length; ++j){
                for (int k = 0; k < RNNmask_h[0][i][j].length; ++k){
                    for (int l = 0; l < RNNmask_h[0][i][j][k].length; ++l){
                        RNNmask_in[0][i][j][k][l] = 1f;
                        RNNmask_h[0][i][j][k][l] = 1f;
                    }
                }
            }
        }

        RNNmask_inTensor = Tensor.create(RNNmask_in, Float.class);
        RNNmask_hTensor = Tensor.create(RNNmask_h, Float.class);

    }


    public float[][][] process(List<MultiImage> inputImages){

        if (inputImages ==  null || inputImages.isEmpty()){
            return new float[0][0][0];
        }

        ArrayList<BufferedImage> images = new ArrayList<>(21);

        for (MultiImage img : inputImages){
            try {
                images.add(Thumbnails.of(img.getBufferedImage()).forceSize(448, 448).asBufferedImage());
            } catch (IOException e) {
                LOGGER.error("Could not scale input image: {}", LogHelper.getStackTrace(e));
            }
            if (images.size() >= 21){
                break;
            }
        }

        while (images.size() < 21){
            images.add(images.get(images.size() - 1));
        }

        final float[][][][][] input = new float[1][21][448][448][3];

        for (int i = 0; i < 21; ++i){
            BufferedImage img = images.get(i);
            for (int x = 0; x < 448; ++x){
                for (int y = 0; y < 448; ++y){
                    int color = img.getRGB(x, y);
                    input[0][i][y][x][0] = ((ReadableRGBContainer.getRed(color) / 255.0f) * 2 - 1f);
                    input[0][i][y][x][1] = ((ReadableRGBContainer.getGreen(color) / 255.0f) * 2 - 1f);
                    input[0][i][y][x][2] = ((ReadableRGBContainer.getBlue(color) / 255.0f) * 2 - 1f);
                }
            }
        }

        Tensor<Float> inputTensor = Tensor.create(input, Float.class);

        Tensor<Float> result = session.runner().feed("Placeholder", inputTensor).feed("Placeholder_1", RNNmask_inTensor).feed("Placeholder_2", RNNmask_hTensor)
                .fetch("inference/concat_78").run().get(0).expect(Float.class);

        inputTensor.close();

        int len = result.numElements();
        FloatBuffer buf = FloatBuffer.allocate(len);
        result.writeTo(buf);
        result.close();

        float[] resultArray = buf.array();

        int resultImages = Math.min(16, inputImages.size());

        float[][][] resultTensor = new float[resultImages][112][112];

        int idx = 0;
        for (int i = 0; i < resultImages; ++i){
            for (int y = 0; y < 112; ++y){
                for (int x = 0; x < 112; ++x){
                    resultTensor[i][y][x] = resultArray[idx++];
                }
            }
        }

        return resultTensor;

    }

    public void close(){
        RNNmask_inTensor.close();
        RNNmask_hTensor.close();
        session.close();
        graph.close();
    }



    private static byte[] load() {
        try {
            return Files.readAllBytes((Paths.get("omcnn2clstm.pb")));
        } catch (IOException e) {
            throw new RuntimeException(
                    "could not load graph: " + LogHelper.getStackTrace(e));
        }
    }

}
