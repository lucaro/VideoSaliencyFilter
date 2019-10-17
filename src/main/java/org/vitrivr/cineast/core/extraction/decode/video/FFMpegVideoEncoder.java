package org.vitrivr.cineast.core.extraction.decode.video;

import org.vitrivr.cineast.core.config.CacheConfig;
import org.vitrivr.cineast.core.config.DecoderConfig;
import org.vitrivr.cineast.core.data.frames.AudioDescriptor;
import org.vitrivr.cineast.core.data.frames.AudioFrame;
import org.vitrivr.cineast.core.data.frames.VideoFrame;
import org.vitrivr.cineast.core.data.raw.images.MultiImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

import static java.lang.StrictMath.sin;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_NONE;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

/**
 *
 * based on
 * https://github.com/FFmpeg/FFmpeg/blob/master/doc/examples/muxing.c
 *
 * */
public class FFMpegVideoEncoder {

    private AudioFrame tmpFrame = null;

    private static AVRational one = new AVRational();

    static {
        one.num(1);
        one.den(1);
    }

//    private static  float t, tincr, tincr2;
//    private static AudioFrame generateDummyAudioFrame(int samples){
//        short v;
//
//        int q = 0;
//
//        short[] buffer = new short[samples * 2];
//        for (int j = 0; j < samples; j++) {
//            v = (short)(sin(t) * 10000);
//            for (int i = 0; i < 2; i++){
//                buffer[q++] = v;
//            }
//            t     += tincr;
//            tincr += tincr2;
//        }
//
//        ByteBuffer byteBuf = ByteBuffer.allocate(2*buffer.length);
//        for (int i = 0; i < buffer.length; ++i) {
//            short s = buffer[i];
//            byteBuf.put((byte)((s) & 0xff));
//            byteBuf.put((byte)((s >> 8) & 0xff));
//        }
//
//        return new AudioFrame(0, 0, byteBuf.array(), new AudioDescriptor(44100, 2, samples / 44100));
//
//    }

    private VideoOutputStreamContainer video_st = null;
    private AudioOutputStreamContainer audio_st = null;
    private AVOutputFormat fmt;
    private AVFormatContext oc = new AVFormatContext();

    private Queue<MultiImage> imageQueue = new LinkedList<>();
    private Queue<AudioFrame> audioQueue = new LinkedList<>();

    private boolean useAudio = false;

