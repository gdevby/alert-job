package by.gdev.common.service.playwright.sessions.utils;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Файл сессии: magic AJS1 + length + UTF-8 JSON (Playwright storageState).
 */
public final class MappedSessionFile {

    static final int MAGIC = 0x414A5331;
    static final int HEADER_SIZE = 8;

    private MappedSessionFile() {
    }

    public static void write(Path path, String json, int maxPayloadBytes) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        if (payload.length > maxPayloadBytes) {
            throw new IOException("storageState слишком большой: " + payload.length + " байт, лимит " + maxPayloadBytes);
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        int fileSize = HEADER_SIZE + payload.length;
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            channel.truncate(fileSize);
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            mapped.putInt(MAGIC);
            mapped.putInt(payload.length);
            mapped.put(payload);
            mapped.force();
        }
    }

    public static Optional<String> read(Path path, int maxPayloadBytes) throws IOException {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        long size = Files.size(path);
        if (size < HEADER_SIZE) {
            return Optional.empty();
        }
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
            int magic = mapped.getInt();
            if (magic != MAGIC) {
                throw new IOException("неверный magic в " + path);
            }
            int length = mapped.getInt();
            if (length < 0 || length > maxPayloadBytes || HEADER_SIZE + length > size) {
                throw new IOException("некорректная длина payload: " + length);
            }
            byte[] payload = new byte[length];
            mapped.get(payload);
            return Optional.of(new String(payload, StandardCharsets.UTF_8));
        }
    }
}
