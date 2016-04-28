import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

//귓속말, 방퇴장, 방에 있는 사용자 리스트 받기 기능 미완성

public class Server extends JFrame implements ActionListener{

	private JPanel contentPane;
	private JTextField portTextField;
	private JTextArea textArea = new JTextArea();
	private JButton startButton = new JButton("서버 실행");
	private JButton stopButton = new JButton("서버 중지");
	
	//네트워크 지원
	private ServerSocket serverSocket; 
	private Socket socket;
	private int port;
	private Vector userVector = new Vector();
	private Vector roomVector = new Vector();
	private boolean roomCheck = true;
	
	private StringTokenizer stringTokenizer;
	
	public static void main(String[] args) {
		
		new Server();
		
	}
	
	private void init() {   // 화면 구성
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 332, 364);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(6, 6, 320, 233);
		contentPane.add(scrollPane);
		
		scrollPane.setViewportView(textArea);
		textArea.setEditable(false); //false면 서버메세지창에 텍스트 입력 중지.
		
		JLabel label = new JLabel("포트번호");
		label.setBounds(16, 251, 61, 16);
		contentPane.add(label);
		
		portTextField = new JTextField();
		portTextField.setBounds(89, 245, 127, 28);
		contentPane.add(portTextField);
		portTextField.setColumns(10);
		
		startButton.setBounds(26, 285, 117, 29);
		contentPane.add(startButton);
		
		stopButton.setBounds(171, 285, 117, 29);
		contentPane.add(stopButton);
		stopButton.setEnabled(false); // 처음 실행 시 서버 중비 버튼 사용 할 수 없도록 처리
		
