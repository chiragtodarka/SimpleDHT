package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
//import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
	
	static String predecessor;
	static String portStr;
	static String successor;
	static String nodeId;
	
	static SimpleDhtDatabase simpleDhtDatabase;
	
	static boolean gdumpReceived;
	static Message gdumpMessage;
	
	static boolean queryResponseReceived;
	static Message queryResponseMessage;
	
    @Override
    public boolean onCreate() 
    {
    	simpleDhtDatabase = new SimpleDhtDatabase(getContext());
    	
    	TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        SimpleDhtProvider.portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        //Log.v("portStr", SimpleDhtProvider.portStr);
        
        try 
    	{
    		SimpleDhtProvider.nodeId = genHash(SimpleDhtProvider.portStr);
		} 
    	catch (NoSuchAlgorithmException e) 
    	{
			//Log.e("Error in genHash: ", e.getMessage());
		}
        
        if(SimpleDhtProvider.portStr.equals("5554"))
		{
			SimpleDhtProvider.predecessor = "5554";
			SimpleDhtProvider.successor = "5554";
			new MyServer().executeOnExecutor(MyServer.THREAD_POOL_EXECUTOR, (Void)null);
			
			//Log.v("SimpleDhtProvider.successor: ", SimpleDhtProvider.successor);
			//Log.v("SimpleDhtProvider.predecessor: ", SimpleDhtProvider.predecessor);
		}
		else
		{
			SimpleDhtProvider.predecessor = "";
			SimpleDhtProvider.successor = "";
						
			new MyServer().executeOnExecutor(MyServer.THREAD_POOL_EXECUTOR, (Void)null);
			
			Message message = new Message();
			message.messageType = "join";
			message.originalSender = SimpleDhtProvider.portStr;
			message.messageContent = "";
			message.receiver = "5554";
			new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
		}
    	
    	return true;
    }


	@Override
    public Uri insert(Uri uri, ContentValues values) 
	{
		//sqlDB.insert(TABLE_NAME, null, values);
		String key = (String) values.get("key");
		String value = (String) values.get("value");
		
		Message message = new Message();
		message.messageType = "insert";
		message.hop = 0;
		message.messageContent = key+":"+value;
		message.originalSender = SimpleDhtProvider.portStr;
		
		SimpleDhtProvider.insertMessage(message);
		
		return uri;
    }
	
	public static void insertMessage(Message message)
	{
		String[] messageComponent = message.messageContent.split(":");
		String key = messageComponent[0];
		String value = messageComponent[1];
		
		try
		{
			/*	Generating all 50 keys
			 * if((message.hop==2) || ((Hash.genHash(key).compareTo(SimpleDhtProvider.nodeId) <= 0) && (Hash.genHash(key).compareTo(Hash.genHash(SimpleDhtProvider.predecessor))>0 )))
			{
				SQLiteDatabase sqlDB = simpleDhtDatabase.getWritableDatabase();
				ContentValues contentValues = new ContentValues();
				contentValues.put("key", key);
				contentValues.put("value", value);
				sqlDB.insertWithOnConflict(SimpleDhtDatabase.TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
				Log.v("key Added: ", key);
			}
			else if (Hash.genHash(key).compareTo(SimpleDhtProvider.nodeId) > 0 )
			{
				message.receiver = SimpleDhtProvider.successor;
				message.hop++;
				new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
				Log.v("Key send to successor "+SimpleDhtProvider.successor, key);
			}
			else if (Hash.genHash(key).compareTo(Hash.genHash(SimpleDhtProvider.predecessor)) <=0 )
			{
				message.receiver = SimpleDhtProvider.predecessor;
				message.hop++;
				new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
				Log.v("Key send to predesor "+SimpleDhtProvider.predecessor, key);
			}*/
			
			boolean flag1 = (Hash.genHash(SimpleDhtProvider.predecessor).compareTo(SimpleDhtProvider.nodeId)>0) && (Hash.genHash(key).compareTo(Hash.genHash(SimpleDhtProvider.predecessor))>0);
			boolean flag2 = (Hash.genHash(key).compareTo(SimpleDhtProvider.nodeId) <= 0) && (Hash.genHash(key).compareTo(Hash.genHash(SimpleDhtProvider.predecessor))>0 );
			boolean flag3 = (Hash.genHash(key).compareTo(SimpleDhtProvider.nodeId) <= 0) && (Hash.genHash(SimpleDhtProvider.predecessor).compareTo(SimpleDhtProvider.nodeId)>0);
			boolean flag4 = ((SimpleDhtProvider.portStr.equalsIgnoreCase(SimpleDhtProvider.predecessor)) && (SimpleDhtProvider.portStr.equalsIgnoreCase(SimpleDhtProvider.successor)));
			
			if(flag1 || flag2 || flag3 || flag4)
			{
				SQLiteDatabase sqlDB = simpleDhtDatabase.getWritableDatabase();
				ContentValues contentValues = new ContentValues();
				contentValues.put("key", key);
				contentValues.put("value", value);
				sqlDB.insertWithOnConflict(SimpleDhtDatabase.TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
				//Log.v("key Added: ", key);
			}
			else
			{
				message.receiver = SimpleDhtProvider.successor;
				message.hop++;
				new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
				//Log.v("Key send to successor "+SimpleDhtProvider.successor, key);
			}
		}
		catch(NoSuchAlgorithmException e)
		{
			//Log.e("Error in insert: ", e.getMessage());
		}
	}
	
	/*public static void insertMessage(Message message)
	{
		SQLiteDatabase sqlDB = simpleDhtDatabase.getWritableDatabase();
		String[] messageComponent = message.messageContent.split(":");
		String key = messageComponent[0];
		String value = messageComponent[1];
		
		try
		{
			//boolean isLastNode = SimpleDhtProvider.nodeId.compareTo(Hash.genHash(SimpleDhtProvider.successor)) > 0;
			if(message.originalSender.equalsIgnoreCase(SimpleDhtProvider.portStr))
			{
				Log.v("Key generated by me came again:", key);
				ContentValues contentValues = new ContentValues();
				contentValues.put("key", key);
				contentValues.put("value", value);
				sqlDB.insertWithOnConflict(SimpleDhtDatabase.TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
			}
			else if((Hash.genHash(key).compareTo(SimpleDhtProvider.nodeId) <= 0) && (Hash.genHash(key).compareTo(Hash.genHash(SimpleDhtProvider.predecessor))>0 ))
			{
				ContentValues contentValues = new ContentValues();
				contentValues.put("key", key);
				contentValues.put("value", value);
				sqlDB.insertWithOnConflict(SimpleDhtDatabase.TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
				Log.v("Key Added: ", key);
			}
			else if (Hash.genHash(key).compareTo(SimpleDhtProvider.nodeId) > 0)
			{
				message.receiver = SimpleDhtProvider.successor;
				new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
				Log.v("Key fwd to successor " + SimpleDhtProvider.successor, key);
			}
			else if (Hash.genHash(key).compareTo(Hash.genHash(SimpleDhtProvider.predecessor))<=0) 
			{
				message.receiver = SimpleDhtProvider.predecessor;
				new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
				Log.v("Key fwd to predesor " + SimpleDhtProvider.predecessor, key);
			}
		}
		catch(NoSuchAlgorithmException e)
		{
			Log.e("Error in insert: ", e.getMessage());
		}
		
	}*/

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
    {
    	SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(SimpleDhtDatabase.TABLE_NAME);
		SQLiteDatabase db = simpleDhtDatabase.getReadableDatabase();
		Cursor cursor;
		if(selection.equals("all"))
		{
			cursor = queryBuilder.query(db, null, null, null, null, null, null);
		}
		else
		{
			cursor = queryBuilder.query(db, null, SimpleDhtDatabase.TABLE_NAME+"."+SimpleDhtDatabase.COLUMN_KEY+"='"+selection+"'", null, null, null, null);
			if(cursor==null | cursor.getCount() ==0)
			{
				queryResponseReceived = false;
				queryResponseMessage = null;
				Message message = new Message();
				message.messageType = "query";
				message.messageContent = selection;
				message.originalSender = SimpleDhtProvider.portStr;
				message.receiver = SimpleDhtProvider.successor;
				new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
				while(true)
				{
					if(queryResponseReceived == true && queryResponseMessage!=null)
						break;
				}
				String[] messageComponent = queryResponseMessage.messageContent.split(":");
				MatrixCursor newCursor= new MatrixCursor(new String[]{"key","value"});
				newCursor.addRow(new String[]{messageComponent[0],messageComponent[1]});
				cursor = newCursor;
			}
		}
	    cursor.setNotificationUri(getContext().getContentResolver(), uri);
	    return cursor;
    }
    
    public static void findKey(Message message)
    {
    	if(message.originalSender.equals(SimpleDhtProvider.portStr))
    	{
    		//Log.e("Error","Key not found");
    		return;
    	}
    	SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(SimpleDhtDatabase.TABLE_NAME);
		SQLiteDatabase db = simpleDhtDatabase.getReadableDatabase();
		Cursor cursor = queryBuilder.query(db, null, SimpleDhtDatabase.TABLE_NAME+"."+SimpleDhtDatabase.COLUMN_KEY+"='"+message.messageContent+"'", null, null, null, null);
		if(cursor==null | cursor.getCount() ==0)
		{
			message.receiver = SimpleDhtProvider.successor;
			new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
		}
		else
		{
			cursor.moveToFirst();

			if (!(cursor.isFirst() && cursor.isLast())) {
				//Log.e("Error in findKey()", "Wrong number of rows");
			}
			
			int keyIndex = cursor.getColumnIndex("key");
			int valueIndex = cursor.getColumnIndex("value");
			String returnKey = cursor.getString(keyIndex);
			String returnValue = cursor.getString(valueIndex);
			
			message.messageContent = returnKey+":"+returnValue;
			message.messageType = "queryResponse";
			message.receiver = message.originalSender;
			new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
		}
		cursor.close();
		
    }
    
    public static String gdumpQuery()
    {
    	SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(SimpleDhtDatabase.TABLE_NAME);
		SQLiteDatabase db = simpleDhtDatabase.getReadableDatabase();
		Cursor resultCursor = queryBuilder.query(db, null, null, null, null, null, null);
		String data = "";
				
		if(resultCursor!=null & resultCursor.getCount() != 0)
		{
			resultCursor.moveToFirst();
			
			while(true)
			{
				int keyIndex = resultCursor.getColumnIndex("key");
				int valueIndex = resultCursor.getColumnIndex("value");
				if (keyIndex == -1 || valueIndex == -1) 
				{
					//Log.e("Error in LDump: ", "Wrong columns");
					resultCursor.close();
				}
				
				String returnKey = resultCursor.getString(keyIndex);
				String returnValue = resultCursor.getString(valueIndex);
				
				data = data + returnKey + returnValue + "\n";
				
				if(resultCursor.isLast())
				{
					break;
				}
				else
				{
					resultCursor.moveToNext();
				}
			}
			
			resultCursor.close();
		}
		
		return data;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }
    
	@Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }


}


