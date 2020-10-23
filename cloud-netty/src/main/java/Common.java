import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Common {
    private static Common ourInstance = new Common();

    public static Common getInstance() {
        return ourInstance;
    }

    private Common() {
    }

    private static Channel currentChannel;

    public static Channel getCurrentChannel() {
        return currentChannel;
    }

    public void start(CountDownLatch countDownLatch) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrapCommon = new Bootstrap();
            bootstrapCommon.group(group);
            bootstrapCommon.channel(NioSocketChannel.class);
            bootstrapCommon.remoteAddress(new InetSocketAddress("localhost", 8189));
            bootstrapCommon.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast();
                    currentChannel = socketChannel;
                }
            });
            ChannelFuture channelFuture = bootstrapCommon.connect().sync();  //открывает соединение
            countDownLatch.countDown(); //сообщает об открытии
            channelFuture.channel().closeFuture().sync(); //ждет остановки
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void stop() {
        currentChannel.close();
    }
}



