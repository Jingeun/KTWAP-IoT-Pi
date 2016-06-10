package wap_kt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.xml.bind.DatatypeConverter;
import org.json.simple.JSONObject;
import io.socket.client.Socket;

public class VideoSerammingThread extends Thread{
	private final String DeviceID;
	private final String videoPath;
	private final int videoThreadTime = 100;
	private Socket socket;
	public VideoSerammingThread(String DeviceID, Socket socket) {
		this.DeviceID = DeviceID;
		this.socket = socket;
		this.videoPath = "/home/pi/KT/video/"+DeviceID+".jpg";
	}
	public void run(){
		while(true){
			if(socket.connected()){
				try {
					JSONObject videoJSON = new JSONObject();
					File videoFile = new File(videoPath);
					FileInputStream inputStream = new FileInputStream(videoFile);
					ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
					 
					int len=0;
			        byte[] buf = new byte[1024];
			        while((len=inputStream.read(buf)) != -1){
			        	byteOutStream.write(buf, 0, len);
			        }
			        String streamming = DatatypeConverter.printBase64Binary(byteOutStream.toByteArray());
			        videoJSON.put("base64", streamming);
					socket.emit("recv_display", videoJSON);
				} catch (Exception e) { e.printStackTrace(); } 
			}
			try { Thread.sleep(videoThreadTime);} 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
}
