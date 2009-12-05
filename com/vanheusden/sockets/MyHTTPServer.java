/* Released under GPL 2.0
 * (C) 2009 by folkert@vanheusden.com
 */
package com.vanheusden.sockets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyHTTPServer
{
	private Socket socket;
	private BufferedReader inputStream;
	private BufferedWriter outputStream;
	private ServerSocket serverSocket = null;

	public MyHTTPServer(String adapter, int port) throws Exception
	{
		serverSocket = new ServerSocket();
		System.out.println("Binding to " + adapter + ":" + port);
		serverSocket.bind(new InetSocketAddress(adapter, port));
	}

	public List<HTTPRequestData> acceptConnectionGetRequest() throws Exception
	{
		socket = serverSocket.accept();

		socket.setKeepAlive(true);

		inputStream  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		inputStream.mark(0);

		List<HTTPRequestData> request = new ArrayList<HTTPRequestData>();
		for(;;)
		{
			// FIXME check for NULL
			String line = inputStream.readLine();

			if (line == null || line.equals(""))
				break;

			int space = line.indexOf(" ");
			if (space == -1)
				request.add(new HTTPRequestData(line, null));
			else
				request.add(new HTTPRequestData(line.substring(0, space), line.substring(space).trim()));
		}

		return request;
	}

	public InetSocketAddress getRemoteSocketAddress()
	{
		return (InetSocketAddress)socket.getRemoteSocketAddress();
	}

	public InputStream getInputStream() throws Exception
	{
		return socket.getInputStream();
	}

	public OutputStream getOutputStream() throws Exception
	{
		return socket.getOutputStream();
	}

	public void close() throws Exception
	{
		outputStream.flush();

		if (socket != null)
			socket.close();
		socket = null;
	}

	public HTTPRequestData findRecord(List<HTTPRequestData> records, String what)
	{
		for(HTTPRequestData record : records)
		{
			if (record.getName().equals(what))
				return record;
		}

		return null;
	}

	public List<HTTPRequestData> getRequestData(List<HTTPRequestData> request) throws Exception
	{
		int nBytes = -1;

		for(HTTPRequestData line : request)
		{
			if (line.getName().equals("Content-Length:"))
			{
				if (line.getData() == null)
					continue;

				String par = line.getData().trim();
				nBytes = Integer.valueOf(par);
				break;
			}
		}

		if (nBytes == -1)
			return null;

		char [] data = new char[nBytes];
		int nRead = inputStream.read(data, 0, nBytes);
		String dataStr = new String(data, 0, nRead);

		System.out.println("request data: " + dataStr);

		List<HTTPRequestData> requestPairs = new ArrayList<HTTPRequestData>();
		String [] pairs = dataStr.split("&");
		if (pairs.length == 0)
			return null;
		for(String pair : pairs)
		{
			int is = pair.indexOf("=");

			if (is == -1)
				requestPairs.add(new HTTPRequestData(pair, null));
			else
				requestPairs.add(new HTTPRequestData(pair.substring(0, is), pair.substring(is + 1)));
		}

		return requestPairs;
	}

	public void sendReply(List<String> reply) throws Exception
	{
		for(String currentLine : reply)
			outputStream.write(currentLine, 0, currentLine.length());

		outputStream.flush();

		close();
	}
}
