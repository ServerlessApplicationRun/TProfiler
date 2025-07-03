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
import java.util.concurrent.atomic.AtomicInteger;

import com.taobao.profile.Manager;
import com.taobao.profile.runtime.MethodCache;

/**
 * 对外提供Socket开关
 * 
 * @author shutong.dy
 * @since 2012-1-11
 */
public class InnerSocketThread extends Thread {
	/**
	 * server
	 */
	private ServerSocket socket;

	/**
	 * Maximum concurrent connections allowed
	 */
	private static final int MAX_CONCURRENT_CONNECTIONS = 10;
	
	/**
	 * Maximum command length to prevent DoS attacks
	 */
	private static final int MAX_COMMAND_LENGTH = 100;
	
	/**
	 * Current active connection count
	 */
	private static final AtomicInteger activeConnections = new AtomicInteger(0);

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			// Fix: Bind only to localhost for security
			socket = new ServerSocket(Manager.PORT, 50, InetAddress.getLoopbackAddress());
			while (true) {
				Socket child = null;
				try {
					child = socket.accept();

					// Fix: Implement connection limiting to prevent DoS
					if (activeConnections.get() >= MAX_CONCURRENT_CONNECTIONS) {
						child.close();
						continue;
					}

					activeConnections.incrementAndGet();
					
					// Fix: Set reasonable timeout and buffer limits
					child.setSoTimeout(5000);

					String command = read(child.getInputStream());

					if (Manager.START.equals(command)) {
						Manager.instance().setSwitchFlag(true);
					} else if (Manager.STATUS.equals(command)) {
						write(child.getOutputStream());
					} else if (Manager.FLUSHMETHOD.equals(command)) {
						MethodCache.flushMethodData();
					} else {
						Manager.instance().setSwitchFlag(false);
					}
				} catch (SocketException e) {
					// Expected when socket is closed, don't log as error
					if (!socket.isClosed()) {
						e.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					// Fix: Ensure client socket is always closed
					if (child != null) {
						try {
							child.close();
						} catch (IOException e) {
							// Log but don't rethrow
							e.printStackTrace();
						} finally {
							activeConnections.decrementAndGet();
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Fix: Ensure server socket is always closed
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
		StringBuilder sb = new StringBuilder();
        int i;
		int bytesRead = 0;
		
		// Fix: Add length limit to prevent DoS attacks
		while ((i = bin.read()) != -1) {
			if (++bytesRead > MAX_COMMAND_LENGTH) {
				throw new IOException("Command too long, potential DoS attack");
			}
			
			char c = (char) i;
			if (c == '\r') {
				break;
			} else {
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
		try {
			if (Manager.instance().getSwitchFlag()) {
				out.write("running".getBytes());
			} else {
				out.write("stop".getBytes());
			}
			out.write('\r');
			out.flush();
		} finally {
			// Fix: Ensure output stream is properly closed
			try {
				out.close();
			} catch (IOException e) {
				// Log but don't rethrow
				e.printStackTrace();
			}
		}
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
