package com.tughi.aggregator.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper methods for {@link InputStream} operations.
 */
public class StreamHelper {

    /**
     * Reads the entire stream into a byte array.
     */
    public static byte[] read(InputStream input) throws IOException {
        try {
            byte[] buffer = new byte[4 << 10]; // 4KB
            int size;

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            while ((size = input.read(buffer)) > 0) {
                output.write(buffer, 0, size);
            }

            return output.toByteArray();
        } finally {
            input.close();
        }
    }

}