		this.setVisible(true); //true 면 화면에 보이게 
	}
	
	Server() { // 생성자
		init(); //화면 생성 메소드
		start(); // 리스너 설정 메소드
	}

	private void start() { 
		startButton.addActionListener(this);
		stopButton.addActionListener(this);
	}
	
	private void ServerStart() {
		try {
			serverSocket = new ServerSocket(port); // 12345번 포트 사용
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "이미 사용중인 포트입니다", "알림", JOptionPane.ERROR_MESSAGE);
		} //12345번 포트 사용
		if (serverSocket != null) { //정상적으로 포트가 열렸을 경우
			Connection();
		}
	}
	
	private void Connection() {
		//1개의 Thread는 1가지의 일만 처리할 수 있다.
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() { // Thread에서 처리 할 일을 기재.
				
				while (true) {				
				try {
					textArea.append("사용자 접속 대기중\n");
					socket = serverSocket.accept(); // 사용자 접속대기/ 무한 대기
					textArea.append("사용자 접속!\n");
					UserInfo user = new UserInfo(socket);
					user.start(); // 객체의 Thread 실행
				} catch (IOException e) {
					break;
				}
			} //  end while
		}
	});
		thread.start();
	}
	
	class RoomInfomation {
		private String roomName;
		private Vector roomUserVector = new Vector();
		
		RoomInfomation(String roomName, UserInfo userInfo) {
			this.roomName = roomName;
			this.roomUserVector.add(userInfo);
		}
		public void broadCastRoom(String str) { // 현재 방의 모든 사람에게 알린다.
			for (int i = 0; i < roomUserVector.size(); i++) {
				UserInfo userInfo = (UserInfo)roomUserVector.elementAt(i);
				userInfo.sendMessage(str);
			}
		}
		private void AddUser(UserInfo userInfo) {
			this.roomUserVector.add(userInfo);
		}
	}
	
	@Override 
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == startButton) {
			System.out.println("서버 실행 버튼 클릭");
			
			port = Integer.parseInt(portTextField.getText().trim());
			
			ServerStart(); // 소켓 생성 및 사용자 접속 대기
			
			startButton.setEnabled(false); // 서버 시작 버튼이 2번 눌리지 않도록 처리
			portTextField.setEditable(false); // port 번호도 수정 할 수 없도록 처리
			stopButton.setEnabled(true); // 중비 버튼 활성화
			
		} else if (e.getSource() == stopButton) {
			stopButton.setEnabled(false);
			startButton.setEnabled(true); // 중지 버튼 누루면 다시 사용할 수 있도록 처리
			portTextField.setEditable(true);
			
			try {
				serverSocket.close();
				userVector.removeAllElements(); 
				roomVector.removeAllElements(); // 서버 초기화 상태로 만든닫
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("서버 중지 버튼 클릭");
		}
	} //ActionEvent 끝.
	
	class UserInfo extends Thread {
		private InputStream is;
		private OutputStream os;
		private DataInputStream dis;
		private DataOutputStream dos;
		
		private Socket userSocket;
		private String nickName = "";
		
		UserInfo(Socket socket) { // 생성자 메소드
			this.userSocket = socket;
			userNetwork();
		}
		
		private void userNetwork() { // 네트워크 자원 설정
			try {
			is = userSocket.getInputStream();
			dis = new DataInputStream(is); 
			
			os =  userSocket.getOutputStream();
			dos = new DataOutputStream(os);
			
			nickName = dis.readUTF(); // 사용자의 닉네임을 받는다.
			textArea.append(nickName + " :사용자 접속!");
			//기존 사용자들에게 새호운 사용자 알림
			System.out.println("현재 접속된 사용자 수 : " + userVector.size());
			
			broadCast("NewUser/" + nickName); //기존 사용자에게 자신을 알린다.
			
			//자신에게 기존 사용자를 받아오는 부분
			for (int i = 0; i < userVector.size(); i++) {
				UserInfo user = (UserInfo)userVector.elementAt(i);
				sendMessage("OldUser/" + user.nickName);
			}
			for (int i=0; i < roomVector.size(); i++) {
				RoomInfomation r = (RoomInfomation)roomVector.elementAt(i);
				sendMessage("OldRoom/" + r.roomName);
			}

				sendMessage("roomListUpdate/ ");
			
				userVector.add(this); // 사용자에게 알린 후 Vector에 자신을 추가
				broadCast("userListUpdate/ ");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Stream설정 에러 발생", "알림", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		public void run() { // Thread에서 처리할 내용
			while (true) {
				try {
					String msg = dis.readUTF();
					textArea.append(nickName + "사용자로부터 들어온 메세지 : " + msg);
				 	inMessage(msg);
				} catch (IOException e) {
					textArea.append(nickName + "사용자의 접속이 끊어졌습니다\n");
					
					try {
						dos.close();
						dis.close();
						userSocket.close();
						userVector.remove(this);
						broadCast("UserOut/" + nickName);
						broadCast("userListUpdate/ ");
					} catch (IOException e1) {} break;
				} // 메세지 수신
			}
		} // end run.
		
		private void inMessage (String str) { //클라이언트로부터 들어오는 메세지 처리
			stringTokenizer = new StringTokenizer(str,"/");
			String protocol = stringTokenizer.nextToken();
			String message = stringTokenizer.nextToken();
			
			System.out.println("프로토콜 :" + protocol);
			System.out.println("메세지 : " + message);
			
			if (protocol.equals("Note")) {
				//protocol = Note
				//message = user2@~~
				String note = stringTokenizer.nextToken();
				
				System.out.println("받는 사람 : " + message);
				System.out.println("보낼 내용 : " + note);
			
				//백터에서 해당 사용자 찾아서 메세지 전송
				 for (int i = 0; i < userVector.size(); i++) {
					 UserInfo u = (UserInfo) userVector.elementAt(i);
					 if (u.nickName.equals(message)) {
						 u.sendMessage("Note/" + nickName + "/" + note);
						 //Note/User1/~~
					 } 
				 } 
			// end if문
			} else if (protocol.equals("CreateRoom")) { 
				 //1. 현재 같은 방이 존재 하는지 확인한다.
				 for (int i = 0; i < roomVector.size(); i++) {
					 RoomInfomation roomInfo = (RoomInfomation)roomVector.elementAt(i);
					 if (roomInfo.roomName.equals(message)) { // 만들고자 하는 방이 존재할때
						 sendMessage("CreateRoomFail/ok"); 
						 roomCheck = false;
						 break;
					 } 
				 } // end for
				 if (roomCheck) { // 방을 만들 수 있을때 
					RoomInfomation newRoom = new RoomInfomation(message, this);
					roomVector.add(newRoom); // 전체 방 백터에 방을 추가;
					sendMessage("CreateRoom/" + message);
					broadCast("NewRoom/" + message);
				 }
				 roomCheck = true;
			 // end else if 	 
			} else if (protocol.equals("Chatting")) {
				 String msg = stringTokenizer.nextToken();
				 for (int i = 0; i < roomVector.size(); i++) {
					 RoomInfomation r = (RoomInfomation)roomVector.elementAt(i);
					 if (r.roomName.equals(message)) { // 해당 방을 찾았을때
						r.broadCastRoom("Chatting/" + nickName + "/" + msg);
					 }
				 }
			 } else if (protocol.equals("JoinRoom")) {
				 for (int i = 0; i < roomVector.size(); i++) {
					 RoomInfomation r = (RoomInfomation)roomVector.elementAt(i);
					 if (r.roomName.equals(message)) {
						 // 사용자 추가
						 r.broadCastRoom("Chatting/알림/**********" + nickName + "님이 입장하셨습니다**********");
						 
						 r.AddUser(this);
						 sendMessage("JoinRoom/" + message);
					 }
				 }
			 }
		}
		
		private void sendMessage (String str) { // 문자열을 받아서 전송
			try {
				dos.writeUTF(str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void broadCast(String str) { // 전체 사용자에게 메세지 보내는 부분
			for (int i = 0; i < userVector.size(); i++) {
				UserInfo user = (UserInfo)userVector.elementAt(i);
		
				user.sendMessage(str);
			}
		}
	}
}