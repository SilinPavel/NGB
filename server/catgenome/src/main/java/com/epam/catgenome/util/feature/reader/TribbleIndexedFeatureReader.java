package com.epam.catgenome.util.feature.reader;
/*
 * The MIT License
 *
 * Copyright (c) 2013 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.IndexUtils;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.tribble.*;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.Block;
import htsjdk.tribble.readers.PositionalBufferedStream;
import htsjdk.tribble.util.ParsingUtils;
import org.testng.Assert;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Copied from HTSJDK library. Added: class TribbleIndexCache for saving cache values,
 * method retrieveIndexFromCache(final String indexFile) for caching index and
 * modified readHeader() method for caching header
 *
 * A reader for text feature files  (i.e. not tabix files).   This includes tribble-indexed and non-indexed files.  If
 * index both iterate() and query() methods are supported.
 * <p/>
 * Note: Non-indexed files can be gzipped, but not bgzipped.
 *
 * @author Jim Robinson
 * @since 2/11/12
 */
public class TribbleIndexedFeatureReader<T extends Feature, S> extends AbstractFeatureReader<T, S> {

    public static final int BUFFERED_STREAM_SIZE = 512000;
    public static final int POSITIONAL_BUFFERED_STREAM_SIZE = 1000;
    public static final int MAX_BUFFER_SIZE = 100000000;
    public static final int MIN_BUFFER_SIZE = 2000000;

    private Index index;
    /**
     * is the path pointing to our source data a regular file?
     */
    private final boolean pathIsRegularFile;

    /**
     * a potentially reusable seekable stream for queries over regular files
     */
    private SeekableStream seekableStream = null;

    /**
     * We lazy-load the index but it might not even exist
     * Don't want to keep checking if that's the case
     */
    private boolean needCheckForIndex = true;

    /**
     * @param featurePath  - path to the feature file, can be a local file path, http url, or ftp url
     * @param codec        - codec to decode the features
     * @param requireIndex - true if the reader will be queries for specific ranges.  An index (idx) file must exist
     * @param indexCache  - a cache for Index objects
     * @throws IOException
     */
    public TribbleIndexedFeatureReader(final String featurePath, final FeatureCodec<T, S> codec,
                                       final boolean requireIndex, final EhCacheBasedIndexCache indexCache)
            throws IOException {
        super(featurePath, codec);
        this.indexCache = indexCache;
        if (requireIndex) {
            this.loadIndex();
            if(!this.hasIndex()){
                throw new TribbleException("An index is required, but none found.");
            }
        }

        // does path point to a regular file?
        this.pathIsRegularFile = SeekableStreamFactory.isFilePath(path);

        readHeader();
    }

    /**
     * @param featureFile  - path to the feature file, can be a local file path, http url, or ftp url
     * @param indexFile    - path to the index file
     * @param codec        - codec to decode the features
     * @param requireIndex - true if the reader will be queries for specific ranges.  An index (idx) file must exist
     * @param indexCache  - a cache for Index objects
     * @throws IOException
     */
    public TribbleIndexedFeatureReader(final String featureFile, final String indexFile,
                                       final FeatureCodec<T, S> codec,
                                       final boolean requireIndex, final EhCacheBasedIndexCache indexCache)
            throws IOException {
        this(featureFile, codec, false, indexCache); // required to read the header
        if (indexFile != null && IOHelper.resourceExists(indexFile)) {
            index = retrieveIndex(indexFile);
            this.needCheckForIndex = false;
        } else {
            if (requireIndex) {
                this.loadIndex();
                if(!this.hasIndex()){
                    throw new TribbleException("An index is required, but none found.");
                }
            }
        }
    }

    private Index retrieveIndex(final String indexFile) {
        Index index;
        String indexFilePath = IndexUtils.getFirstPartForIndexPath(Tribble.indexFile(this.path));
        if (indexCache != null && indexCache.contains(indexFilePath)) {
            tribbleIndexCache = (TribbleIndexCache) indexCache.getFromCache(indexFilePath);
            if (tribbleIndexCache.index != null) {
                return tribbleIndexCache.index;
            } else {
                index = IndexUtils.loadIndex(indexFile);
                tribbleIndexCache.index = index;
                indexCache.putInCache(tribbleIndexCache, indexFilePath);
                return index;
            }
        } else {
            index = IndexUtils.loadIndex(indexFile);
            if (indexCache != null) {
                tribbleIndexCache = new TribbleIndexCache();
                tribbleIndexCache.index = index;
                indexCache.putInCache(tribbleIndexCache, indexFilePath);
            }
            return index;
        }
    }
    /**
     * @param featureFile - path to the feature file, can be a local file path, http url, or ftp url
     * @param codec       - codec to decode the features
     * @param index       - a tribble Index object
     * @param indexCache  - a cache for Index objects
     * @throws IOException
     */
    public TribbleIndexedFeatureReader(final String featureFile, final FeatureCodec<T, S> codec,
                                       final Index index, final EhCacheBasedIndexCache indexCache) throws IOException {
        this(featureFile, codec, false, indexCache); // required to read the header
        this.index = index;
        this.needCheckForIndex = false;
    }

