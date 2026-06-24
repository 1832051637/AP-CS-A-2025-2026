import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MusicPlayer {
    private static Thread audioThread;
    private static volatile boolean running;
    private static String currentMusicFilePath;

    public static void playGeneratedLoop() {
        stop();
        running = true;
        audioThread = new Thread(() -> {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            byte[] tone = generateTheme(format);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();
                System.out.println("MusicPlayer: generated music thread started.");
                while (running) {
                    line.write(tone, 0, tone.length);
                }
                line.drain();
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
            }
            System.out.println("MusicPlayer: generated music thread stopped.");
        }, "MusicPlayer-Thread");
        audioThread.setDaemon(false);
        audioThread.start();
    }

    public static void playBackgroundMusic() {
        // 默认使用生成的循环背景音乐；项目可改为加载文件
        playGeneratedLoop();
    }

    private static byte[] generateTheme(AudioFormat format) {
        int sampleRate = (int) format.getSampleRate();
        int length = sampleRate * 4; // 4 second loop buffer
        byte[] buffer = new byte[length * 2];
        // 8-bit 平台游戏风格主旋律 (欢快、跳跃感强)
        double[] melody = {
            523.25, 523.25, 587.33, 523.25, 659.25, 523.25, 783.99, 0,
            659.25, 587.33, 523.25, 392.00, 440.00, 493.88, 523.25, 0,
            523.25, 587.33, 659.25, 783.99, 659.25, 587.33, 523.25, 493.88,
            523.25, 392.00, 440.00, 493.88, 587.33, 659.25, 783.99, 1046.50
        };
        int noteDuration = sampleRate / 8; // 8分音符，更轻快
        int pos = 0;
        for (int note = 0; note < melody.length && pos < length; note++) {
            double freq = melody[note];
            int noteLen = Math.min(noteDuration, length - pos);
            for (int j = 0; j < noteLen; j++) {
                double t = (pos + j) / (double) sampleRate;
                double sample = 0;

                // 主旋律 - 8-bit 方波风格 ( richer harmonics )
                if (freq > 0) {
                    double phase = 2 * Math.PI * freq * t;
                    double lead = Math.sin(phase) * 0.45
                                + Math.sin(phase * 2) * 0.22
                                + Math.sin(phase * 3) * 0.12
                                + Math.sin(phase * 4) * 0.06;
                    double attack = Math.min(1.0, j / (noteLen * 0.06 + 1e-9));
                    double release = Math.max(0.0, 1.0 - (j - noteLen * 0.72) / (noteLen * 0.28 + 1e-9));
                    sample += lead * attack * release * 0.35;
                }

                //  walking bass 低音线
                int bassIndex = note % 16;
                double bassFreq;
                if (bassIndex < 8) {
                    bassFreq = (bassIndex % 2 == 0) ? 130.81 : 164.81; // C3 / E3
                } else {
                    bassFreq = (bassIndex % 2 == 0) ? 196.00 : 146.83; // G3 / D3
                }
                double bassPhase = 2 * Math.PI * bassFreq * t;
                double bass = Math.sin(bassPhase) * 0.6 + Math.sin(bassPhase * 2) * 0.3;
                double bassEnv = Math.min(1.0, j / (noteLen * 0.12))
                               * Math.max(0.0, 1.0 - (j - noteLen * 0.55) / (noteLen * 0.45));
                sample += bass * bassEnv * 0.22;

                // Kick 鼓点 (第1、3、5、7...拍)
                int globalSample = pos + j;
                int beatPos = globalSample % (sampleRate / 4);
                if (beatPos < sampleRate / 28) {
                    double kickFreq = 55.0 + (beatPos / (double)(sampleRate / 28)) * 30.0; // 音高下滑
                    double kickEnv = Math.max(0.0, 1.0 - beatPos / (double)(sampleRate / 28));
                    sample += Math.sin(2 * Math.PI * kickFreq * t) * kickEnv * 0.28;
                }

                // Snare/Clap (第3、7拍 - 反拍)
                int offBeatPos = (globalSample + sampleRate / 8) % (sampleRate / 4);
                if (offBeatPos < sampleRate / 48) {
                    double snareEnv = Math.max(0.0, 1.0 - offBeatPos / (double)(sampleRate / 48));
                    double snareNoise = Math.sin(2 * Math.PI * 600 * t) * 0.5
                                      + Math.sin(2 * Math.PI * 900 * t) * 0.35;
                    sample += snareNoise * snareEnv * 0.12;
                }

                // Hi-hat (每8分音符的弱拍)
                int hatPos = globalSample % (sampleRate / 8);
                if (hatPos < sampleRate / 96) {
                    double hatEnv = Math.max(0.0, 1.0 - hatPos / (double)(sampleRate / 96));
                    sample += Math.sin(2 * Math.PI * 1200 * t) * hatEnv * 0.04;
                }

                short s = (short) (sample * Short.MAX_VALUE * 0.55);
                buffer[2 * (pos + j)] = (byte) (s & 0xff);
                buffer[2 * (pos + j) + 1] = (byte) ((s >> 8) & 0xff);
            }
            pos += noteLen;
        }
        while (pos < length) {
            buffer[2 * pos] = 0;
            buffer[2 * pos + 1] = 0;
            pos++;
        }
        return buffer;
    }

    public static void playMusicFile(String filePath) {
        stop();
        if (filePath == null || filePath.isEmpty()) return;
        currentMusicFilePath = filePath;
        running = true;
        audioThread = new Thread(() -> {
            while (running) {
                try (AudioInputStream fileStream = AudioSystem.getAudioInputStream(new File(filePath))) {
                    AudioFormat baseFormat = fileStream.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * 2,
                            baseFormat.getSampleRate(),
                            false);
                    try (AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, fileStream);
                         SourceDataLine line = AudioSystem.getSourceDataLine(decodedFormat)) {
                        line.open(decodedFormat);
                        line.start();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while (running && (bytesRead = din.read(buffer, 0, buffer.length)) != -1) {
                            line.write(buffer, 0, bytesRead);
                        }
                        line.drain();
                    }
                } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }, "MusicPlayer-Thread");
        audioThread.setDaemon(false);
        audioThread.start();
    }

    public static void playCoinSound() {
        playShortTone(new double[]{880.0, 1046.50}, 180);
    }

    public static void playStarSound() {
        playShortTone(new double[]{1046.50, 1318.51, 1567.98}, 320);
    }

    private static void playShortTone(double[] freqs, int durationMs) {
        new Thread(() -> {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            int sampleRate = (int) format.getSampleRate();
            int length = (int) ((durationMs / 1000.0) * sampleRate);
            byte[] buffer = new byte[length * 2];
            for (int i = 0; i < length; i++) {
                double t = i / (double) sampleRate;
                double value = 0;
                for (double freq : freqs) {
                    double phase = 2 * Math.PI * freq * t;
                    value += Math.sin(phase) * 0.65
                        + Math.sin(phase * 2) * 0.22
                        + Math.sin(phase * 3) * 0.08;
                }
                value /= freqs.length;
                double attack = Math.min(1.0, i / (length * 0.18 + 1e-9));
                double sustain = 0.92;
                double release = 1.0 - Math.max(0.0, (i - length * sustain) / (length * 0.08 + 1e-9));
                double envelope = Math.min(attack, release);
                short sample = (short) (value * envelope * Short.MAX_VALUE * 0.18);
                buffer[2 * i] = (byte) (sample & 0xff);
                buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xff);
            }
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
            }
        }, "SFX-Thread").start();
    }

    public static void stop() {
        running = false;
        if (audioThread != null && audioThread.isAlive()) {
            try {
                audioThread.join(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        audioThread = null;
    }
}
