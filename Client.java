import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Client extends JFrame implements ActionListener, KeyListener {
	// Login GUI 변수
	private JFrame loginGui = new JFrame();
	private JPanel loginPane;
	private JTextField ipTextField; // IP 받는 텍스트 필드
	private JTextField portTextField; //  Port 번호 받는 텍스트 필드
	private JTextField idTextField; // ID 받는 텍스트 필드
	private JButton logoinButton = new JButton("접속"); // 접속 버튼
	
	// Main GUI 변수
	private JPanel contentPane;
	private JLabel idlOnline = new JLabel("Online");
	private JLabel roomListLabel = new JLabel("Room List");
	private JButton noteSendButton = new JButton("쪽지보내기");
	private JButton joinButton = new JButton("방 참여");
	private JButton roomCreateButton = new JButton("방 만들기");
	private JButton sendButton = new JButton("전 송"); // 채팅 전송 버튼
	
	private JList userList = new JList(); // 전체 접속자 List
	private JList roomList = new JList(); // 전체 방목록 List
	
	private JTextField messeageTextField = new JTextField();
	private JTextArea chatArea = new JTextArea(); // 채팅창 변수
	
	//네트워크를 위한 자원 변수
	private Socket socket;
	private String ip = ""; // 127.0.0.1 은 자기 자신
	private int port;
	private String id = "";
	private InputStream is;
	private OutputStream os;
	private DataInputStream dis;
	private DataOutputStream dos;

	//그외 변수들
	Vector userListVC = new Vector();
	Vector roomListVC = new Vector();
	StringTokenizer stringTokenizer;
	
	private String myRoom; //  내가 있는 방
	
	public static void main(String[] args) {
		new Client();
	}
	
	Client() {
		loginInit(); // 로그인창 화면 구성 메소드
		mainInit();
		start();
	}
	
	private void start() {
		logoinButton.addActionListener(this); //로그인 버튼 리스너
		noteSendButton.addActionListener(this); // 쪽지보내기 버튼 리스너
		joinButton.addActionListener(this); // 채팅방 참여 버튼 리스너
		roomCreateButton.addActionListener(this); // 채팅방 만들기 버튼 리스너
		sendButton.addActionListener(this); // 채팅 전송 버튼 리스너
		messeageTextField.addKeyListener(this); // 키입력 리스너
	}
	
	private void loginInit() {
		loginGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		loginGui.setBounds(100, 100, 338, 406);
		loginPane = new JPanel();
		loginPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		loginGui.setContentPane(loginPane);
		loginPane.setLayout(null);
		
		JLabel ipLabel = new JLabel("Server IP");
		ipLabel.setBounds(23, 219, 85, 22);
		loginPane.add(ipLabel);
		
		JLabel portLabel = new JLabel("Server Port");
		portLabel.setBounds(23, 262, 85, 16);
		loginPane.add(portLabel);
		
		JLabel idLabel = new JLabel("ID");
		idLabel.setBounds(23, 297, 61, 16);
		loginPane.add(idLabel);
		
		ipTextField = new JTextField();
		ipTextField.setBounds(130, 216, 171, 28);
		ipTextField.setColumns(10);
		loginPane.add(ipTextField);
		
		portTextField = new JTextField();
		portTextField.setBounds(130, 256, 171, 28);
		portTextField.setColumns(10);
		loginPane.add(portTextField);
		
		idTextField = new JTextField();
		idTextField.setBounds(130, 291, 171, 28);
		idTextField.setColumns(10);
		loginPane.add(idTextField);
		
		logoinButton.setBounds(93, 340, 117, 29);
		loginPane.add(logoinButton);
		
		loginGui.setVisible(true); // ture면 화면에 보임
	}
	
	private void mainInit() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 560, 476);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		idlOnline.setBounds(28, 12, 61, 16);
		contentPane.add(idlOnline);
		
		roomListLabel.setBounds(23, 218, 72, 21);
		contentPane.add(roomListLabel);
		
		noteSendButton.setBounds(23, 190, 85, 29);
		contentPane.add(noteSendButton);

		roomCreateButton.setBounds(23, 419, 97, 29);
		contentPane.add(roomCreateButton);
		
		joinButton.setBounds(23, 390, 97, 29);
		contentPane.add(joinButton);
		
		sendButton.setBounds(493, 404, 61, 29);
		contentPane.add(sendButton);
		sendButton.setEnabled(false); // 채팅 방 참여 후 채팅 창에 입력 가능하도록 처리
		
		userList.setBounds(23, 40, 97, 145);
		contentPane.add(userList);
		userList.setListData(userListVC);
		
		roomList.setBounds(22, 251, 98, 127);
		contentPane.add(roomList);
		roomList.setListData(roomListVC);
		
		chatArea.setBounds(132, 40, 407, 351);
		contentPane.add(chatArea);
		chatArea.setEditable(false); // 채팅 방 참여 후 채팅 창에 입력 가능하도록 처리
		
		messeageTextField.setBounds(136, 403, 355, 28);
		messeageTextField.setColumns(10);
		contentPane.add(messeageTextField);
		messeageTextField.setEditable(false); //방을 만든 후 메세지 입력 가능하도록 처리
		
		this.setVisible(false); 
	}
		
	private void netWork() {
		try {
			socket = new Socket(ip, port);
			if (socket != null) { //정상저으로 소켓이 연결되었을 경우
				connection();
			}
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "연결 실패", "알림", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "연결 실패", "알림", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void connection() {  // 실제적인 메소드 연결 부분
		try {
		is = socket.getInputStream();
		dis = new DataInputStream(is);
		
		os = socket.getOutputStream();
		dos = new DataOutputStream(os);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "연결 실패", "알림", JOptionPane.ERROR_MESSAGE);
		} // Stream 설정 끝.
		
		this.setVisible(true); // Main GUI 활성화
		this.loginGui.setVisible(false); // Login GUI 비활성화
		
		// 처음 접속시에 ID 전송
		sendMessage(id); 
		
		//userLists에 사용자 추가
		userListVC.add(id);
		
		Thread thread = new Thread (new Runnable() {
			
			@Override
			public void run() {
				
				while (true) {
					try {
						String msg = dis.readUTF(); // 메세지 수신
						System.out.println("서버로부터 수신된 메세지 : " + msg);
						
						inMessage(msg);
						
					} catch (IOException e) {
						try {
							os.close();
							is.close();
							dis.close();
							dos.close();
							socket.close();
						JOptionPane.showMessageDialog(null, "서버와 접속이 끊어졌습니다", "알림", JOptionPane.ERROR_MESSAGE);
						} catch (IOException ex) {}
								break; 
					} 
				}
			}
		});
		thread.start();
	}
	
	private void sendMessage(String str) { // 서버에서 메세지를 보내는 부분
		try {
			dos.writeUTF(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void inMessage(String str) { // 서버로부터 들어오는 모든 메세지
		
		stringTokenizer = new StringTokenizer(str,"/");
		
		String protocol = stringTokenizer.nextToken();
		String Message = stringTokenizer.nextToken();
		
		System.out.println("프로토콜: " + protocol);
		System.out.println("내용 : " + Message);
		
		if (protocol.equals("NewUser")) { // 새로운 접속자
			userListVC.add(Message);
			
		} else if (protocol.equals("OldUser")) {
			userListVC.add(Message);
			
		} else if (protocol.equals("Note")) {

			String note = stringTokenizer.nextToken();
			
			System.out.println(Message + " 사용자로부터 온 쪽지 " + note);
			JOptionPane.showMessageDialog(null, note, Message + "님으로 부터 쪽지",JOptionPane.CLOSED_OPTION);
			
		} else if (protocol.equals("userListUpdate")) {
			userList.setListData(userListVC);
			
		} else if (protocol.equals("CreateRoom")) { // 방을 만들었을때
			myRoom = Message;
			messeageTextField.setEditable(true); // 방개설 후 메세지 입력 가능하도록 처리
			sendButton.setEnabled(true); // 방개설 후 메세지 입력 가능하도록 처리
			joinButton.setEnabled(false); // 채개방 후 다른방에 입장 할 수 없도록 처리
			roomCreateButton.setEnabled(false);  //개팅방 후 다른방에 개설 할 수 없도록 처리
			
		} else if (protocol.equals("CreateRoomFail")) {
			JOptionPane.showMessageDialog(null, "방 만들기 실패", "알림", JOptionPane.ERROR_MESSAGE);
			
		} else if (protocol.equals("NewRoom")) { //새로운 방을 만들었을때
			roomListVC.add(Message);
			roomList.setListData(roomListVC);
			
		} else if (protocol.equals("Chatting")) {
			String msg = stringTokenizer.nextToken();
			chatArea.append(Message + " : " + msg + "\n");
			
		} else if (protocol.equals("OldRoom")) {
			roomListVC.add(Message);
			
		} else if (protocol.equals("roomListUpdate")) {
			roomList.setListData(roomListVC);
			
		} else if (protocol.equals("JoinRoom")) {
			myRoom = Message;
			messeageTextField.setEditable(true); // 방입장 후 메세지 입력 가능하도록 처리
			sendButton.setEnabled(true); // 방입장 후 메세지 입력 가능하도록 처리
			joinButton.setEnabled(false); // 채팅방 참여 후 다른방에 입장 할 수 없도록 처리
			roomCreateButton.setEnabled(false);  //채팅방 참여 후 다른방에 개설 할 수 없도록 처리
			JOptionPane.showMessageDialog(null, "채팅방에 입장했습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
			
		} else if (protocol.equals("UserOut")) {
			userListVC.remove(Message);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == logoinButton) {
			System.out.println("로그인 버튼 클릭");
			
			if (ipTextField.getText().length()==0) {
				ipTextField.setText("IP를 입력해주세요");
				ipTextField.requestFocus(); //IP 주소 입력 안했을 시 setText("IP를 입력해주세요") 메세지를 출력
				
			} else if (portTextField.getText().length()==0) {
				portTextField.setText("Port번호를 입력해주세요");
				portTextField.requestFocus();
				
			} else if (idTextField.getText().length()==0) {
				idTextField.setText("ID를 입력해주세요");
				idTextField.requestFocus();
				
			} else {
				ip = ipTextField.getText().trim(); // trim 은 입력 시 빈공간이 있으면 그것을 매워준다.
				port = Integer.parseInt(portTextField.getText().trim()); // trim은 String으로 가져오기 때문에 Integer로 형변환.
				id = idTextField.getText().trim(); // ID 받아오는 부분
				netWork();
			}
			
		} else if (e.getSource() == noteSendButton) {
			System.out.println("쪽지 보내기 버튼 클릭");
			String user = (String) userList.getSelectedValue();
			String note = JOptionPane.showInputDialog("보낼메세지");
			
			if (note!=null) {
				sendMessage("Note/" + user + "/" + note);
				// ex) Note/User2/ 나는 User1이야
				// Note = protocol/ User2 =  받는 사람 / User1이야 = 메세지 
			}
			System.out.println("받는 사람 : " + user + " | 보낼 내용 : " + note);
			
		} else if (e.getSource() == joinButton) {
			String joinRoom = (String) roomList.getSelectedValue();
			
			sendMessage("JoinRoom/" + joinRoom);
			
			System.out.println("방 참여 버튼 클릭");
			
		} else if (e.getSource() == roomCreateButton) {
			String roomName = JOptionPane.showInputDialog("방 이름");
			if (roomName !=null) {
				sendMessage("CreateRoom/" + roomName);
			}
			System.out.println("방 만들기 버튼 클릭");
			
		} else if (e.getSource() == sendButton) {
			sendMessage("Chatting/" + myRoom + "/" + messeageTextField.getText().trim());
			messeageTextField.setText(""); // 메세지 전송 후 입력창이 비어있게 만들기 위함.
			messeageTextField.requestFocus(); // 전송 후 다시 위치 할 수 있게 처리

			//Chatting + 방 이름 + 내용
			System.out.println("채팅 전송 버튼 클릭");
		}
	}

	@Override
	public void keyReleased(KeyEvent e) { //키를 눌렀다가 땟을때
		System.out.println(e);
		if (e.getKeyCode()==10) {
			sendMessage("Chatting/" + myRoom + "/" + messeageTextField.getText().trim());
			messeageTextField.setText(""); // 메세지 전송 후 입력창이 비어있게 만들기 위함.
			messeageTextField.requestFocus(); // 전송 후 다시 위치 할 수 있게 처리
		}
	}
	
	@Override
	public void keyPressed(KeyEvent e) { // 키를 눌렀을때
		// TODO Auto-generated method stub
	}

	@Override
	public void keyTyped(KeyEvent e) { // 타이핑을 했을떄
		// TODO Auto-generated method stub
	}
}