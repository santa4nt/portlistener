package com.swijaya.portlistener;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;

public class DiscardServerHandler extends SimpleChannelHandler {

    private final boolean sendUpstream;
    private final ChannelGroup channelGroup;

    public DiscardServerHandler(boolean sendUpstream, ChannelGroup channelGroup) {
        this.sendUpstream = sendUpstream;
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channelGroup.add(e.getChannel());
        super.channelOpen(ctx, e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channelGroup.remove(e.getChannel());
        super.channelClosed(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        // by doing nothing here, we effectively discard the bytes we receive in the message event's buffer
        {
            // do nothing
        }

        // however, if we pass this message along upstream, the next handler in the pipeline might
        // have some use for it (e.g. for logging purposes)
        // for example, with the LoggingHandler we configured in the bootstrap pipeline:
        /*
        santa@samoyed:~/Code/portlistener$ mvn clean compile exec:exec -Puse-log4j
        [INFO] Scanning for projects...
        [INFO]
        [INFO] ------------------------------------------------------------------------
        [INFO] Building Port Listener 1.0-SNAPSHOT
        [INFO] ------------------------------------------------------------------------
        [snip]
        [INFO] --- exec-maven-plugin:1.2.1:exec (default-cli) @ portlistener ---
        DEBUG SelectorUtil - Using select timeout of 500
        DEBUG SelectorUtil - Epoll-bug workaround enabled = false
        DEBUG PortListener - [id: 0x2dd092c3, /127.0.0.1:54872 => /127.0.0.1:8080] OPEN
        DEBUG PortListener - [id: 0x2dd092c3, /127.0.0.1:54872 => /127.0.0.1:8080] BOUND: /127.0.0.1:8080
        DEBUG PortListener - [id: 0x2dd092c3, /127.0.0.1:54872 => /127.0.0.1:8080] CONNECTED: /127.0.0.1:54872
        DEBUG PortListener - [id: 0x2dd092c3, /127.0.0.1:54872 => /127.0.0.1:8080] RECEIVED: BigEndianHeapChannelBuffer(ridx=0, widx=13, cap=13)
                 +-------------------------------------------------+
                 |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
        +--------+-------------------------------------------------+----------------+
        |00000000| 68 65 6c 6c 6f 2c 20 77 6f 72 6c 64 0a          |hello, world.   |
        +--------+-------------------------------------------------+----------------+
        DEBUG PortListener - [id: 0x2dd092c3, /127.0.0.1:54872 :> /127.0.0.1:8080] DISCONNECTED
        DEBUG PortListener - [id: 0x2dd092c3, /127.0.0.1:54872 :> /127.0.0.1:8080] UNBOUND
        DEBUG PortListener - [id: 0x2dd092c3, /127.0.0.1:54872 :> /127.0.0.1:8080] CLOSED
         */
        if (sendUpstream) {
            ctx.sendUpstream(e);
            // alternatively, we can do our own logging here to ensure that the message buffer is truly discarded at this layer
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        //e.getCause().printStackTrace();   // should be logged by a logging handler in the pipeline
        Channel ch = e.getChannel();
        ch.close();
    }

}