class MyServer extends AsyncTask<Void, Void, Void> 
{
	@Override
	protected Void doInBackground(Void... arg0) 
	{
		try
		{
			ServerSocket serverSocket = new ServerSocket(10000);
			Socket listener;
			while(true)
			{
				listener = serverSocket.accept();
				ObjectInputStream ois = new ObjectInputStream(listener.getInputStream());
				Message message = (Message)ois.readObject();
				
				if(SimpleDhtProvider.portStr.equals("5554") && message.messageType.equalsIgnoreCase("join"))
				{
					if(SimpleDhtProvider.successor.equalsIgnoreCase(SimpleDhtProvider.portStr) && SimpleDhtProvider.predecessor.equalsIgnoreCase(SimpleDhtProvider.portStr))
					{
						Message messageToSend = new Message();
						messageToSend.messageType = "setNeighbour";
						messageToSend.originalSender = SimpleDhtProvider.portStr;
						messageToSend.messageContent = SimpleDhtProvider.portStr+":"+SimpleDhtProvider.portStr;
						messageToSend.receiver = message.originalSender;
						new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, messageToSend);
						
						SimpleDhtProvider.predecessor = message.originalSender;
						SimpleDhtProvider.successor = message.originalSender;
						
						//Log.v("SimpleDhtProvider.successor: ", SimpleDhtProvider.successor);
						//Log.v("SimpleDhtProvider.predecessor: ", SimpleDhtProvider.predecessor);
					}
					else
					{
						String messageNodeId = Hash.genHash(message.originalSender);
						if(SimpleDhtProvider.nodeId.compareTo(messageNodeId) < 0 )
						{
							Message messageToSend1 = new Message();
							messageToSend1.messageType = "setNeighbour";
							messageToSend1.originalSender = SimpleDhtProvider.portStr;
							messageToSend1.messageContent = SimpleDhtProvider.successor+":"+SimpleDhtProvider.portStr;
							messageToSend1.receiver = message.originalSender;
							new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, messageToSend1);
							
							Message messageToSend2 = new Message();
							messageToSend2.messageType = "setNeighbour";
							messageToSend2.originalSender = SimpleDhtProvider.portStr;
							messageToSend2.messageContent = SimpleDhtProvider.portStr+":"+message.originalSender;
							messageToSend2.receiver = SimpleDhtProvider.predecessor;
							new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, messageToSend2);
							
							SimpleDhtProvider.successor = message.originalSender;
							//Log.v("SimpleDhtProvider.successor: ", SimpleDhtProvider.successor);
							//Log.v("SimpleDhtProvider.predecessor: ", SimpleDhtProvider.predecessor);
						}
						
						else if(SimpleDhtProvider.nodeId.compareTo(messageNodeId) > 0)
						{
							Message messageToSend3 = new Message();
							messageToSend3.messageType = "setNeighbour";
							messageToSend3.originalSender = SimpleDhtProvider.portStr;
							messageToSend3.messageContent = SimpleDhtProvider.portStr+":"+SimpleDhtProvider.successor;
							messageToSend3.receiver = message.originalSender;
							new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, messageToSend3);
							
							Message messageToSend4 = new Message();
							messageToSend4.messageType = "setNeighbour";
							messageToSend4.originalSender = SimpleDhtProvider.portStr;
							messageToSend4.messageContent = message.originalSender+":"+SimpleDhtProvider.portStr;
							messageToSend4.receiver = SimpleDhtProvider.successor;
							new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, messageToSend4);
							
							SimpleDhtProvider.predecessor = message.originalSender;
							//Log.v("SimpleDhtProvider.successor: ", SimpleDhtProvider.successor);
							//Log.v("SimpleDhtProvider.predecessor: ", SimpleDhtProvider.predecessor);
						}
					}
				}
				else if(message.messageType.equalsIgnoreCase("setNeighbour"))
				{
					String[] messageComponent = message.messageContent.split(":");
					SimpleDhtProvider.successor = messageComponent[0];
					SimpleDhtProvider.predecessor = messageComponent[1];
					//Log.v("SimpleDhtProvider.successor: ", SimpleDhtProvider.successor);
					//Log.v("SimpleDhtProvider.predecessor: ", SimpleDhtProvider.predecessor);
				}
				else if(message.messageType.equalsIgnoreCase("insert"))
				{
					SimpleDhtProvider.insertMessage(message);
				}
				else if(message.messageType.equalsIgnoreCase("gdump"))
				{
					if(message.originalSender.equalsIgnoreCase(SimpleDhtProvider.portStr))
					{
						SimpleDhtProvider.gdumpMessage = message;
						SimpleDhtProvider.gdumpReceived = true;
					}
					else
					{
						String data = SimpleDhtProvider.gdumpQuery();
						message.messageContent = message.messageContent + data;
						message.receiver = SimpleDhtProvider.successor;
						new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
					}
				}
				else if (message.messageType.equalsIgnoreCase("query"))
				{
					SimpleDhtProvider.findKey(message);
				}
				else if (message.messageType.equalsIgnoreCase("queryResponse"))
				{
					SimpleDhtProvider.queryResponseMessage = message;
					SimpleDhtProvider.queryResponseReceived = true;
				}
			}
		}
		catch (Exception e) 
		{
			//Log.e("Error in Server:" ,e.getMessage());
		}
		return null;
	}
	
}


class MyClient extends AsyncTask<Message, Void, Void>
{
	static String tcpString = "10.0.2.2";
	
	@Override
	synchronized protected Void doInBackground(Message... messageArray) 
	{
		try
		{
			int receiverPort = 0;
			
			if(messageArray[0].receiver.equals("5554"))
				receiverPort = 11108;
			else if (messageArray[0].receiver.equals("5556"))
				receiverPort = 11112;
			else if (messageArray[0].receiver.equals("5558"))
				receiverPort = 11116;
			
			Socket clientSocket = new Socket("10.0.2.2", receiverPort);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
			objectOutputStream.writeObject(messageArray[0]);
			clientSocket.close();
		}
		catch(IOException e)
		{
			//Log.v("Error in Client: ", e.getMessage().toString());
		}
		return null;
	}
	
}

@SuppressWarnings("serial")
class Message implements Serializable
{
	String messageType;
	String originalSender;
	String messageContent;
	String receiver;
	int hop;
		
	public Message()
	{
		super();
	}
}

class Hash
{
	public static String genHash(String input) throws NoSuchAlgorithmException 
	{
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    } 
}