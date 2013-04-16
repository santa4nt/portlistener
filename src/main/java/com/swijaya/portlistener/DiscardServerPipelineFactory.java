package com.swijaya.portlistener;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.logging.LoggingHandler;

public class DiscardServerPipelineFactory implements ChannelPipelineFactory {

    private boolean addLogHandler;
    private ChannelGroup channelGroup;

    public DiscardServerPipelineFactory(boolean addLogHandler, ChannelGroup channelGroup) {
        this.addLogHandler = addLogHandler;
        this.channelGroup = channelGroup;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        // configure the pipeline where our DISCARD logic lies
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("discard", new DiscardServerHandler(addLogHandler, channelGroup));
        if (addLogHandler)
            pipeline.addLast("log", new LoggingHandler("PortListener"));
        return pipeline;
    }

}