    /**
     * Attempt to load the index for the specified {@link #path}.
     * If the {@link #path} has no available index file,
     * does nothing
     * @throws IOException
     */
    private void loadIndex() throws IOException{
        String indexFile = Tribble.indexFile(this.path);
        if (IOHelper.resourceExists(indexFile)) {
            index = retrieveIndex(indexFile);
        } else {
            // See if the index itself is gzipped
            indexFile = ParsingUtils.appendToPath(indexFile, ".gz");
            if (IOHelper.resourceExists(indexFile)) {
                index = retrieveIndex(indexFile);
            }
        }
        this.needCheckForIndex = false;
    }

    /**
     * Get a seekable stream appropriate to read information from the current feature path
     * <p/>
     * This function ensures that if reuseStreamInQuery returns true then this function will only
     * ever return a single unique instance of SeekableStream for all calls given this instance of
     * TribbleIndexedFeatureReader.  If reuseStreamInQuery() returns false then the returned SeekableStream
     * will be newly opened each time, and should be closed after each use.
     *
     * @return a SeekableStream
     */
    private SeekableStream getSeekableStream() throws IOException {
        final SeekableStream result;
        if (reuseStreamInQuery()) {
            // if the stream points to an underlying file, only create the underlying seekable stream once
            if (seekableStream == null) {
                seekableStream = SeekableStreamFactory.getInstance().getStreamFor(path);
            }
            result = seekableStream;
        } else {
            // we are not reusing the stream, so make a fresh copy each time we request it
            result = SeekableStreamFactory.getInstance().getStreamFor(path);
        }
        return result;
    }

    /**
     * Are we attempting to reuse the underlying stream in query() calls?
     *
     * @return true if
     */
    private boolean reuseStreamInQuery() {
        return pathIsRegularFile;
    }

    public void close() throws IOException {
        // close the seekable stream if that's necessary
        if (seekableStream != null) {
            seekableStream.close();
        }
    }

    /**
     * Return the sequence (chromosome/contig) names in this file, if known.
     *
     * @return list of strings of the contig names
     */
    public List<String> getSequenceNames() {
        return !this.hasIndex() ? new ArrayList<>() : new ArrayList<>(index.getSequenceNames());
    }

    @Override
    public boolean hasIndex() {
        if(index == null && this.needCheckForIndex){
            try {
                this.loadIndex();
            } catch (IOException e) {
                throw new TribbleException("Error loading index file: " + e.getMessage(), e);
            }
        }
        return index != null;
    }

