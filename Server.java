import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.net.ServerSocket;
class Server
{
	private static ServerSocket ssocket = null;
	private static Socket csocket = null;

	public static ArrayList<clientThread> clients = new ArrayList<clientThread>();
	public static void main(String args[])
	{
		int portnum = 1234;
		if (args.length < 1)
		{
			System.out.println("No port is specified by the user ");
			System.out.println("Server is running on default port number=" + portnum);
			System.out.println("\nPrivate chat -between two clients cannot visible outside---for private chat use @");
			System.out.println("\nBlock chat -----for block chat use !");
			System.out.println("\nGroupChat ----can be visible to all users in the Group chat");
		}
		else
		{
			portnum = Integer.valueOf(args[0]).intValue();
			System.out.println("Server is running on user specified port number=" + portnum);
		}
		try
		{
			ssocket = new ServerSocket(portnum);
		}catch (IOException e)
		{
			System.out.println("Server Socket cannot be created");
		}
		int clientno = 1;
		while (true)
		{
			try
			{
				csocket = ssocket.accept();
				clientThread ct = new clientThread(csocket, clients);
				clients.add(ct);
				ct.start();
				System.out.println("Client " + clientno + " is connected!");
				clientno++;
			}catch (IOException e)
			{
				System.out.println("Client can not be connected");
			}
		}
	}
}
class clientThread extends Thread
{

	private String clientname = null;
	private ObjectInputStream is = null;
	private ObjectOutputStream os = null;
	private Socket csocket = null;
	private final ArrayList<clientThread> clients;
	public clientThread(Socket csocket, ArrayList<clientThread> clients) {
	this.csocket = csocket;
	this.clients = clients;
	}
	public void run()
	{
		ArrayList<clientThread> clients = this.clients;
		try
		{
			is = new ObjectInputStream(csocket.getInputStream());
			os = new ObjectOutputStream(csocket.getOutputStream());
			String Name;
			while (true)
			{
				synchronized(this)
				{
					this.os.writeObject("Please enter your Name :");
					this.os.flush();
					Name = ((String) this.is.readObject()).trim();
					if ((Name.indexOf('@') == -1) || (Name.indexOf('!') == -1))
					{
						break;
					}
					else
					{
						this.os.writeObject("UserName should not contain '@' or '!' characters.please reenter again");
						this.os.flush();
					}
				}
			}
			System.out.println("Client Name is " + Name);
			this.os.writeObject("*** Welcome " + Name + " to the CHAT ROOM***");
			this.os.writeObject("\nEnter quit to leave the chat room");
			this.os.flush();
			this.os.writeObject("Directory Created");
	
			this.os.flush();
			synchronized(this)
			{
				for (clientThread ct : clients)
				{
					if (ct != null && ct == this)
					{
						clientname = "@" + Name;
						break;
					}
				}
				for (clientThread ct : clients)
				{
					if (ct != null && ct != this)
					{
						ct.os.writeObject("Client "+Name + " has joined");
						ct.os.flush();
					}
				}
			}
			while (true)
			{
				this.os.writeObject("Please Enter command to sent in the Group or Private:");
				this.os.flush();
				String line = (String) is.readObject();
				if (line.startsWith("quit"))
				{
					break;
				}
				if (line.startsWith("@"))
				{
					privatechat(line,Name);
				}
				else if(line.startsWith("!"))
				{
					blockchat(line,Name);
				}
				else
				{
					groupchat(line,Name);
				}
			}
			this.os.writeObject("*** Bye " + Name + " ***");
			this.os.flush();
			System.out.println(Name + " disconnected.");
			clients.remove(this);
			synchronized(this)
			{
				if (!clients.isEmpty())
				{
					for (clientThread ct : clients)
					{
						if (ct != null && ct != this && ct.clientname != null)
						{
							ct.os.writeObject("*** The user " + Name + " disconnected ***");
							ct.os.flush();
						}
					}
				}
			}
			this.is.close();
			this.os.close();
			csocket.close();
		}
		catch (IOException e)
		{
			System.out.println("User Session terminated");
		}
		catch (ClassNotFoundException e)
		{
			System.out.println("Class Not Found");
		}
	}
	void blockchat(String line, String Name) throws IOException, ClassNotFoundException
	{
		String[] words = line.split(":", 2);
		if (words[1].split(" ")[0].toLowerCase().equals("sendfile"))
		{
			byte[] file_data = (byte[]) is.readObject();

			synchronized(this)
			{
				for (clientThread ct : clients)
				{
					if (ct != null && ct != this && ct.clientname != null&& !ct.clientname.equals("@"+words[0].substring(1)))
					{
						ct.os.writeObject("Sending_File:"+words[1].split(" ",2)[1].substring(words[1].split("\\s",2)[1].lastIndexOf(File.separator)+1));
						ct.os.writeObject(file_data);
						ct.os.flush();
					}
				}
				this.os.writeObject(">>chat File sent to everyone except "+words[0].substring(1));
				this.os.flush();
				System.out.println("In CHAT *****File sent by "+ this.clientname.substring(1) + " to everyone except " +words[0].substring(1)+"****");
			}
		}
		else
		{
			if (words.length > 1 && words[1] != null)
			{
				words[1] = words[1].trim();
				if (!words[1].isEmpty())
				{
					synchronized (this)
					{
						for (clientThread ct : clients)
						{
							if (ct != null && ct != this && ct.clientname != null && !ct.clientname.equals("@"+words[0].substring(1)))
							{
								ct.os.writeObject("<" + Name + "> " + words[1]);
								ct.os.flush();
							}
						}
						this.os.writeObject(">>*****Chat message sent to everyone except "+words[0].substring(1)+"****");
						this.os.flush();
						System.out.println("Message sent by "+ this.clientname.substring(1) + " to everyone except " + words[0].substring(1));
					}
				}
			}
		}
	}

