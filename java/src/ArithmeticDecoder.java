/* 
 * Reference arithmetic coding
 * Copyright (c) Project Nayuki
 * 
 * https://www.nayuki.io/page/reference-arithmetic-coding
 * https://github.com/nayuki/Reference-arithmetic-coding
 */

import java.io.IOException;


public final class ArithmeticDecoder extends ArithmeticCoderBase {
	
	private BitInputStream input;
	
	// The current code being read, which is always in the range [low, high].
	private long code;
	
	
	
	// Creates an arithmetic coding decoder.
	public ArithmeticDecoder(BitInputStream in) throws IOException {
		super();
		if (in == null)
			throw new NullPointerException();
		input = in;
		code = 0;
		for (int i = 0; i < STATE_SIZE; i++)
			code = code << 1 | readCodeBit();
	}
	
	
	
	// Decodes and returns a symbol.
	public int read(FrequencyTable freqs) throws IOException {
		return read(new CheckedFrequencyTable(freqs));
	}
	
	
	// Decodes and returns a symbol.
	public int read(CheckedFrequencyTable freqs) throws IOException {
		// Translate from coding range scale to frequency table scale
		long total = freqs.getTotal();
		if (total > MAX_TOTAL)
			throw new IllegalArgumentException("Cannot decode symbol because total is too large");
		long range = high - low + 1;
		long offset = code - low;
		long value = ((offset + 1) * total - 1) / range;
		if (value * range / total > offset)
			throw new AssertionError();
		if (value < 0 || value >= total)
			throw new AssertionError();
		
		// A kind of binary search
		int start = 0;
		int end = freqs.getSymbolLimit();
		while (end - start > 1) {
			int middle = (start + end) >>> 1;
			if (freqs.getLow(middle) > value)
				end = middle;
			else
				start = middle;
		}
		if (start == end)
			throw new AssertionError();
		
		int symbol = start;
		if (freqs.getLow(symbol) * range / total > offset || freqs.getHigh(symbol) * range / total <= offset)
			throw new AssertionError();
		update(freqs, symbol);
		if (code < low || code > high)
			throw new AssertionError("Code out of range");
		return symbol;
	}
	
	
	protected void shift() throws IOException {
		code = ((code << 1) & MASK) | readCodeBit();
	}
	
	
	protected void underflow() throws IOException {
		code = (code & TOP_MASK) | ((code << 1) & (MASK >>> 1)) | readCodeBit();
	}
	
	
	private int readCodeBit() throws IOException {
		int temp = input.read();
		if (temp == -1)
			temp = 0;
		return temp;
	}
	
}
