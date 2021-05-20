package com.demo.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyGenerator;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;


public class FFmpegUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegUtils.class);
	
	
	// 跨平台换行符
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	/**
	 * 生成随机16个字节的AESKEY
	 * @return
	 */
	private static byte[] genAesKey ()  {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(128);
			return keyGenerator.generateKey().getEncoded();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	/**
	 * 在指定的目录下生成key_info, key文件，返回key_info文件
	 * @param folder
	 * @throws IOException 
	 */
	private static Path genKeyInfo(String folder) throws IOException {
		// AES 密钥
		byte[] aesKey = genAesKey();
		// AES 向量
		String iv = Hex.encodeHexString(genAesKey());
		
		// key 文件写入
		Path keyFile = Paths.get(folder, "key");
		Files.write(keyFile, aesKey, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		// key_info 文件写入
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("key").append(LINE_SEPARATOR);					// m3u8加载key文件网络路径
		stringBuilder.append(keyFile.toString()).append(LINE_SEPARATOR);	// FFmeg加载key_info文件路径
		stringBuilder.append(iv);											// ASE 向量
		
		Path keyInfo = Paths.get(folder, "key_info");
		
		Files.write(keyInfo, stringBuilder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		
		return keyInfo;
	}
	
	/**
	 * 指定的目录下生成 master index.m3u8 文件
	 * @param fileName			master m3u8文件地址
	 * @param indexPath			访问子index.m3u8的路径
	 * @param bandWidth			流码率
	 * @throws IOException
	 */
	private static void genIndex(String file, String indexPath, String bandWidth) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("#EXTM3U").append(LINE_SEPARATOR);
		stringBuilder.append("#EXT-X-STREAM-INF:BANDWIDTH=" + bandWidth).append(LINE_SEPARATOR);  // 码率
		stringBuilder.append(indexPath);
		Files.write(Paths.get(file), stringBuilder.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}
	
	/**
	 * 转码视频为m3u8
	 * @param source				源视频
	 * @param destFolder			目标文件夹
	 * @param config				配置信息
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void transcodeToM3u8(String source, String destFolder, TranscodeConfig config) throws IOException, InterruptedException {
		
		// 判断源视频是否存在
		if (!Files.exists(Paths.get(source))) {
			throw new IllegalArgumentException("文件不存在：" + source);
		}
		
		// 创建工作目录
		Path workDir = Paths.get(destFolder, "ts");
		Files.createDirectories(workDir);
		
		// 在工作目录生成KeyInfo文件
		Path keyInfo = genKeyInfo(workDir.toString());
		
		// 构建命令
		List<String> commands = new ArrayList<>();
		commands.add("ffmpeg");			
		commands.add("-i")						;commands.add(source);					// 源文件
		commands.add("-c:v")					;commands.add("libx264");				// 视频编码为H264
		commands.add("-c:a")					;commands.add("copy");					// 音频直接copy
		commands.add("-hls_key_info_file")		;commands.add(keyInfo.toString());		// 指定密钥文件路径
		commands.add("-hls_time")				;commands.add(config.getTsSeconds());	// ts切片大小
		commands.add("-hls_playlist_type")		;commands.add("vod");					// 点播模式
		commands.add("-hls_segment_filename")	;commands.add("%06d.ts");				// ts切片文件名称
		
		if (StringUtils.hasText(config.getCutStart())) {
			commands.add("-ss")					;commands.add(config.getCutStart());	// 开始时间
		}
		if (StringUtils.hasText(config.getCutEnd())) {
			commands.add("-to")					;commands.add(config.getCutEnd());		// 结束时间
		}
		commands.add("index.m3u8");														// 生成m3u8文件
		
		// 构建进程
		Process process = new ProcessBuilder()
			.command(commands)
			.directory(workDir.toFile())
			.start()
			;
		
		// 读取进程标准输出
		new Thread(() -> {
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					LOGGER.info(line);
				}
			} catch (IOException e) {
			}
		}).start();
		
		// 读取进程异常输出
		new Thread(() -> {
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					LOGGER.info(line);
				}
			} catch (IOException e) {
			}
		}).start();
		
		
		// 阻塞直到任务结束
		if (process.waitFor() != 0) {
			throw new RuntimeException("视频切片异常");
		}
		
		// 切出封面
		if (!screenShots(source, String.join(File.separator, destFolder, "poster.jpg"), config.getPoster())) {
			throw new RuntimeException("封面截取异常");
		}
		
		// 获取视频信息
		MediaInfo mediaInfo = getMediaInfo(source);
		if (mediaInfo == null) {
			throw new RuntimeException("获取媒体信息异常");
		}
		
		// 生成index.m3u8文件
		genIndex(String.join(File.separator, destFolder, "index.m3u8"), "ts/index.m3u8", mediaInfo.getFormat().getBitRate());
		
		// 删除keyInfo文件
		Files.delete(keyInfo);
	}
	
	/**
	 * 获取视频文件的媒体信息
	 * @param source
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static MediaInfo getMediaInfo(String source) throws IOException, InterruptedException {
		List<String> commands = new ArrayList<>();
		commands.add("ffprobe");	
		commands.add("-i")				;commands.add(source);
		commands.add("-show_format");
		commands.add("-show_streams");
		commands.add("-print_format")	;commands.add("json");
		
		Process process = new ProcessBuilder(commands)
				.start();
		 
		MediaInfo mediaInfo = null;
		
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			mediaInfo = new Gson().fromJson(bufferedReader, MediaInfo.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (process.waitFor() != 0) {
			return null;
		}
		
		return mediaInfo;
	}
	
	/**
	 * 截取视频的指定时间帧，生成图片文件
	 * @param source		源文件
	 * @param file			图片文件
	 * @param time			截图时间 HH:mm:ss.[SSS]		
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static boolean screenShots(String source, String file, String time) throws IOException, InterruptedException {
		
		List<String> commands = new ArrayList<>();
		commands.add("ffmpeg");	
		commands.add("-i")				;commands.add(source);
		commands.add("-ss")				;commands.add(time);
		commands.add("-y");
		commands.add("-q:v")			;commands.add("1");
		commands.add("-frames:v")		;commands.add("1");
		commands.add("-f");				;commands.add("image2");
		commands.add(file);
		
		Process process = new ProcessBuilder(commands)
					.start();
		
		// 读取进程标准输出
		new Thread(() -> {
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					LOGGER.info(line);
				}
			} catch (IOException e) {
			}
		}).start();
		
		// 读取进程异常输出
		new Thread(() -> {
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					LOGGER.error(line);
				}
			} catch (IOException e) {
			}
		}).start();
		
		return process.waitFor() == 0;
	}
}

