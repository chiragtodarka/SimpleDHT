package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnGDumpClickListener implements OnClickListener 
{
	public TextView textView;
	public ContentResolver contentResolver;
	public Uri uri;

	public OnGDumpClickListener(TextView textView, ContentResolver contentResolver) 
	{
		this.textView = textView;
		this.contentResolver = contentResolver;
		this.uri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
	}
	
	private Uri buildUri(String scheme, String authority) 
	{
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
	
	@Override
	public void onClick(View view) 
	{
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);	
	}
	
	private class Task extends AsyncTask<Void, String, Void>
	{
		@Override
		protected Void doInBackground(Void... arg0) 
		{
			String data = "";
			SimpleDhtProvider.gdumpMessage = null;
			SimpleDhtProvider.gdumpReceived = false;
			Cursor resultCursor = contentResolver.query(uri, null, "all", null, null);
			
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
			
			boolean flag = ((SimpleDhtProvider.portStr.equalsIgnoreCase(SimpleDhtProvider.predecessor)) && (SimpleDhtProvider.portStr.equalsIgnoreCase(SimpleDhtProvider.successor)));
			
			if(!flag)
			{
				Message message = new Message();
				message.messageType = "gdump";
				message.messageContent = "";
				message.originalSender = SimpleDhtProvider.portStr;
				message.receiver = SimpleDhtProvider.successor;
				new MyClient().executeOnExecutor(MyClient.THREAD_POOL_EXECUTOR, message);
				
				while(true)
				{
					if(SimpleDhtProvider.gdumpReceived== true && SimpleDhtProvider.gdumpMessage!=null)
						break;
				}
				
				data = data + SimpleDhtProvider.gdumpMessage.messageContent;
			}
						
			publishProgress(data);
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... strings) 
		{
			textView.append(strings[0]);
			return;
		}
	}

}