    public FFMpegVideoEncoder(int width, int height, int frameRate, int sampleRate, String filename, boolean useAudio){

        this.useAudio = useAudio;

        AVDictionary opt = new AVDictionary();


        /* allocate the output media context */
        avformat_alloc_output_context2(oc, null, null, filename);
        if (oc.isNull()) {
            System.err.println("Could not deduce output format from file extension: using MPEG.");
            avformat_alloc_output_context2(oc, null, "mpeg", filename);
        }
        if (oc.isNull()){
            return;
        }

        fmt = oc.oformat();

        if (fmt.video_codec() != AV_CODEC_ID_NONE) {
            video_st  = new VideoOutputStreamContainer(width, height, 2_000_000, frameRate, oc, fmt.video_codec(), opt);
        }

        if (fmt.audio_codec() != AV_CODEC_ID_NONE && useAudio) {
            audio_st = new AudioOutputStreamContainer(oc, fmt.audio_codec(), sampleRate, 128_000, opt);
        }

        av_dump_format(oc, 0, filename, 1);

        int ret;

        /* open the output file, if needed */
        if ((fmt.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext pb = new AVIOContext(null);
            ret = avio_open(pb, filename, AVIO_FLAG_WRITE);
            oc.pb(pb);
            if (ret < 0) {
                System.err.println("Could not open " + filename + " : " + ret);
                return;
            }
        }

        /* Write the stream header, if any. */
        ret = avformat_write_header(oc, opt);
        if (ret < 0) {
            System.err.println("Error occurred when opening output file: " + ret);
        }

    }

    private void encode(){

        while (!this.imageQueue.isEmpty() || !this.audioQueue.isEmpty()) {
            boolean writtenPacket = false;
            /* select the stream to encode */
            if (!useAudio || (av_compare_ts(video_st.frameCounter, video_st.c.time_base(), audio_st.next_pts, audio_st.c.time_base()) <= 0)) {
                if (!this.imageQueue.isEmpty()){
                    video_st.addFrame(this.imageQueue.poll());
                    writtenPacket = true;
                }
            } else {
                if (!this.audioQueue.isEmpty()){
                    audio_st.addFrame(this.audioQueue.poll());
                    writtenPacket = true;
                }
            }

            if (!writtenPacket){
                break;
            }
        }
    }

    public void add(MultiImage img){
        this.imageQueue.add(img);
        encode();
    }

    public void add(AudioFrame frame){

        int samples = audio_st.tmp_frame.nb_samples();

        if (tmpFrame == null){
            tmpFrame = new AudioFrame(frame);
        } else {
            tmpFrame.append(frame);
        }
        while (tmpFrame.numberOfSamples() > samples){
            this.audioQueue.add(tmpFrame.split(samples));
        }

        encode();
    }

    public void add(VideoFrame frame){
        this.imageQueue.add(frame.getImage());
        if(frame.getAudio().isPresent()){
            this.add(frame.getAudio().get());
        }
        encode();
    }

    public void close(){

        encode();

        av_write_trailer(oc);

        /* Close each codec. */
        if (video_st != null){
            video_st.close();
        }
        if (audio_st != null){
            audio_st.close();
        }

        if ((fmt.flags() & AVFMT_NOFILE) == 0)
            /* Close the output file. */
            avio_closep(oc.pb());

        /* free the stream */
        avformat_free_context(oc);

    }

    //Test stuff
    public static void main(String[] args) throws IOException {
//
//        BufferedImage testImg = ImageIO.read(new File("img.jpg"));
//
//        MultiImage img = CachedDataFactory.DEFAULT_INSTANCE.newInMemoryMultiImage(testImg);
//
//        FFMpegVideoEncoder mux = new FFMpegVideoEncoder(img.getWidth(), img.getHeight(), 25,"out.mp4", true);
//
//
//        if(mux.useAudio) {
//            /* init signal generator */
//            t = 0;
//            tincr = (float) (2 * M_PI * 110.0 / mux.audio_st.c.sample_rate());
//            /* increment frequency by 110 Hz per second */
//            tincr2 = (float) (2 * M_PI * 110.0 / mux.audio_st.c.sample_rate() / mux.audio_st.c.sample_rate());
//        }
//
//        for (int i = 0; i < 250; ++i){
//
//
//            mux.add(img);
//
//            while(mux.useAudio && mux.audioQueue.isEmpty()){
//                mux.add(generateDummyAudioFrame(mux.audio_st.tmp_frame.nb_samples()));
//            }
//
//        }
//
//        mux.close();

        FFMpegVideoDecoder decoder = new FFMpegVideoDecoder();
        decoder.init(Path.of("bbb.mp4"), new DecoderConfig(), new CacheConfig());

        Queue<VideoFrame> tmpFrames = new LinkedList<>();
        AudioFrame firstAudioFrame = null;
        VideoFrame frame = null;
        while(firstAudioFrame == null){
            frame = decoder.getNext();
            tmpFrames.add(frame);

            if (frame.getAudio().isPresent()){
                firstAudioFrame = frame.getAudio().get();
            }
        }

        frame = tmpFrames.peek();

        FFMpegVideoEncoder mux = new FFMpegVideoEncoder(frame.getImage().getWidth(), frame.getImage().getHeight(), (int)frame.getDescriptor().getFps(), (int)firstAudioFrame.getSamplingrate() / 4,"out.mp4", true);

        while (!tmpFrames.isEmpty()){
            mux.add(tmpFrames.poll());
        }

        for (int i = 0; i < 1200; ++i){
            frame = decoder.getNext();
            mux.add(frame);
            System.out.println(i);
        }

        decoder.close();
        mux.close();

    }

}