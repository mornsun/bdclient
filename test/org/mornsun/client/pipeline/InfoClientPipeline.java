package org.mornsun.client.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.mornsun.client.framework.KeeyAliveClientHandler;
import org.mornsun.client.protobuf.ProtobufVarint64FrameDecoder;
import org.mornsun.client.protobuf.ProtobufVarint64LengthFieldPrepender;
import org.mornsun.client.protocol.InfoProtocol;

/**
 * 
 * @author Chauncey
 *
 */
public class InfoClientPipeline extends ClientPipelineFactory
{
    protected static final OneToOneEncoder g_frameEncoder = new ProtobufVarint64LengthFieldPrepender(); // Reused in PROCESS scale
    protected static final OneToOneEncoder g_protobufEncoder = new ProtobufEncoder(); // Reused in PROCESS scale
    protected static final OneToOneDecoder g_protobufDecoder = new ProtobufDecoder(
            InfoProtocol.InfoResPack.getDefaultInstance()); // Reused in PROCESS scale

    protected final SimpleChannelUpstreamHandler m_handler; // Reused in SERVICE scale

    /**
	 * 
	 */
    public InfoClientPipeline()
    {
        m_handler = new KeeyAliveClientHandler();
    }

    /**
	 * 
	 */
    @Override
    public ChannelPipeline getPipeline() throws Exception
    {
        ChannelPipeline p = Channels.pipeline();
        p.addLast("frameEncoder", g_frameEncoder);
        p.addLast("frameDecoder", new ProtobufVarint64FrameDecoder()); // MUST be reused in CHANNEL scale
        p.addLast("protobufEncoder", g_protobufEncoder);
        p.addLast("protobufDecoder", g_protobufDecoder);
        p.addLast("timeoutHandler", m_timeoutHandler);
        p.addLast("handler", m_handler);
        return p;
    }
}
