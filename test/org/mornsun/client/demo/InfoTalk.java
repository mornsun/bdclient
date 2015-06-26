/**
 * 
 */
package org.mornsun.client.demo;

import java.io.File;

import org.apache.log4j.PropertyConfigurator;
import org.mornsun.client.ClientMgr;
import org.mornsun.client.framework.Talk;
import org.mornsun.client.util.ICallbackable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat.ParseException;
import org.mornsun.client.protocol.InfoProtocol;
import org.mornsun.client.protocol.InfoProtocol.InfoReqPack;

/**
 * @author Chauncey
 *
 */
public class InfoTalk extends Talk
{
    private static final Logger log = LoggerFactory.getLogger(InfoTalk.class);

    private static int g_last_checksum = 0; // identify the last package to print time elapsed
    private static long g_start_time = 0; // record start time

    private int m_checkSum = -1; // verify the response correspond with its request
    private ICallbackable m_callback = null; // further callback, which may be the owner of the talk, like a GUI delegate, or a middleware

    /**
	 * 
	 */
    public InfoTalk()
    {
        // TODO Auto-generated constructor stub
    }

    /**
     * 
     * @param checkSum
     */
    public InfoTalk(int checkSum)
    {
        m_checkSum = checkSum;
    }

    /**
     * 
     * @param checkSum
     * @param callback
     */
    public InfoTalk(int checkSum, ICallbackable callback)
    {
        m_checkSum = checkSum;
        m_callback = callback;
    }

    /**
     * Asynchronous callback to parse the response of downstream
     * 
     * @see org.mornsun.client.util.ICallbackable#callback(java.lang.Object)
     */
    @Override
    public void callback(Object response)
    {
        // TODO Auto-generated method stub
        if (response == null) {
            log.warn("Empty response. Please check your protocol.");
            return;
        }

        InfoProtocol.InfoResPack proto = (InfoProtocol.InfoResPack) response;
        String isp = proto.getIsp();
        // String nation = proto.getNation();
        // String province = proto.getProvince();
        // String city = proto.getCity();
        // List<Integer> sids = proto.getSidsList();
        // if (!isp.equals("000000"+m_checkSum)) {
        // System.out.println("error:" + m_checkSum + " isp:" + isp);
        // }
        if (g_last_checksum == Integer.parseInt(isp)) { // when receive the last package, print time elapsed.
            System.out.println("Recv time elapsed:" + (System.currentTimeMillis() - g_start_time));
        }

        String tmpsString = "=========================\n" + "count:" + m_checkSum + "\n"
                + proto.toString();
        if (null != m_callback) { // further callback, which may be the owner of the talk, like a GUI delegate, or a middleware
            m_callback.callback(tmpsString);
        }
    }

    /**
     * Handle a request, customized by an user's protocol
     * 
     * @param uid
     * @param ip
     * @param app
     * @param apv
     * @param os
     * @param osv
     * @param ch
     * @param op
     * @param nation
     * @param province
     * @param city
     * @throws ParseException
     * @throws InvalidProtocolBufferException
     * @throws Exception
     */
    public void handle(String uid, String ip, String app, String apv, String os, String osv,
            String ch, String op, String nation, String province, String city)
            throws ParseException, InvalidProtocolBufferException, Exception
    {
        InfoReqPack.Builder builder = InfoReqPack.newBuilder();
        if (null != uid && !"".equals(uid))
            builder.setUid(uid);
        if (null != ip && !"".equals(ip))
            builder.setIp(ip);
        if (null != app && !"".equals(app))
            builder.setApp(app);
        if (null != apv && !"".equals(apv))
            builder.setAppVer(apv);
        if (null != os && !"".equals(os))
            builder.setOs(os);
        if (null != osv && !"".equals(osv))
            builder.setOsVer(osv);
        if (null != ch && !"".equals(ch))
            builder.setChannel(ch);
        if (null != op && !"".equals(op))
            builder.setIsp(op);
        if (null != nation && !"".equals(nation))
            builder.setNation(nation);
        if (null != province && !"".equals(province))
            builder.setProvince(province);
        if (null != city && !"".equals(city))
            builder.setCity(city);
        builder.setLocSwitch(true);
        InfoReqPack pack = builder.build();
        // if (ClientMgr.getInstance().send("ADAPTER", pack, this)) {
        if (ClientMgr.getInstance().send("INFO", pack, this)) {
             log.info("REQ succ:" + this.m_checkSum);
        } else {
            log.info("REQ failed:" + this.m_checkSum);
            throw new Exception("send fail");
        }
    }

    /**
     * A test program to send multiple requests asynchronously to downstream (almost at the same time)
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub
        String configFolder = System.getProperty("config", "./config/");
        System.setProperty("bdclient.dir", configFolder + "../");
        // log configuration
        String log4jConfig = configFolder + File.separator + "log4j.properties";
        System.out.println("log4j: " + log4jConfig);
        PropertyConfigurator.configure(log4jConfig);

        ClientMgr.getInstance();

        g_start_time = System.currentTimeMillis();
        g_last_checksum = 100;
        for (int i = 1; i <= g_last_checksum; i++) {
            InfoTalk talk = new InfoTalk(i);
            try {
                talk.handle("50001", "106.3.32.1", "YP", "1.3.1", "IOS", "7.0.1", "mi", "cmcc" + i,
                        "中国", "湖南", "长沙");
                // Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Current:" + i);
                break;
            }
        }
        System.out.println("Send time elapsed:" + (System.currentTimeMillis() - g_start_time));
    }

}
