package io.github.seerainer.csv;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BOMDetector {

	public static class BOMInfo {
		private final Charset charset;
		private final int bomLength;

		public BOMInfo(final Charset charset, final int bomLength) {
			this.charset = charset;
			this.bomLength = bomLength;
		}

		public int getBomLength() {
			return bomLength;
		}

		public Charset getCharset() {
			return charset;
		}
	}

	public static BOMInfo detectBOM(final byte[] data) {
		if (data.length < 2) {
			return new BOMInfo(StandardCharsets.UTF_8, 0);
		}

		// UTF-8 BOM: EF BB BF
		if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
			return new BOMInfo(StandardCharsets.UTF_8, 3);
		}

		// UTF-16 BE BOM: FE FF
		if ((data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
			return new BOMInfo(StandardCharsets.UTF_16BE, 2);
		}

		// UTF-16 LE BOM: FF FE
		if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
			// Check if it's UTF-32 LE (FF FE 00 00)
			if (data.length >= 4 && data[2] == 0 && data[3] == 0) {
				return new BOMInfo(StandardCharsets.UTF_32LE, 4);
			}
			return new BOMInfo(StandardCharsets.UTF_16LE, 2);
		}

		// UTF-32 BE BOM: 00 00 FE FF
		if (data.length >= 4 && data[0] == 0 && data[1] == 0 && (data[2] & 0xFF) == 0xFE && (data[3] & 0xFF) == 0xFF) {
			return new BOMInfo(StandardCharsets.UTF_32BE, 4);
		}

		// No BOM detected
		return new BOMInfo(StandardCharsets.UTF_8, 0);
	}
}