    /**
     * read the header from the file
     *
     * @throws IOException throws an IOException if we can't open the file
     */
    private void readHeader() throws IOException {
        InputStream is = null;
        PositionalBufferedStream pbs = null;

        try {
            is = IOHelper.openStream(path);
            if (path.endsWith("gz")) {
                // TODO -- warning I don't think this can work, the buffered input stream screws up position
                is = new GZIPInputStream(new BufferedInputStream(is));
            }
            pbs = new PositionalBufferedStream(is);
            final S source;
            String indexFilePath = IndexUtils.getFirstPartForIndexPath(Tribble.indexFile(this.path));

            if (indexCache != null && indexCache.contains(indexFilePath)) {
                tribbleIndexCache = (TribbleIndexCache) indexCache.getFromCache(indexFilePath);
                header = tribbleIndexCache.header;
                if (header == null) {
                    source = codec.makeSourceFromStream(pbs);
                    header = codec.readHeader(source);
                    tribbleIndexCache.header = header;
                    tribbleIndexCache.codec = codec;
                    indexCache.putInCache(tribbleIndexCache, indexFilePath);
                }
                codec = tribbleIndexCache.codec;
            }  else {
                source = codec.makeSourceFromStream(pbs);
                header = codec.readHeader(source);
                if (indexCache != null) {
                    tribbleIndexCache = new TribbleIndexCache();
                    tribbleIndexCache.header = header;
                    tribbleIndexCache.codec = codec;
                    indexCache.putInCache(tribbleIndexCache, indexFilePath);
                }
            }
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile(
                    "Unable to parse header with error: " + e.getMessage(), path, e);
        } finally {
            if (pbs != null) {
                pbs.close();
            } else if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Return an iterator to iterate over features overlapping the specified interval
     * <p/>
     * Note that TribbleIndexedFeatureReader only supports issuing and manipulating a single query
     * for each reader.  That is, the behavior of the following code is undefined:
     * <p/>
     * reader = new TribbleIndexedFeatureReader()
     * Iterator it1 = reader.query("x", 10, 20)
     * Iterator it2 = reader.query("x", 1000, 1010)
     * <p/>
     * As a consequence of this, the TribbleIndexedFeatureReader are also not thread-safe.
     *
     * @param chr   contig
     * @param start start position
     * @param end   end position
     * @return an iterator of records in this interval
     * @throws IOException
     */
    public CloseableTribbleIterator<T> query(final String chr, final int start, final int end) throws IOException {
        if (!this.hasIndex()) {
            throw new TribbleException("Index not found for: " + path);
        }

        if (index.containsChromosome(chr)) {
            final List<Block> blocks = index.getBlocks(chr, start - 1, end);
            return new QueryIterator(chr, start, end, blocks);
        } else {
            return new EmptyIterator<T>();
        }
    }

    /**
     * @return Return an iterator to iterate over the entire file
     * @throws IOException
     */
    public CloseableTribbleIterator<T> iterator() throws IOException {
        return new WFIterator();
    }

    /**
     * Class to iterator over an entire file.
     */
    class WFIterator implements CloseableTribbleIterator<T> {
        private T currentRecord;
        private S source;

        /**
         * Constructor for iterating over the entire file (seekableStream).
         *
         * @throws IOException
         */
        WFIterator() throws IOException {
            final InputStream inputStream = IOHelper.openStream(path);
            final PositionalBufferedStream pbs;

            if (path.endsWith(".gz")) {
                // Gzipped -- we need to buffer the GZIPInputStream methods as this class makes read() calls,
                // and seekableStream does not support single byte reads
                final InputStream is = new GZIPInputStream(new BufferedInputStream(inputStream, BUFFERED_STREAM_SIZE));
                pbs = new PositionalBufferedStream(is, POSITIONAL_BUFFERED_STREAM_SIZE);
                // Small buffer as this is buffered already.
            } else {
                pbs = new PositionalBufferedStream(inputStream, BUFFERED_STREAM_SIZE);
            }
            /*
             * The header was already read from the original source in the constructor; don't read it again,
             * since some codecs keep state about its initializagtion.  Instead, skip that part of the stream.
             */
            long skippedBytes = pbs.skip(header.getHeaderEnd());
            if (skippedBytes == 0) {
                Assert.assertEquals(skippedBytes, 0);
            }
            source = codec.makeSourceFromStream(pbs);
            readNextRecord();
        }

        @Override
        public boolean hasNext() {
            return currentRecord != null;
        }

        @Override
        public T next() {
            final T ret = currentRecord;
            try {
                readNextRecord();
            } catch (IOException e) {
                throw new RuntimeIOException("Unable to read the next record, the last record was at " +
                        ret.getContig() + ":" + ret.getStart() + "-" + ret.getEnd(), e);
            }
            return ret;
        }

        /**
         * Advance to the next record in the query interval.
         *
         * @throws IOException
         */
        private void readNextRecord() throws IOException {
            currentRecord = null;

            while (!codec.isDone(source)) {
                final T f;
                try {
                    f = codec.decode(source);

                    if (f == null) {
                        continue;
                    }

                    currentRecord = f;
                    return;

                } catch (TribbleException e) {
                    e.setSource(path);
                    throw e;
                } catch (NumberFormatException e) {
                    final String error = "Error parsing line at byte position: " + source;
                    throw new TribbleException.MalformedFeatureFile(error, path, e);
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported in Iterators");
        }

        @Override
        public void close() {
            codec.close(source);
        }

        @Override
        public WFIterator iterator() {
            return this;
        }
    }

    /**
     * Iterator for a query interval
     */
    class QueryIterator implements CloseableTribbleIterator<T> {
        private String chrAlias;
        int start;
        int end;
        private T currentRecord;
        private S source;
        private SeekableStream mySeekableStream;
        private Iterator<Block> blockIterator;

        QueryIterator(final String chr, final int start, final int end, final List<Block> blocks)
                throws IOException {
            this.start = start;
            this.end = end;
            mySeekableStream = getSeekableStream();
            blockIterator = blocks.iterator();
            advanceBlock();
            readNextRecord();

            // The feature chromosome might not be the query chromosome, due to alias definitions.  We assume
            // the chromosome of the first record is correct and record it here.  This is not pretty.
            chrAlias = (currentRecord == null ? chr : currentRecord.getContig());
        }

        public boolean hasNext() {
            return currentRecord != null;
        }

        public T next() {
            final T ret = currentRecord;
            try {
                readNextRecord();
            } catch (IOException e) {
                throw new RuntimeIOException("Unable to read the next record, the last record was at " +
                        ret.getContig() + ":" + ret.getStart() + "-" + ret.getEnd(), e);
            }
            return ret;
        }

        private void advanceBlock() throws IOException {
            while (blockIterator != null && blockIterator.hasNext()) {
                final Block block = blockIterator.next();
                if (block.getSize() > 0) {
                    final int bufferSize =
                            Math.min(MIN_BUFFER_SIZE, block.getSize() > MAX_BUFFER_SIZE
                                    ? (MAX_BUFFER_SIZE /10)
                                    : (int) block.getSize());
                    source = codec.makeSourceFromStream(new PositionalBufferedStream(
                            new BlockStreamWrapper(mySeekableStream, block), bufferSize));
                    // note we don't have to skip the header here as the block should never start in the header
                    return;
                }
            }

            // If we get here the blocks are exhausted, set reader to null
            if (source != null) {
                codec.close(source);
                source = null;
            }
        }

        /**
         * Advance to the next record in the query interval.
         *
         * @throws IOException
         */
        private void readNextRecord() throws IOException {
            if (source == null) {
                return;  // <= no more features to read
            }
            currentRecord = null;

            while (true) {   // Loop through blocks
                while (!codec.isDone(source)) {  // Loop through current block
                    final T f;
                    try {
                        f = codec.decode(source);
                        if (f == null) {
                            continue;   // Skip
                        }
                        if ((chrAlias != null && !f.getContig().equals(chrAlias)) || f.getStart() > end) {
                            if (blockIterator.hasNext()) {
                                advanceBlock();
                                continue;
                            } else {
                                return;    // Done
                            }
                        }
                        if (f.getEnd() < start) {
                            continue;   // Skip
                        }

                        currentRecord = f;     // Success
                        return;
                    } catch (TribbleException e) {
                        e.setSource(path);
                        throw e;
                    } catch (NumberFormatException e) {
                        final String error = "Error parsing line: " + source;
                        throw new TribbleException.MalformedFeatureFile(error, path, e);
                    }
                }
                if (blockIterator != null && blockIterator.hasNext()) {
                    advanceBlock();   // Advance to next block
                } else {
                    return;   // No blocks left, we're done.
                }
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported.");
        }

        public void close() {
            // Note that this depends on BlockStreamWrapper not actually closing the underlying stream
            codec.close(source);
            if (!reuseStreamInQuery()) {
                // if we are going to reuse the underlying stream we don't close the underlying stream.
                try {
                    mySeekableStream.close();
                } catch (IOException e) {
                    throw new TribbleException("Couldn't close seekable stream", e);
                }
            }
        }

        public Iterator<T> iterator() {
            return this;
        }
    }

    /**
     * Wrapper around a SeekableStream that limits reading to the specified "block" of bytes.  Attempts to
     * read beyond the end of the block should return -1  (EOF).
     */
    static class BlockStreamWrapper extends InputStream {

        SeekableStream seekableStream;
        long maxPosition;

        BlockStreamWrapper(final SeekableStream seekableStream, final Block block) throws IOException {
            this.seekableStream = seekableStream;
            seekableStream.seek(block.getStartPosition());
            maxPosition = block.getEndPosition();
        }

        @Override
        public int read() throws IOException {
            return (seekableStream.position() > maxPosition) ? -1 : seekableStream.read();
        }

        @Override
        public int read(final byte[] bytes, final int off, final int len) throws IOException {
            // note the careful treatment here to ensure we can continue to
            // read very long > Integer sized blocks
            final long maxBytes = maxPosition - seekableStream.position();
            if (maxBytes <= 0) {
                return -1;
            }

            final int bytesToRead = (int) Math.min(len, Math.min(maxBytes, Integer.MAX_VALUE));
            return seekableStream.read(bytes, off, bytesToRead);
        }
    }

    protected static class TribbleIndexCache <T extends Feature, S> implements IndexCache {
        private Index index;
        private FeatureCodecHeader header;
        private FeatureCodec <T, S> codec;

        public Index getIndex() {
            return index;
        }

        public void setIndex(Index index) {
            this.index = index;
        }

        public FeatureCodecHeader getHeader() {
            return header;
        }

        public void setHeader(FeatureCodecHeader header) {
            this.header = header;
        }

        public FeatureCodec<T, S> getCodec() {
            return codec;
        }

        public void setCodec(FeatureCodec<T, S> codec) {
            this.codec = codec;
        }

    }

    protected TribbleIndexCache tribbleIndexCache;
}
