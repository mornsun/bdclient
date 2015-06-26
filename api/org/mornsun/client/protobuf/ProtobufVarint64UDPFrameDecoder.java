package org.mornsun.client.protobuf;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.CorruptedFrameException;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.google.protobuf.CodedInputStream;

/**
 * A decoder that splits the received {@link ChannelBuffer}s dynamically by the value of the Google Protocol Buffers <a
 * href="http://code.google.com/apis/protocolbuffers/docs/encoding.html#varints">Base 128 Varints</a> integer length
 * field in the message. For example:
 * 
 * <pre>
 * BEFORE DECODE (306 bytes)                 AFTER DECODE (300 bytes)
 * +--------+---------+---------------+      +---------------+
 * | Length |   Tag   | Protobuf Data |----->| Protobuf Data |
 * | 0xAC02 | 0.1.0.0 |  (300 bytes)  |      |  (300 bytes)  |
 * +--------+---------+---------------+      +---------------+
 * </pre>
 * 
 * @see CodedInputStream
 */
public class ProtobufVarint64UDPFrameDecoder extends FrameDecoder
{
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception
	{
		final byte[] buf = new byte[5];
		for (int i = 0; i < buf.length; i++)
		{
			if (!buffer.readable())
			{
				buffer.clear();
				return null;
			}
			buf[i] = buffer.readByte();
			if (buf[i] >= 0)
			{
				int length = CodedInputStream.newInstance(buf, 0, i + 1).readRawVarint32();
				if (length < 0)
				{
					buffer.clear();
					throw new CorruptedFrameException("negative length: " + length);
				}
				if (buffer.readableBytes() < 4 + length)
				{
					buffer.clear();
					return null;
				}
				else
				{
					buffer.readInt();
					return buffer.readBytes(length);
				}
			}
		}
		buffer.clear();
		throw new CorruptedFrameException("length wider than 32-bit");
	}
}
