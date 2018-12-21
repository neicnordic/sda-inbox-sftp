package se.nbis.lega.inbox.streams;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class WaitingInputStream extends FilterInputStream {

    private volatile boolean done;

    /**
     * {@inheritDoc}
     */
    public WaitingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        log.info("Done: {}, read: {}", done, read);
        if (!done && read == -1) {
            safeSleep(1000);
            return read();
        }
        return read;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        int read = super.read(b);
        log.info("Done: {}, read: {}", done, read);
        if (!done && read == -1) {
            safeSleep(1000);
            return read(b);
        }
        return read;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        log.info("Done: {}, read: {}", done, read);
        if (!done && read == -1) {
            safeSleep(1000);
            return read(b, off, len);
        }
        return read;
    }

    private void safeSleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void setDone(boolean done) {
        this.done = done;
    }

}
