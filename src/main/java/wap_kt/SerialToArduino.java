package wap_kt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;

import org.json.simple.JSONObject;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;


public class SerialToArduino implements SerialPortEventListener {
	SerialPort serialPort;
	private JSONObject obj = null;
	private final String DeviceID;
	private static final String[] PORT_NAMES = {"/dev/ttyACM0", "/dev/ttyACM1"};
	
	/** 포트에서 데이터를 읽기 위한 버퍼를 가진 input stream */
	private BufferedReader input;
	/** 포트를 통해 아두이노에 데이터를 전송하기 위한 output stream */
	private OutputStream output;
	/** 포트가 오픈되기 까지 기다리기 위한 대략적인 시간(2초) */
	private static final int TIME_OUT = 2000;
	/** 포트에 대한 기본 통신 속도, 아두이노의 Serial.begin의 속도와 일치 */
	private static final int DATA_RATE = 9600;
	
	public SerialToArduino(String DeviceID){
		this.DeviceID = DeviceID;
	}
	
	@SuppressWarnings("unchecked")
	private void sendToServer(String data[]){
		/** H2S+CH4+TEMP / data[0] : H2S  #  data[1] : CH4  #  data[2] : TEMP */
		obj = new JSONObject();
		String menu[] = {"ID", "H2S", "CH4", "TEMP", "ETC1", "ETC2", "ETC3"};
		String value[] = {DeviceID, data[0], data[1], data[2], "0", "0", "0"};
		for(int i=0;i<menu.length;i++)
			obj.put(menu[i], value[i]);
	}
	public JSONObject getJSON(){ return obj; }
	public void initialize() {
        //System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
		// 아두이노 포트 식별자 찾기
		while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            for (String portName : PORT_NAMES) {
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
            }
        }

		// 식별자를 찾지 못했을 경우 종료
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// 시리얼 포트 오픈, 클래스 이름을 애플리케이션을 위한 포트 식별 이름으로 사용
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
			// 속도등 포트의 파라메터 설정
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			 // 포트를 통해 읽고 쓰기 위한 스트림 오픈
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();
			// 아두이노로 부터 전송된 데이터를 수신하는 리스너를 등록
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) { System.err.println(e.toString()); }
	}
	
	/** 이 메서드는 포트 사용을 중지할 때 반드시 호출해야 한다. 리눅스와 같은 플랫폼에서는 포트 잠금을 방지한다. */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/** 시리얼 통신에 대한 이벤트를 처리. 데이터를 읽고 출력한다.. */
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String ArduinoMSG = input.readLine();
				String data[] = ArduinoMSG.trim().split("\\+");
				/** H2S+CH4+TEMP / data[0] : H2S  #  data[1] : CH4  #  data[2] : TEMP */ 
				sendToServer(data);
			} catch (Exception e) { System.err.println(e.toString()); }
		}
	}

}