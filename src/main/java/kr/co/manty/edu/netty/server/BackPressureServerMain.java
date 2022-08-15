package kr.co.manty.edu.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class BackPressureServerMain {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(10);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,  new WriteBufferWaterMark(1*1024,8*1024));
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast("outbound", new StringEncoder());

                            p.addLast("inbound", new ChannelInboundHandlerAdapter(){
                                boolean needsToWrite = true;

                                final Runtime runtime = Runtime.getRuntime();
                                final AtomicInteger counter = new AtomicInteger(0);

                                Long start = null;
                                Long elapse = null;

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    start = System.currentTimeMillis();
                                    writeIfPossible(ctx.channel());
                                }

                                @Override
                                public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                                    writeIfPossible(ctx.channel());
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    super.channelRead(ctx, msg);
                                }


                                private void writeIfPossible(Channel channel) {
                                    while(needsToWrite && channel.isWritable()) {
                                        int i = counter.incrementAndGet();
                                        channel.writeAndFlush("hello"+ i +"\n" );

                                        System.out.println("writability status: " + channel.isWritable());

                                        System.out.println(i + " heap:" + (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + "Mi");
                                    }
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    needsToWrite = false;
                                    elapse = System.currentTimeMillis() - this.start;
                                    System.out.println("elapse:" + elapse/1000 + "s");
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    needsToWrite = false;
                                    System.err.println(cause);
                                }
                            });


                        }

                    });
            ChannelFuture f = b.bind(9999).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

        }

    }
}
