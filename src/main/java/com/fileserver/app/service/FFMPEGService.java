package com.fileserver.app.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

    // 4 seconds after 3 seconds from front, 3 seconds from middle, 3 seconds from
    // 40 seconds before last

    // 5sec: format, 20: format, 30:format
    public String createPreview(@NonNull String filename) {
        FFmpegProbeResult probeResult;
        try {
            probeResult = ffprobe.probe(uploadDir + "/" + filename);
        } catch (IOException e) {
            throw new FFMPEGException(filename + " cannot format");
        }
        FFmpegFormat format = probeResult.getFormat();
        double duration = format.duration;
        List<String> clips = new ArrayList<>();
        double[] dd = generateClipTimes(duration);
        for (double d : dd) {
            String name = RandomStringUtils.randomAlphanumeric(10); // name of clip
            generateClip(filename, d, name);
            clips.add(uploadDir + "/" + name);
        }

        String preview = filename.replace(".mp4", "") + "_preview.mp4";
        combineClips(clips, preview);
        // combineClipsDemuxer(clips, preview);
        return preview;
    }

    // 10 min clip
    private double[] generateClipTimes(double duration) {
        double[] dd = new double[8];
        if (duration < 10d) {
            return new double[0];
        }

        double step = ((duration - 10) / 10);

        double one = step;
        double two = step + one;
        double three = step + two;
        double four = step + three;
        double five = step + four;
        double six = step + five;
        double seven = step + six;
        double eight = step + seven;
        double nine = step + eight;

        dd[0] = two;
        dd[1] = three;
        dd[2] = four;
        dd[3] = five;
        dd[4] = six;
        dd[5] = seven;
        dd[6] = eight;
        dd[7] = nine;
        return dd;
    }

    private void generateClip(String filename, double d, String name) {
        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);
        FFmpegBuilder builder = new FFmpegBuilder().setStartOffset((long) d, TimeUnit.SECONDS) // offset -ss 00:03
                .addInput(uploadDir + "/" + filename).addOutput(uploadDir + "/" + name + ".mp4")// Filename, or a
                .disableAudio()
                .disableSubtitle()
                .addExtraArgs("-t", "1", "-c", "copy").done();
        executor.createJob(builder).run();
    }

    private void combineClips(List<String> names, String name) {
        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);
        FFmpegBuilder builder = new FFmpegBuilder();
        for (String file : names) {
            builder.addInput(file + ".mp4");
        }
        builder.addOutput(uploadDir + "/" + name).addExtraArgs("-filter_complex", "concat=n=8:v=1:a=0", "-y").done();
        executor.createJob(builder).run();
        for (String file : names) {
            try {
                Files.deleteIfExists(Paths.get(file + ".mp4"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void combineClipsDemuxer(List<String> names, String name) {
        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);
        FFmpegBuilder builder = new FFmpegBuilder();
        String concatTxt = uploadDir + "/" + "concat.txt";
        try {
            Files.deleteIfExists(Paths.get(concatTxt));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileWriter fw = new FileWriter(concatTxt, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            for (String file : names) {
                out.println("file '" + file + ".mp4'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        builder.addExtraArgs("-f", "concat", "-safe", "0").setInput(concatTxt).addOutput(uploadDir + "/" + name)
                .addExtraArgs("-c", "copy").done();
        executor.createJob(builder).run();
        for (String file : names) {
            try {
                Files.deleteIfExists(Paths.get(file + ".mp4"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void generateImage(String filename, String name, double d) {
        FFmpegExecutor executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);

        FFmpegBuilder builder = new FFmpegBuilder().setInput(uploadDir + "/" + filename)
                .addOutput(uploadDir + "/" + name + ".png").setStartOffset((long) d, TimeUnit.SECONDS) // offset -ss
                                                                                                       // 00:03
                .addExtraArgs("-vframes", "1").done();
        executor.createJob(builder).run();
    }

}
