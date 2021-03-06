//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2008 Jan 23: Java 5 generics, log() method, format code. - dj@opennms.org
// 2003 Jan 31: Cleaned up some unused imports.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.                                                            
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact: 
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//

package org.infosec.ismp.eventd.adaptors.udp;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.infosec.ismp.model.event.Event;
import org.infosec.ismp.model.event.Log;

/**
 * 
 * 将接收到的udp内容转成相关的事件。
 * 
 */
final class UdpReceivedEvent {
	/**
	 * The received XML event, decoded using the UTF-8 encoding.
	 */
	private String m_eventXML;

	/**
	 * The decoded event document. The classes are defined in an XSD and
	 * generated by castor.
	 */
	private Log m_log;

	/**
	 * The internet addrress of the sending agent.
	 */
	private InetAddress m_sender;

	/**
	 * The port of the agent on the remote system.
	 */
	private int m_port;

	/**
	 * The list of event that have been acknowledged.
	 */
	private List<Event> m_ackEvents;

	/**
	 * Private constructor to prevent the used of <em>new</em> except by the
	 * <code>make</code> method.
	 */
	private UdpReceivedEvent() {
		// constructor not supported except through make method!
	}

	/**
	 * Constructs a new event encapsulation instance based upon the information
	 * passed to the method. The passed datagram data is decoded into a string
	 * using the <tt>US-ASCII</tt> character encoding.
	 * 
	 * @param packet
	 *            The datagram received from the remote agent.
	 * 
	 * @throws java.io.UnsupportedEncodingException
	 *             Thrown if the data buffer cannot be decoded using the
	 *             US-ASCII encoding.
	 */
	static UdpReceivedEvent make(DatagramPacket packet)
			throws UnsupportedEncodingException {
		return make(packet.getAddress(), packet.getPort(), packet.getData(),
				packet.getLength());
	}

	/**
	 * Constructs a new event encapsulation instance based upon the information
	 * passed to the method. The passed byte array is decoded into a string
	 * using the <tt>US-ASCII</tt> character encoding.
	 * 
	 * @param addr
	 *            The remote agent's address.
	 * @param port
	 *            The remote agent's port
	 * @param data
	 *            The XML data in US-ASCII encoding.
	 * @param len
	 *            The length of the XML data in the buffer.
	 * 
	 * @throws java.io.UnsupportedEncodingException
	 *             Thrown if the data buffer cannot be decoded using the
	 *             US-ASCII encoding.
	 */
	static UdpReceivedEvent make(InetAddress addr, int port, byte[] data,
			int len) throws UnsupportedEncodingException {
		UdpReceivedEvent e = new UdpReceivedEvent();
		e.m_sender = addr;
		e.m_port = port;
		e.m_eventXML = new String(data, 0, len, "UTF-8");
		e.m_ackEvents = new ArrayList<Event>(16);
		e.m_log = null;
		return e;
	}

	/**
	 * Decodes the XML package from the remote agent. If an error occurs or the
	 * datagram had malformed XML then an exception is generated.
	 * 
	 * @return The toplevel <code>Log</code> element of the XML document.
	 * 
	 * @throws org.exolab.castor.xml.ValidationException
	 *             Throws if the documents data does not match the defined XML
	 *             Schema Definition.
	 * @throws org.exolab.castor.xml.MarshalException
	 *             Thrown if the XML is malformed and cannot be converted.
	 */
	@SuppressWarnings("deprecation")
	Log unmarshal() throws MarshalException, ValidationException {

		if (m_log == null) {
			StringReader rdr = new StringReader(m_eventXML);
			m_log = (Log) Unmarshaller.unmarshal(Log.class, rdr);
		}
		return m_log;
	}

	/**
	 * Adds the event to the list of events acknowledged in this event XML
	 * document.
	 * 
	 * @param e
	 *            The event to acknowledge.
	 */
	void ackEvent(Event e) {
		if (!m_ackEvents.contains(e)) {
			m_ackEvents.add(e);
		}
	}

	/**
	 * Returns the raw XML data as a string.
	 */
	String getXmlData() {
		return m_eventXML;
	}

	/**
	 * Returns the sender's address.
	 */
	InetAddress getSender() {
		return m_sender;
	}

	/**
	 * Returns the sender's port
	 */
	int getPort() {
		return m_port;
	}

	/**
	 * Get the acknowledged events
	 */
	public List<Event> getAckedEvents() {
		return m_ackEvents;
	}

	/**
	 * Returns true if the instance matches the object based upon the remote
	 * agent's address &amp; port. If the passed instance is from the same agent
	 * then it is considered equal.
	 * 
	 * @param o
	 *            instance of the class to compare.
	 * 
	 * @return Returns true if the objects are logically equal, false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof UdpReceivedEvent) {
			UdpReceivedEvent e = (UdpReceivedEvent) o;
			return (this == e || (m_port == e.m_port && m_sender
					.equals(e.m_sender)));
		}
		return false;
	}

	/**
	 * Returns the hash code of the instance. The hash code is computed by
	 * taking the bitwise XOR of the port and the agent's internet address hash
	 * code.
	 * 
	 * @return The 32-bit has code for the instance.
	 */
	@Override
	public int hashCode() {
		return (m_port ^ m_sender.hashCode());
	}
}
