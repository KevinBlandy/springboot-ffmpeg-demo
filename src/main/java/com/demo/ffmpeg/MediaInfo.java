package com.demo.ffmpeg;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MediaInfo {
	public static class Format {
		@SerializedName("bit_rate")
		private String bitRate;
		public String getBitRate() {
			return bitRate;
		}
		public void setBitRate(String bitRate) {
			this.bitRate = bitRate;
		}
	}

	public static class Stream {
		@SerializedName("index")
		private int index;

		@SerializedName("codec_name")
		private String codecName;

		@SerializedName("codec_long_name")
		private String codecLongame;

		@SerializedName("profile")
		private String profile;
	}
	
	// ----------------------------------

	@SerializedName("streams")
	private List<Stream> streams;

	@SerializedName("format")
	private Format format;

	public List<Stream> getStreams() {
		return streams;
	}

	public void setStreams(List<Stream> streams) {
		this.streams = streams;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}
}
