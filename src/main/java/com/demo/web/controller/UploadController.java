package com.demo.web.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.demo.ffmpeg.FFmpegUtils;
import com.demo.ffmpeg.TranscodeConfig;

@RestController
@RequestMapping("/upload")
public class UploadController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UploadController.class);
	
	@Value("${app.video-folder}")
	private String videoFolder;

	private Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
	
	/**
	 * 上传视频进行切片处理，返回访问路径
	 * @param video
	 * @param transcodeConfig
	 * @return
	 * @throws IOException 
	 */
	@PostMapping
	public Object upload (@RequestPart(name = "file", required = true) MultipartFile video,
						@RequestPart(name = "config", required = true) TranscodeConfig transcodeConfig) throws IOException {
		
		LOGGER.info("文件信息：title={}, size={}", video.getOriginalFilename(), video.getSize());
		LOGGER.info("转码配置：{}", transcodeConfig);
		
		// 原始文件名称，也就是视频的标题
		String title = video.getOriginalFilename();
		
		// io到临时文件
		Path tempFile = tempDir.resolve(title);
		LOGGER.info("io到临时文件：{}", tempFile.toString());
		
		try {
			
			video.transferTo(tempFile);
			
			// 删除后缀
			title = title.substring(0, title.lastIndexOf("."));
			
			// 按照日期生成子目录
			String today = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
			
			// 尝试创建视频目录
			Path targetFolder = Files.createDirectories(Paths.get(videoFolder, today, title));
			
			LOGGER.info("创建文件夹目录：{}", targetFolder);
			Files.createDirectories(targetFolder);
			
			// 执行转码操作
			LOGGER.info("开始转码");
			try {
				FFmpegUtils.transcodeToM3u8(tempFile.toString(), targetFolder.toString(), transcodeConfig);
			} catch (Exception e) {
				LOGGER.error("转码异常：{}", e.getMessage());
				Map<String, Object> result = new HashMap<>();
				result.put("success", false);
				result.put("message", e.getMessage());
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
			}
			
			// 封装结果
			Map<String, Object> videoInfo = new HashMap<>();
			videoInfo.put("title", title);
			videoInfo.put("m3u8", String.join("/", "", today, title, "index.m3u8"));
			videoInfo.put("poster", String.join("/", "", today, title, "poster.jpg"));
			
			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("data", videoInfo);
			return result;
		} finally {
			// 始终删除临时文件
			Files.delete(tempFile);
		}
	}
}
