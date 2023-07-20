import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
public class Client implements Runnable
{
	private static Socket csocket = null;
	private static ObjectOutputStream os = null;
	private static ObjectInputStream is = null;
	private static BufferedReader inputtext = null;
	private static BufferedInputStream bis = null;
	private static boolean closed = false;
	
	public static void main(String[] args)
	{
		int portnum =1234;
		String host = "localhost";
		String s="";
		if (args.length < 2)
		{

			System.out.println("Default Server: " + host + ", Default Port: " + portnum);
		}
		else
		{
			host = args[0];
			portnum = Integer.valueOf(args[1]).intValue();
			System.out.println("Server: " + host + ", Port: " + portnum);
		}
		try
		{
			csocket = new Socket(host, portnum);
			inputtext = new BufferedReader(new InputStreamReader(System.in));
			os = new ObjectOutputStream(csocket.getOutputStream());
			is = new ObjectInputStream(csocket.getInputStream());
		}catch (UnknownHostException e)
		{
			System.err.println("Unknown " + host);
		}
		catch (IOException e)
		{
			System.err.println("No Server found. Please ensure that the Server program is running and restart it again.");
		}
		if (csocket != null && os != null && is != null)
		{
			try
			{
				new Thread(new Client()).start();
				while (!closed)
				{
					String msg = (String) inputtext.readLine().trim();
					if ((msg.split(":").length > 1))
					{
						if (msg.split(":")[1].toLowerCase().startsWith("sendfile"))
						{
							File files = new File((msg.split(":")[1]).split(" ",2)[1]);
							if (!files.exists())
							{
								System.out.println("File Doesn't exist!!");
								continue;
							}
							byte [] bytearray = new byte [(int)files.length()];
							FileInputStream fis = new FileInputStream(files);
							bis = new BufferedInputStream(fis);
						while (bis.read(bytearray,0,bytearray.length)>=0)
						{
							bis.read(bytearray,0,bytearray.length);
							s=new String(bytearray);
							//System.out.println(s);
						}
						StringBuffer r=new StringBuffer();
						for(int i=0;i<s.length();i++)
						{
							if(Character.isUpperCase(s.charAt(i)))
							{
								 char c=(char)(((int)s.charAt(i)+3-65)%26+65);
								r.append(c);
							}
							else
							{
					 			char c=(char)(((int)s.charAt(i)+3-97)%26+97);
								r.append(c);
							}
						}
	

						for(int i=0;i<s.length();i++)
						{
							if(Character.isUpperCase(s.charAt(i)))
							{
								 char c=(char)(((int)s.charAt(i)-3+65)%26+65);
								r.append(c);
							}
							else
							{
								 char c=(char)(((int)s.charAt(i)-3+97)%26+97);
								r.append(c);
							}
						}	
			
						//System.out.println(r);
						os.writeObject(msg);
						os.writeObject(bytearray);
						os.flush();
					}
					else
					{
						os.writeObject(msg);
						os.flush();
					}
				}
				else if (msg.toLowerCase().startsWith("sendfile"))
				{
					File files = new File(msg.split(" ",2)[1]);
					if (!files.exists())
					{
						System.out.println("File Doesn't exist!!");
						continue;
					}
					byte [] bytearray = new byte [(int)files.length()];
					FileInputStream fis = new FileInputStream(files);
					bis = new BufferedInputStream(fis);
					while (bis.read(bytearray,0,bytearray.length)>=0)
					{
						bis.read(bytearray,0,bytearray.length);
						s=new String(bytearray);
						//System.out.println(s);
					}
					StringBuffer r=new StringBuffer();
					for(int i=0;i<s.length();i++)
					{
						if(Character.isUpperCase(s.charAt(i)))
						{
							 char c=(char)(((int)s.charAt(i)+3-65)%26+65);
							r.append(c);
						}
						else
						{
							 char c=(char)(((int)s.charAt(i)+3-97)%26+97);
							r.append(c);
						}
					}
					for(int i=0;i<s.length();i++)
					{
						if(Character.isUpperCase(s.charAt(i)))
						{
							 char c=(char)(((int)s.charAt(i)-3+65)%26+65);
							r.append(c);
						}
						else
						{
							 char c=(char)(((int)s.charAt(i)-3+97)%26+97);
							r.append(c);
						}
					}
			
					//System.out.println(r);
					os.writeObject(msg);
					os.writeObject(bytearray);
					os.flush();
				}
				else
				{
					os.writeObject(msg);
					os.flush();
				}
			}
			os.close();
			is.close();
			csocket.close();
			} catch (IOException e)
			{
				System.err.println("IOException: " + e);
			}	
		}
	}
	
	public void run()
	{
		String responseLine;
		String filename = null;
		byte[] ipfile = null;
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		File directory_name = null;
		String full_path;
		String dir_name = "Received_Files";
		try
		{
			while ((responseLine = (String) is.readObject()) != null)
			{
				if (responseLine.equals("Directory Created"))
				{
					directory_name = new File((String) dir_name);
					if (!directory_name.exists())
					{
						directory_name.mkdir();
						System.out.println("New Receiving file directory for this client created!!");
					}
					else
					{
						System.out.println("Receiving file directory for this client already exists!!");
					}
				}
				else if (responseLine.startsWith("Sending_File"))
				{
					try
					{

						filename = responseLine.split(":")[1];
						full_path = directory_name.getAbsolutePath()+"/"+filename;
						ipfile = (byte[]) is.readObject();
						fos = new FileOutputStream(full_path);
						bos = new BufferedOutputStream(fos);
						bos.write(ipfile);
						bos.flush();
						System.out.println("File Received.");
					}
					finally
					{
						if (fos != null) fos.close();
						if (bos != null) bos.close();
					}
				}
				else
				{
					System.out.println(responseLine);
				}
				if (responseLine.indexOf("*** Bye") != -1)
					break;
				}
				closed = true;
				System.exit(0);
			}catch (IOException | ClassNotFoundException e)
			{
				System.err.println("Server Process Stopped Unexpectedly!!");
			}
		}
	}
