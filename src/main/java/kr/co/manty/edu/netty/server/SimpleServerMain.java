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

public class SimpleServerMain {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(10);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast("outbound", new StringEncoder());

                            p.addLast("inbound", new ChannelInboundHandlerAdapter(){
                                Boolean needsToWrite = true;

                                final AtomicInteger counter = new AtomicInteger(0);

                                Long start = null;
                                Long elapse = null;

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    start = System.currentTimeMillis();
                                    write(ctx.channel());
                                }


                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    super.channelRead(ctx, msg);
                                }

                                final Runtime runtime = Runtime.getRuntime();

                                private void write(Channel channel) throws Exception {
                                    while (needsToWrite) {
                                        if (!channel.isActive()) return;
                                        try {
                                            Thread.sleep(10L);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                        int i = counter.incrementAndGet();
                                        channel.writeAndFlush("hello" + i + "\n");
                                        System.out.println(i + " heap:" + (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + "Mi");
                                    }
                                }

                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                    System.out.println("first Event Triggered: " + evt);
                                    super.userEventTriggered(ctx, evt);
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
