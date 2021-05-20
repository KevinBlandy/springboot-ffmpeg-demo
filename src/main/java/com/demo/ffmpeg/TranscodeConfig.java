package com.demo.ffmpeg;

public class TranscodeConfig {
	private String poster;				// 截取封面的时间			HH:mm:ss.[SSS]
	private String tsSeconds;			// ts分片大小，单位是秒
	private String cutStart;			// 视频裁剪，开始时间		HH:mm:ss.[SSS]
	private String cutEnd;				// 视频裁剪，结束时间		HH:mm:ss.[SSS]
	public String getPoster() {
		return poster;
	}

	public void setPoster(String poster) {
		this.poster = poster;
	}

	public String getTsSeconds() {
		return tsSeconds;
	}

	public void setTsSeconds(String tsSeconds) {
		this.tsSeconds = tsSeconds;
	}

	public String getCutStart() {
		return cutStart;
	}

	public void setCutStart(String cutStart) {
		this.cutStart = cutStart;
	}

	public String getCutEnd() {
		return cutEnd;
	}

	public void setCutEnd(String cutEnd) {
		this.cutEnd = cutEnd;
	}

	@Override
	public String toString() {
		return "TranscodeConfig [poster=" + poster + ", tsSeconds=" + tsSeconds + ", cutStart=" + cutStart + ", cutEnd="
				+ cutEnd + "]";
	}
}
