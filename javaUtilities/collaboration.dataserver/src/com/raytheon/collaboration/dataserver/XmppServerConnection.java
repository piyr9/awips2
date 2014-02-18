/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.collaboration.dataserver;

import java.net.UnknownHostException;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.SyncPacketSend;

/**
 * Starts and runs XMPP client thread for communication with XMPP server
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 14, 2014 2756       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class XmppServerConnection implements Runnable {

    private static final int PACKET_SEND_TIMEOUT = 5000; // 5 seconds

    private final XMPPConnection conn;

    private final String user;

    private final String password;

    private final String xmppServerAddress;

    private final String dataServerUrl;

    private final Logger log = Log.getLogger(this.getClass());

    /**
     * Creates connection and logs in
     * 
     * @throws XMPPException
     * @throws UnknownHostException
     */
    public XmppServerConnection() throws XMPPException, UnknownHostException {
        this.dataServerUrl = Config.getDataserverUrl();
        this.user = Config.getXmppUsername();
        this.password = Config.getXmppPassword();
        this.xmppServerAddress = Config.getProp(Config.XMPP_SERVER_KEY,
                Config.XMPP_SERVER_DEFAULT);
        this.conn = new XMPPConnection(xmppServerAddress);
        this.conn.connect();
        this.conn.login(user, password);
        log.debug("Connected to XMPP server at address: " + xmppServerAddress);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        conn.addPacketListener(new PacketListener() {
            @Override
            public void processPacket(Packet packet) {
                log.debug(packet.toXML());
            }
        }, new PacketFilter() {
            @Override
            public boolean accept(Packet packet) {
                return true;
            }
        });
        IQ packet = new IQ() {
            @Override
            public String getChildElementXML() {
                return "<query xmlns=\"urn:uf:viz:collaboration:iq:http\">"
                        + "<httpinfo xmlns=\"urn:uf:viz:collaboration\">"
                        + "<url>" + dataServerUrl + "</url></httpinfo></query>";
            }
        };
        packet.setType(Type.SET);
        try {
            Packet reply = SyncPacketSend.getReply(conn, packet,
                    PACKET_SEND_TIMEOUT);
            log.debug("URL configuration set response: " + reply.toXML());
        } catch (XMPPException e) {
            log.warn("Problem sending URL configuration packet", e);
        }
        while (conn.isConnected()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.warn("Server connection thread interrupted", e);
                disconnect();
            }
        }
    }

    /**
     * disconnect from XMPP server
     */
    public void disconnect() {
        log.debug("Disconnecting from XMPP server");
        conn.disconnect();
    }

}