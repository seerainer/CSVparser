package io.github.seerainer.csv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Iterator for streaming CSV records from a Reader. This allows
 * memory-efficient processing of large CSV files.
 */
public class CSVRecordIterator implements Iterator<CSVRecord>, Closeable {
    private final BufferedReader reader;
    private final CSVParser parser;
    private final StringBuilder contentBuffer;
    private final Queue<CSVRecord> recordQueue;
    private CSVRecord nextRecord;
    private int currentLineNumber;
    private boolean closed;
    private boolean reachedEOF;

    CSVRecordIterator(final Reader reader, final CSVParser parser) {
	this.reader = reader instanceof final BufferedReader b ? b : new BufferedReader(reader);
	this.parser = parser;
	this.contentBuffer = new StringBuilder();
	this.recordQueue = new ArrayDeque<>();
	this.currentLineNumber = 1;
	this.closed = false;
	this.reachedEOF = false;
	advanceToNext();
    }

    private void advanceToNext() {
	if (closed) {
	    nextRecord = null;
	    return;
	}

	// If queue already has records, pull next
	if (!recordQueue.isEmpty()) {
	    nextRecord = recordQueue.poll();
	    return;
	}

	while (true) {
	    // Read more content and parse records
	    try {
		if (reachedEOF && contentBuffer.length() == 0) {
		    nextRecord = null;
		    return;
		}

		// Read a chunk of lines
		final var chunkSize = 100; // Read up to 100 lines at a time
		var linesRead = 0;
		String line;

		while (linesRead < chunkSize && (line = reader.readLine()) != null) {
		    if (contentBuffer.length() > 0) {
			contentBuffer.append('\n');
		    }
		    contentBuffer.append(line);
		    linesRead++;
		}

		if (linesRead == 0) {
		    reachedEOF = true;
		}

		// Try to parse what we have
		if (contentBuffer.length() > 0) {
		    final var content = contentBuffer.toString();
		    try {
			final var records = parser.parseString(content);

			if (!records.isEmpty()) {
			    // Add all records to the queue
			    recordQueue.addAll(records);
			    // Clear the content buffer since we successfully parsed it
			    contentBuffer.setLength(0);
			    // Get the next record from the queue
			    nextRecord = recordQueue.poll();
			    return;
			}
		    } catch (final CSVParseException e) {
			// If we haven't reached EOF, try reading more content by looping again
			if (!reachedEOF) {
			    // continue reading more into the buffer
			    continue;
			}
		    }
		}

		// If buffer empty and EOF, then no next
		nextRecord = null;
		return;

	    } catch (final IOException e) {
		throw new RuntimeException("Error reading CSV data", e);
	    }
	}
    }

    @Override
    public void close() throws IOException {
	if (closed) {
	    return;
	}
	closed = true;
	reader.close();
    }

    public int getCurrentLineNumber() {
	return currentLineNumber;
    }

    @Override
    public boolean hasNext() {
	return nextRecord != null;
    }

    public boolean isClosed() {
	return closed;
    }

    @Override
    public CSVRecord next() {
	if (!hasNext()) {
	    throw new NoSuchElementException("No more CSV records available");
	}

	final var current = nextRecord;
	// Advance the line number for the next record
	currentLineNumber++;
	advanceToNext();
	return current;
    }
}