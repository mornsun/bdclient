package org.mornsun.client.protobuf;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.MessageLite;

public class ProtobufLiteDecoder extends OneToOneDecoder
{
	private final MessageLite prototype;
	private final ExtensionRegistryLite extensionRegistry;

	/**
	 * Creates a new instance.
	 */
	public ProtobufLiteDecoder(MessageLite prototype)
	{
		this(prototype, null);
	}

	public ProtobufLiteDecoder(MessageLite prototype, ExtensionRegistryLite extensionRegistry)
	{
		if (prototype == null)
		{
			throw new NullPointerException("prototype");
		}
		this.prototype = prototype.getDefaultInstanceForType();
		this.extensionRegistry = extensionRegistry;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception
	{
		if (!(msg instanceof ChannelBuffer))
		{
			return msg;
		}

		ChannelBuffer buf = (ChannelBuffer) msg;
		if (buf.hasArray())
		{
			final int offset = buf.readerIndex();
			if (extensionRegistry == null)
			{
				return prototype.newBuilderForType()
						.mergeFrom(buf.array(), buf.arrayOffset() + offset, buf.readableBytes()).build();
			}
			else
			{
				return prototype.newBuilderForType()
						.mergeFrom(buf.array(), buf.arrayOffset() + offset, buf.readableBytes(), extensionRegistry)
						.build();
			}
		}
		else
		{
			if (extensionRegistry == null)
			{
				return prototype.newBuilderForType().mergeFrom(new ChannelBufferInputStream((ChannelBuffer) msg))
						.build();
			}
			else
			{
				return prototype.newBuilderForType()
						.mergeFrom(new ChannelBufferInputStream((ChannelBuffer) msg), extensionRegistry).build();
			}
		}
	}
}
