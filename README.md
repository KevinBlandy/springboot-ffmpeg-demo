# springboot-ffmpeg-demo
SpringBoot + FFmpeg 实现视频转码为M3U8，可以点播。**需要先在本机安装ffmpeg，并且添加到环境变量**

## 使用
下载工程后，只需要修改 `application.yaml`中的一处配置，然后启动就行

```yaml
app:
  # 存储转码视频的文件夹
  video-folder: C:\Users\KevinBlandy\Desktop\tmp\video
```

## 有什么问题，建议可以在此贴进行讨论
[https://springboot.io/t/topic/3669](https://springboot.io/t/topic/3669)
