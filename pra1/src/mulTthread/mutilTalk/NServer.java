package mulTthread.mutilTalk;

import com.sun.nio.sctp.SctpServerChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;

/**
 * Created by chen on 2017/8/24.
 */
public class NServer {
    private Selector selector = null;
    static final int port = 3332;

    private Charset charset = Charset.forName("UTF-8");
    public void init() throws IOException {
        selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();  //监听请求
        server.bind(new InetSocketAddress(port));
        //非阻塞方式
        server.configureBlocking(false);
        //注册到选择器上，设为监听状态
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server is listening now....");

        //阻塞监听,select函数为阻塞
        while (selector.select() > 0) {
            //找到可用的socketChannel
            for (SelectionKey sk:selector.selectedKeys()) {
                selector.selectedKeys().remove(sk);
                if (sk.isAcceptable()) {
                    SocketChannel sc = server.accept();
                    sc.configureBlocking(false); //设为非阻模式
                    //注册选择器，设为读取模式
                    sc.register(selector,SelectionKey.OP_READ);
                    //将对应的channel设置为准备接受其他客户端请求
                    sk.interestOps(SelectionKey.OP_ACCEPT);
                    System.out.println("Server is listening from client"+sc.getRemoteAddress());
                }

                //处理客户端请求
                if (sk.isReadable()) {
                    SocketChannel sc = (SocketChannel)sk.channel();
                    ByteBuffer buff = ByteBuffer.allocate(1024);
                    StringBuilder content = new StringBuilder();

                    try {
                        while(sc.read(buff) > 0) {
                            buff.flip();  //从写模式改为读模式
                            content.append(charset.decode(buff));
                        }
                        System.out.println("Server is listening from client"+sc.getRemoteAddress());
                        //将此对应的channel设置为准备下一次接受数据
                        sk.interestOps(SelectionKey.OP_READ);
                    }catch (IOException io) {
                        sk.channel();
                        if (sk.channel() != null) {
                            sk.channel().close();
                        }
                    }

                    if (content.length() > 0) {
                        //广播到所有的SocketChannel中
                        for (SelectionKey key: selector.keys()) {
                            Channel targetChannel = key.channel();
                            if (targetChannel instanceof SocketChannel) {
                                SocketChannel dest = (SocketChannel)targetChannel;
                                dest.write(charset.encode(content.toString()));
                            }
                        }
                    }

                }

            }
        }
    }
    public static void main(String[] args) throws IOException {
        new NServer().init();
    }
}
