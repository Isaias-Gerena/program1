import java.net.Socket;
import java.nio.file.Files;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.util.Scanner;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

	private Socket socket;
	private File workable; // stores the desired file
	private String fileType = "";
	private boolean fileFound;
	private boolean fileCreated;
	// I use fileFound as a boolean to decide whether or not the "My server
	// works!" header will be presented
	// fileCreated is used to decide whether or not the body will be read from a
	// file
	// If fileFound is false, the 404 error is thrown. If fileFound is true and
	// fileCreated is true,
	// A file is read from to create the body. If fileFound is true and
	// fileCreated is false, only the header
	// "My server works!" appears.

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request
	 * and then returns, which destroys the thread. This method assumes that
	 * whoever created the worker created it with a valid open socket object.
	 **/
	public void run()
	{
		System.out.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			readHTTPRequest(is);
			writeHTTPHeader(os, fileType);
			writeContent(os);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.out.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private void readHTTPRequest(InputStream is)
	{
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));

		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				System.out.println("Request line: (" + line + ")");

				if (line.contains("GET") && (line.length() > 14))
				{
					fileFound = false;
					workable = new File(line.substring(5, line.length() - 9));
					if (workable.exists())
						fileFound = true;
					fileCreated = true;
					if (line.contains(".txt") | line.contains(".html"))
						fileType = "text/html";
					else if (line.contains(".png"))
						fileType = "image/png";
					else if (line.contains(".jpg") || line.contains(".jpeg"))
						fileType = "image/jpeg";
					else if (line.contains(".gif"))
						fileType = "image/gif";
					System.out.println(fileType);
				}
				if (line.contains("GET") && (line.length() == 14))
				{
					fileFound = true;
					fileCreated = false;
				}
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *            is the OutputStream object to write to
	 * @param contentType
	 *            is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		if (fileFound)
			os.write("HTTP/1.1 200 OK\n".getBytes());
		else
			os.write("HTTP/1.1 404 Not Found\n".getBytes());
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55
		// GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be
	 * done after the HTTP header has been written out.
	 * 
	 * @param os
	 *            is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os) throws Exception
	{
		if (fileFound)
		{
			if (fileCreated)
			{
				Scanner sc = new Scanner(workable);
				if (fileType.equals("text/html"))
				{
					os.write("<html><head></head><body>\n".getBytes());
					os.write("<h3>My web server works!</h3>\n".getBytes());
					os.write("<p>".getBytes());
					while (sc.hasNextLine()) // runs each line through the
					// editString method
					{
						String temp = sc.nextLine();
						temp = editString(temp);
						os.write(temp.getBytes());
					}
					os.write("</p>".getBytes());
					os.write("</body></html>\n".getBytes());
					sc.close();
				}
				if(fileType.contains("image"))
				{
					byte picture[] = Files.readAllBytes(workable.toPath());
					for(int x = 0; x < picture.length; x++)
					{
						os.write(picture[x]);
					}
				}
			}
			else
			{
				os.write("<html><head></head><body>\n".getBytes());
				os.write("<h3>My web server works!</h3>\n".getBytes());
				os.write("<p>".getBytes());
			}
		}
		else
		{
			os.write("<html><head></head><body>\n".getBytes());
			os.write("404 Not Found\n".getBytes());
			os.write("</body></html>\n".getBytes());
		}
	}

	/*
	 * This method is used to parse through the line given and ensures the
	 * specific tags that need to be filtered are replaced with the proper text.
	 * The first while loop parses through the line and replaces all instances
	 * of <cs371server>, and the second while loop parses through the line and
	 * replaces all instances of <cs371date>. I used while loops just in case
	 * there was more than one instance of the same tag.
	 */
	public String editString(String initialString)
	{
		Scanner sc = new Scanner(initialString);
		String line = sc.nextLine();

		while (line.contains("<cs371server>"))
		{
			int index = line.indexOf("<cs371server>");
			line = (line.substring(0, index) + " Turtle Server! " + line.substring(index + 13));
		}
		while (line.contains("<cs371date>"))
		{
			Date d = new Date();
			DateFormat df = DateFormat.getDateTimeInstance();
			int index = line.indexOf("<cs371date>");
			line = (line.substring(0, index) + " " + df.format(d) + " " + line.substring(index + 11));
		}
		sc.close();
		return line;
	}

} // end class
