package ServerControl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by ibrohim on 14/04/15.
 */

public class ClientServer {

    protected AsynchronousSocketChannel client;
    protected ByteBuffer readBuffer;
    protected ByteBuffer writeBuffer;
    protected Future<Integer> readBufferStatus;
    protected Future<Integer> writeBufferStatus;

    protected void receiveMessage() {
        readBuffer = ByteBuffer.allocate(1024);
        readBufferStatus = client.read(readBuffer);
    }

    public ClientServer(AsynchronousSocketChannel _client){
        client = _client;
        receiveMessage();
    }

    public void bury() throws IOException {
        client.close();
    }

    public boolean isDead() throws ExecutionException, InterruptedException {
        return readBufferStatus.get() == -1;
    }

    public boolean isReadComplete() throws ExecutionException, InterruptedException {
        return (readBufferStatus.isDone());
    }

    public boolean isWriteComplete() {
        return (writeBufferStatus!=null) && (writeBufferStatus.isDone());
    }

    public String getMessage() {
        readBuffer.flip();
        String result = new String(readBuffer.array());
        readBuffer.clear();
        return result;
    }

    public void sendMessage(String message) throws CharacterCodingException {
        Charset charset = Charset.forName("UTF-8");
        CharsetEncoder encoder = charset.newEncoder();

        writeBuffer =  encoder.encode(CharBuffer.wrap(message));
        writeBufferStatus = client.write(writeBuffer);
    }
    
    public void sendMessage(byte[] message) throws CharacterCodingException {
        Charset charset = Charset.forName("UTF-8");
        CharsetEncoder encoder = charset.newEncoder();
        
        char[] buffer = new char[message.length >> 1];
        for(int i = 0; i < buffer.length; i++) {
            int bpos = i << 1;
            char c = (char)(((message[bpos]&0x00FF)<<8) + (message[bpos+1]&0x00FF));
            buffer[i] = c;
        }

        writeBuffer =  encoder.encode(CharBuffer.wrap(buffer));
        writeBufferStatus = client.write(writeBuffer);
    }
}
