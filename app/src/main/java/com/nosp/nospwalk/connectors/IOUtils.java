package com.nosp.nospwalk.connectors;

import java.io.*;
import java.nio.charset.Charset;

public class IOUtils {
    public static void write(String data, OutputStream out, Charset charset) {
        try (Writer writer = new OutputStreamWriter(out, charset)) {
            writer.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toString(InputStream in, Charset charset) {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(in, charset))) {
            int c;
            while((c = reader.read()) != -1)
                sb.append((char)c);
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
