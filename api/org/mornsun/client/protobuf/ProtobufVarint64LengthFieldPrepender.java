package org.mornsun.client.protobuf;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.google.protobuf.CodedOutputStream;

/*
 * 编码netty要发的穿
 * 将长度信息和类型码插入到头部
 */
@Sharable
public class ProtobufVarint64LengthFieldPrepender extends OneToOneEncoder
{

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception
	{
		if (!(msg instanceof ChannelBuffer))
		{
			return msg;
		}

		ChannelBuffer body = (ChannelBuffer) msg;
		int length = body.readableBytes();
		ChannelBuffer header = channel.getConfig().getBufferFactory()
				.getBuffer(body.order(), CodedOutputStream.computeRawVarint64Size(length) + 4);
		CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(new ChannelBufferOutputStream(header));
		codedOutputStream.writeRawVarint64(length);
		int value = 0x0;
		value |= (0x0 & 0xff);// network version
		value |= ((0x0 & 0xff) << 8);// type
		if (ctx.getPipeline().get("cryptoEncoder") != null) value |= ((0x1 & 0xf) << 16);// crypto type
		else value |= ((0x0 & 0xf) << 16);// crypto type
		value |= ((0x0 & 0xfff) << 20);// version
		codedOutputStream.writeRawLittleEndian32(value);
		codedOutputStream.flush();
		return wrappedBuffer(header, body);
	}

}
