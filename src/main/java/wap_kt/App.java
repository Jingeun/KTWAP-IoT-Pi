package wap_kt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import org.json.simple.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;

public class App{
	private final int ServerWatingTime = 20*1000;
	private final static int SendToMSGWatingTime = 5*1000;
	private static String DeviceID;
	private final String URL = "http://ktwap.kro.kr:9900";
	public Socket socket;
	private final String KTURL = "http://59.20.175.137/iotco.php";
	public App(String DeviceID) throws URISyntaxException{
		this.DeviceID = DeviceID; 
		this.socket = IO.socket(URL);
	}
	private void connection() {
		if(socket.connected()) return;
		try {		
			socket.connect();
			while(!socket.connected()){
				socket.connect();
				System.out.println("Server is not connection. waiting 20 seconds...");
				Thread.sleep(ServerWatingTime);
			}
			JSONObject device = new JSONObject();
			device.put("ID", DeviceID);
			socket.emit("device", device);
		} catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	private void sendToMSG(JSONObject json) throws InterruptedException {
		if(json!=null){
			JSONObject result = new JSONObject();
			result.put("device", json);
			System.out.println("MSG - "+result.toJSONString());
			socket.emit("detail_info", result);
		}
	}
	private void checkActice() {
		socket.on("Active", new Listener(){
			public void call(Object... arg0) {
				System.out.println("서버에서 물어봄");
				if(socket.connected()){
					System.out.println("현재 활성중임");
					JSONObject result = new JSONObject();
					result.put("Req", "OK");
					socket.emit("Req", result);
				}
			}
		});
	}

	private void sendToMSG_iotco(JSONObject json) throws IOException {
		//ID=KT00003&CH4=123&TEMP=24.55&H2S=123&ETC1=0&ETC2=0&ETC3=0
		String sendGetURL = KTURL+"?ID="+json.get("ID").toString()+"&CH4="+json.get("CH4").toString()+"&TEMP="+json.get("TEMP").toString()
				+"&ETC1=0&ETC2=0&ETC3=0";
		URL obj = new URL(sendGetURL);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setConnectTimeout(2000);
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		if(responseCode == HttpURLConnection.HTTP_OK){
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
		}
	}
	
    public static void main( String[] args ) throws Exception{
    	SerialToArduino Arduino = new SerialToArduino(args[0]);
    	Arduino.initialize();
    	App application = new App(args[0]);
    	VideoSerammingThread videoThread = new VideoSerammingThread(args[0], application.socket);
    	videoThread.start();
    	application.checkActice();
    	while(true){
    		application.connection();
    		if(Arduino.getJSON()!=null){
    			application.sendToMSG(Arduino.getJSON());
    			application.sendToMSG_iotco(Arduino.getJSON());
    		}
    		Thread.sleep(SendToMSGWatingTime);
    	}
    }
    
}