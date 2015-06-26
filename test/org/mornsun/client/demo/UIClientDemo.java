package org.mornsun.client.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.apache.log4j.PropertyConfigurator;
import org.mornsun.client.ClientMgr;
import org.mornsun.client.util.ICallbackable;

class ClientFrame extends JFrame
{
    private static final long serialVersionUID = 1583310266601754257L;

    public ClientFrame() throws FileNotFoundException
    {
        setTitle("Client");
        ClientPanel panel = new ClientPanel();
        add(panel);
        pack();
    }
}

class ClientPanel extends JPanel implements ICallbackable
{
    private static final long serialVersionUID = -857208737604762581L;
    public JTextField text_uid;
    public JTextField text_ip;
    public JTextField text_app;
    public JTextField text_apv;
    public JTextField text_os;
    public JTextField text_osv;
    public JTextField text_channel;
    public JTextField text_op;
    public JTextField text_n;
    public JTextField text_p;
    public JTextField text_c;

    public JTextArea textSend;
    public JTextArea textGet;
    public JTextArea textHistory;

    JScrollPane scrollPaneSend;
    JScrollPane scrollPaneGet;
    JScrollPane scrollPaneHistory;

    private int m_iMsg = 0;

    // public InfoClientMgr client;

    public ClientPanel()
    {
        setLayout(new BorderLayout());

        JPanel panelShow = new JPanel();
        panelShow.setLayout(new GridLayout(1, 2));
        JPanel panelShowLeft = new JPanel();
        panelShowLeft.setLayout(new BorderLayout());
        JPanel panelShowRight = new JPanel();
        panelShowRight.setLayout(new BorderLayout());
        panelShow.add(panelShowLeft);
        panelShow.add(panelShowRight);

        JLabel labelSend = new JLabel("Msg to be sent:");
        JLabel labelGet = new JLabel("Msgs:");
        JLabel labelHistory = new JLabel("History:");
        textSend = new JTextArea();
        textSend.setText("");
        textSend.setEditable(false);
        textGet = new JTextArea();
        textGet.setEditable(false);
        textHistory = new JTextArea();
        textHistory.setEditable(false);
        scrollPaneSend = new JScrollPane(textSend);
        scrollPaneGet = new JScrollPane(textGet);
        scrollPaneHistory = new JScrollPane(textHistory);
        Border border = BorderFactory.createBevelBorder(EtchedBorder.RAISED, Color.WHITE,
                Color.GRAY);
        scrollPaneSend.setBorder(border);
        scrollPaneGet.setBorder(border);
        labelSend.setBorder(border);
        labelGet.setBorder(border);

        panelShowLeft.add(labelSend, BorderLayout.NORTH);
        panelShowLeft.add(scrollPaneSend);
        panelShowRight.add(labelGet, BorderLayout.NORTH);
        panelShowRight.add(scrollPaneGet);
        add(panelShow, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(11, 2));

        JLabel label_uid = new JLabel("uid");
        JLabel label_ip = new JLabel("ip");
        JLabel label_app = new JLabel("os_app");
        JLabel label_app_ver = new JLabel("app_ver");
        JLabel label_os = new JLabel("os");
        JLabel label_osv = new JLabel("os_ver");
        JLabel label_channel = new JLabel("channel");
        JLabel label_op = new JLabel("operator");
        JLabel label_n = new JLabel("nation");
        JLabel label_p = new JLabel("province");
        JLabel label_c = new JLabel("city");

        text_uid = new JTextField();
        text_uid.setText("50001");
        text_ip = new JTextField();
        text_ip.setText("106.3.32.1");
        text_app = new JTextField();
        text_app.setText("YP");
        text_apv = new JTextField();
        text_apv.setText("1.3.1");
        text_os = new JTextField();
        text_os.setText("IOS");
        text_osv = new JTextField();
        text_osv.setText("7.0.1");
        text_channel = new JTextField();
        text_channel.setText("mi");
        text_op = new JTextField();
        text_op.setText("cmcc");
        text_n = new JTextField();
        text_n.setText("中国");
        text_p = new JTextField();
        text_p.setText("湖南");
        text_c = new JTextField();
        text_c.setText("长沙");

        panel.add(label_uid);
        panel.add(text_uid);
        panel.add(label_ip);
        panel.add(text_ip);
        panel.add(label_app);
        panel.add(text_app);
        panel.add(label_app_ver);
        panel.add(text_apv);
        panel.add(label_os);
        panel.add(text_os);
        panel.add(label_osv);
        panel.add(text_osv);
        panel.add(label_channel);
        panel.add(text_channel);
        panel.add(label_op);
        panel.add(text_op);
        panel.add(label_n);
        panel.add(text_n);
        panel.add(label_p);
        panel.add(text_p);
        panel.add(label_c);
        panel.add(text_c);

        JPanel panelTop = new JPanel();
        panelTop.setLayout(new GridLayout(1, 2));
        panelTop.add(panel);
        JPanel panelHistory = new JPanel();
        panelHistory.setLayout(new BorderLayout());
        panelHistory.add(labelHistory, BorderLayout.NORTH);
        panelHistory.add(scrollPaneHistory);
        panelTop.add(panelHistory);
        add(panelTop, BorderLayout.NORTH);

        JPanel panel_control = new JPanel();
        JButton btn_send = new JButton("Send");
        btn_send.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent action)
            {
                ClientMgr.getInstance();
                long currTime = System.currentTimeMillis();
                try {
                    InfoTalk talk = new InfoTalk(m_iMsg, ClientPanel.this);
                    talk.handle(text_uid.getText(), text_ip.getText(), text_app.getText(),
                            text_apv.getText(), text_os.getText(), text_osv.getText(),
                            text_channel.getText(), text_op.getText(), text_n.getText(),
                            text_p.getText(), text_c.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Current:" + m_iMsg);
                }
                textHistory.append("======================\n" + textSend.getText() + "\n");
                System.out.println("Time used:" + (System.currentTimeMillis() - currTime));
                m_iMsg++;
            }
        });

        panel_control.setLayout(new BorderLayout());
        JPanel flowPanel = new JPanel();
        flowPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        flowPanel.add(btn_send);
        panel_control.add(flowPanel, BorderLayout.NORTH);
        add(panel_control, BorderLayout.SOUTH);
    }

    public void callback(Object response)
    {
        if (textGet != null) {
            textGet.append(response.toString());
        } else {
            textGet.append("null\n");
        }
        textGet.setCaretPosition(textGet.getText().length()); // Scroll to the bottom
        // JScrollBar bar = scrollPaneGet.getVerticalScrollBar();
        // bar.setValue(bar.getMaximum());
    }

    public void processException(Throwable cause, Object attachment)
    {
        // TODO Auto-generated method stub

    }
}

/**
 * The Class UIClient.
 */
public class UIClientDemo extends JFrame
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -2298125237835718413L;

    /** The Constant WIDTH. */
    static final int WIDTH = 800;

    /** The Constant HEIGHT. */
    static final int HEIGHT = 600;

    /**
     * The main method.
     * 
     * @param args
     *            the arguments
     * @throws FileNotFoundException
     *             the file not found exception
     */
    public static void main(String[] args) throws FileNotFoundException
    {
        // BasicConfigurator.configure();
        String configFolder = System.getProperty("config", "./config/");
        System.setProperty("infoserver.dir", configFolder + "../");
        // log configuration
        String log4jConfig = configFolder + File.separator + "log4j.properties";
        System.out.println("log4j: " + log4jConfig);
        PropertyConfigurator.configure(log4jConfig);

        ClientMgr.getInstance();

        // new NewUIClient();
        ClientFrame frame = new ClientFrame();
        frame.setTitle("UI Tiny_Client for InfoServer");
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        frame.setLocation(x, y);

    }
}