	void groupchat(String line, String Name) throws IOException, ClassNotFoundException
	{
		if (line.split("\\s")[0].toLowerCase().equals("sendfile"))
		{
			byte[] file_data = (byte[]) is.readObject();
			synchronized(this)
			{
				for (clientThread ct : clients)
				{
					if (ct != null && ct.clientname != null && ct.clientname!= this.clientname)
					{
						ct.os.writeObject("Sending_File:"+line.split("\\s",2)[1].substring(line.split("\\s",2)[1].lastIndexOf(File.separator)+1));
						ct.os.writeObject(file_data);
						ct.os.flush();
					}
				}
				this.os.writeObject("IN GROUP file sent successfully");
				this.os.flush();
				System.out.println("In Groupchat file sent by " + this.clientname.substring(1));
			}
		}
		else
		{
			synchronized(this)
			{
				for (clientThread ct : clients)
				{
					if (ct != null && ct.clientname != null && ct.clientname!=this.clientname)
					{
						ct.os.writeObject("<" + Name + "> " + line);
						ct.os.flush();
					}
				}
				this.os.writeObject("********In Groupchat message sent successfully******");
				this.os.flush();
				System.out.println("IN Groupchat message sent by " + this.clientname.substring(1));
			}
		}

	}
	void privatechat(String line, String Name) throws IOException, ClassNotFoundException
	{
		String[] words = line.split(":", 2);
		if (words[1].split(" ")[0].toLowerCase().equals("sendfile"))
		{
			byte[] file_data = (byte[]) is.readObject();
			for (clientThread ct : clients)
			{
				if (ct != null && ct != this && ct.clientname != null && ct.clientname.equals(words[0]))
				{
					ct.os.writeObject("Sending_File:"+words[1].split(" ",2)[1].substring(words[1].split("\\s",2)[1].lastIndexOf(File.separator)+1));
					ct.os.writeObject(file_data);
					ct.os.flush();
					System.out.println(this.clientname.substring(1) + " transferred a private file to client "+ct.clientname.substring(1));
					this.os.writeObject("******Private File sent to " + ct.clientname.substring(1)+"*****");
					this.os.flush();
					break;
				}
			}
		}
		else
		{
			if (words.length > 1 && words[1] != null)
			{
				words[1] = words[1].trim();
				if (!words[1].isEmpty())
				{
					for (clientThread ct : clients)
					{
						if (ct != null && ct != this && ct.clientname != null&& ct.clientname.equals(words[0]))
						{
							ct.os.writeObject("<" + Name + "> " + words[1]);
							ct.os.flush();
							System.out.println(this.clientname.substring(1) + " transferred a private message to client "+ ct.clientname.substring(1));
							this.os.writeObject("*****Private Message sent to " + ct.clientname.substring(1)+"*****");
							this.os.flush();
							break;
						}
					}
				}
			}
		}
	}
}
