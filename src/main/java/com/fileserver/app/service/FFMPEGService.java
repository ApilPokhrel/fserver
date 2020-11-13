package com.fileserver.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fileserver.app.exception.FFMPEGException;
import com.mongodb.lang.NonNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

@Service
public class FFMPEGService {

    @Value("${app.upload.dir}")
    public String uploadDir;

    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    public FFMPEGService() throws IOException {
        ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
        ffprobe = new FFprobe("/usr/bin/ffprobe");
    }

    // 4 seconds after 3 seconds from front, 3 seconds from middle, 3 seconds from 40 seconds before last

    //5sec: format, 20: format, 30:format
    public String createPreview(@NonNull String filename) {
        FFmpegProbeResult probeResult;
        try {
            probeResult = ffprobe.probe(uploadDir + "/" + filename);
        } catch (IOException e) {
            throw new FFMPEGException(filename+ " cannot format");
        }
        FFmpegFormat format = probeResult.getFormat();
        double duration = format.duration;
        List<String> clips = new ArrayList<>();
        double[] dd = generateClipTimes(duration);
        for(double d : dd){
            String name = RandomStringUtils.randomAlphanumeric(10);  //name of clip
            generateClip(filename, d, name);
            clips.add(uploadDir+"/"+name);
        }

        String preview = filename.replace(".mp4", "")+"_preview.mp4";
        combineClips(clips, preview);
        return preview;
    }


    //10 min clip
    private double[] generateClipTimes(double duration){
        double[] dd = new double[10];
        if(duration < 10d) {
            return new double[0];
        }
        dd[0] = 4d;
        double one = duration / 9d;
        double two = duration / 8d;
        double three = duration / 7d;
        double four = duration / 6d;
        double five = duration / 5d;
        double six = duration / 4d;
        double seven = duration / 3d;
        double eight = duration / 2d;
        dd[1] = one;
        dd[2] = two;
        dd[3] = three;
        dd[4] = four;
        dd[5] = five;
        dd[6] = six;
        dd[7] = seven;
        dd[8] = eight;
        if((duration - 40d) > 4) dd[9] = duration - 40d;
        return dd;
    }

    private void generateClip(String filename, double d, String name){
        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);

        FFmpegBuilder builder = new FFmpegBuilder()
        .setStartOffset((long) d, TimeUnit.SECONDS) //offset -ss 00:03
            .setInput(uploadDir + "/" + filename)
            .addOutput(uploadDir + "/" + name + ".mp4")// Filename, or a FFmpegProbeResult
            .disableAudio()
            .addExtraArgs("-t", "1", "-c:v", "copy")
            .done();
            executor.createJob(builder).run();
    }

    private void combineClips(List<String> names, String name){
        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);
        FFmpegBuilder builder = new FFmpegBuilder();
        for(String file : names){
            builder.addInput(file+".mp4");
        }
        builder
            .addOutput(uploadDir+"/"+name)
            .addExtraArgs("-filter_complex", "concat=n=10:v=1:a=0", "-y")
            .done();
            executor.createJob(builder).run();
            for(String file : names){
                try {
                    Files.deleteIfExists(Paths.get(file + ".mp4"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    public void generateImage(String filename, String name, double d){
        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);

        FFmpegBuilder builder = new FFmpegBuilder()
            .setInput(uploadDir+"/"+filename)
            .addOutput(uploadDir+"/"+name+".png")
            .setStartOffset((long) d, TimeUnit.SECONDS) //offset -ss 00:03
            .addExtraArgs("-vframes", "1")
            .done();
            executor.createJob(builder).run();
    }

}
