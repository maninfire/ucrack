package com.virjar.baksmalisrc.dexlib2.writer.io;

import java.io.IOException;

public interface DeferredOutputStreamFactory {
    DeferredOutputStream makeDeferredOutputStream() throws IOException;
}
