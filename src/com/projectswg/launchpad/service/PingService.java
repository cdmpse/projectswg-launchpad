/*
 * 
 * This file is part of ProjectSWG Launchpad.
 *
 * ProjectSWG Launchpad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * ProjectSWG Launchpad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with ProjectSWG Launchpad.  If not, see <http://www.gnu.org/licenses/>.      
 *
 */

package com.projectswg.launchpad.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.projectswg.launchpad.ProjectSWG;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class PingService extends Service<String>
{
	public static final int PING_MAX_SIZE = 1024;
    public static final int PING_TIMEOUT = 3000;
	
	private final Manager manager;
	
	
    public PingService(Manager manager)
    {
       	this.manager = manager;
	}
    
	@Override
	protected Task<String> createTask() {

		return new Task<String>() {

			@Override
			protected String call() throws Exception {
				
				String host = manager.getLoginServerHost().getValue();
				String port = manager.getLoginServerPingPort().getValue();
				
				if (host == null || port == null) {
					ProjectSWG.log("PingService: host or port null");
					return "ERR";
				}
				if (host.equals("") || port.equals("")) {
					ProjectSWG.log(String.format("PingService: host or port was blank ... host = %s, port = %s", host, port));
					return "ERR";
				}
				ProjectSWG.log(String.format("Pinging: host = %s, port = %s", host, port));
				try {
					String msg = "ping";
					long startTime = System.currentTimeMillis();
					DatagramSocket socket = new DatagramSocket();
					InetAddress server = InetAddress.getByName(host);
					DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), server, Integer.parseInt(port));
				    socket.send(packet);
					DatagramPacket response = new DatagramPacket(new byte[PING_MAX_SIZE], PING_MAX_SIZE);
					socket.setSoTimeout(PING_TIMEOUT);
					socket.receive(response);
					socket.close();
					long endTime = System.currentTimeMillis();
					String responseText = new String(response.getData());
					ProjectSWG.log("Ping response: " + responseText + " -> " + (endTime - startTime));
					return "" + (endTime - startTime);
				} catch (UnknownHostException e1) {
					ProjectSWG.log(e1.toString());
					return "UNK";
				} catch (SocketTimeoutException e2) {
					ProjectSWG.log(e2.toString());
					return "TO";
				} catch (IOException e3) {
					ProjectSWG.log(e3.toString());
					return "?";
				}
			}
		};
	}
}
