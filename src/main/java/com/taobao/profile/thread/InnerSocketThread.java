/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.thread;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.taobao.profile.Manager;
import com.taobao.profile.runtime.MethodCache;

/**
 * 对外提供Socket开关
 * 
 * @author shutong.dy
 * @since 2012-1-11
 */
public class InnerSocketThread extends Thread {
	private static final int MAX_COMMAND_LENGTH = 1024;
	/**
	 * server
	 */
	private ServerSocket socket;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			com.taobao.profile.config.ProfConfig config = Manager.instance().getProfConfig();
			String bindAddress = config.getSocketBindAddress(); // Fetches from ProfConfig
			int port = Manager.PORT; // Manager.PORT is already available via ProfConfig during Manager init

			try {
				 socket = new ServerSocket(port, 50, InetAddress.getByName(bindAddress));
				 System.out.println("TProfiler: InnerSocketThread listening on " + bindAddress + ":" + port);
			} catch (java.net.UnknownHostException e) {
				 System.err.println("TProfiler: InnerSocketThread could not bind to address " + bindAddress + ". Defaulting to all interfaces.");
				 e.printStackTrace(); // Log the error
				 socket = new ServerSocket(port); // Fallback to original behavior
			}
			while (true) {
				Socket child = socket.accept();

				child.setSoTimeout(5000);

				String rawFullCommand = read(child.getInputStream());
				
				String authToken = config.getSocketAuthToken();
				String actualCommand = rawFullCommand;
				boolean authenticated = false;

				if (authToken != null && !authToken.trim().isEmpty()) {
					if (rawFullCommand != null && rawFullCommand.contains("@")) {
						String[] parts = rawFullCommand.split("@", 2);
						if (parts.length == 2 && authToken.equals(parts[0])) {
							actualCommand = parts[1];
							authenticated = true;
						}
					}
					if (!authenticated) {
						System.err.println("TProfiler: InnerSocketThread authentication failed. Closing connection.");
						// Optionally send an error response to client before closing
						// child.getOutputStream().write("Authentication failed\r\n".getBytes());
						// child.getOutputStream().flush();
						child.close();
						continue; // Skip further processing for this connection
					}
				} else {
					// No token configured, or token is empty string, so command is considered authenticated
					authenticated = true; 
				}

				// IMPORTANT: Use 'actualCommand' for all subsequent command comparisons, not 'rawFullCommand'
				if (Manager.START.equals(actualCommand)) {
					Manager.instance().setSwitchFlag(true);
				} else if (Manager.STATUS.equals(actualCommand)) {
					write(child.getOutputStream());
				} else if (Manager.FLUSHMETHOD.equals(actualCommand)) {
					MethodCache.flushMethodData();
				} else {
					Manager.instance().setSwitchFlag(false);
				}
				child.close();
			}
		} catch (SocketException e) {
			// SocketException can occur if the socket is closed abruptly,
            // or if there are network issues.
			System.err.println("TProfiler: InnerSocketThread SocketException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			// This will catch the "Command exceeded maximum length" IOException
            // as well as other general IO errors.
			System.err.println("TProfiler: InnerSocketThread IOException: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 读取输入流
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private String read(InputStream in) throws IOException {
		BufferedInputStream bin = new BufferedInputStream(in);
		StringBuffer sb = new StringBuffer();
        int i;
		while ((i = bin.read()) != -1) {
			char c = (char) i;
			if (c == '\r') {
				break;
			} else {
				if (sb.length() >= MAX_COMMAND_LENGTH) {
					throw new IOException("Command exceeded maximum length of " + MAX_COMMAND_LENGTH + " bytes.");
				}
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 输出状态
	 * 
	 * @param os
	 * @throws IOException
	 */
	private void write(OutputStream os) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(os);
		if (Manager.instance().getSwitchFlag()) {
			out.write("running".getBytes());
		} else {
			out.write("stop".getBytes());
		}
		out.write('\r');
		out.flush();
	}

    /**
     * 调试使用
     *
     * @param args
     */
    public static void main(String[] args){
        InnerSocketThread socketThread = new InnerSocketThread();
        socketThread.setName("TProfiler-InnerSocket-Debug");
        socketThread.start();
    }
}
