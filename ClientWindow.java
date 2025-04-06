
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class ClientWindow implements ActionListener
{
	private JButton poll;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel question;
	private JLabel timer;
	private JLabel score;
	private JLabel username;
	private TimerTask clock;
	private TimerTask prevClock;

	private JLabel gameState;

	private JLabel answerFeedback;

	private JPanel panel;

	private JPanel scorePanel;

	Client client;

	int answerChoice;

	private JFrame window;
	
	private static SecureRandom random = new SecureRandom();
	
	// write setters and getters as you need
	
	public ClientWindow(Client client)
	{
		this.client = client;
		this.answerChoice = -1;
		

		window = new JFrame("Trivia Game");
		window.setLayout(new BorderLayout());

		panel = new JPanel();
		panel.setLayout(null); // Disable layout manager
		panel.setBounds(0, 0, window.getWidth(), window.getHeight()); // Set panel size
		window.add(panel, BorderLayout.CENTER);

		question = new JLabel("Question goes here."); // represents the question
		question.setBounds(10, 5, 350, 100);
		System.out.println(question.getText());
		panel.add(question);
		
		answerFeedback = new JLabel(); // represents feedback about answers
		answerFeedback.setBounds(200, 150, 200, 20);
		panel.add(answerFeedback);

		username = new JLabel(); // represents the username
		username.setText(String.format("<html><font color='#be2ee1'>%s</font>%s</html>", 
        "Username: ", client.getUsername()));
		username.setBounds(10, 0, 350, 50);
		panel.add(username);
		
		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for(int index=0; index<options.length; index++)
		{
			options[index] = new JRadioButton("Answer choice " + (index+1));  // represents an option
			// if a radio button is clicked, the event would be thrown to this class to handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110+(index*20), 350, 20);
			panel.add(options[index]);
			optionGroup.add(options[index]);
		}

		timer = new JLabel();  // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		panel.add(timer);

		gameState = new JLabel("Waiting for players...");  // represents the current state of the game
		gameState.setBounds(200, 200, 200, 20);
		panel.add(gameState);
		
		score = new JLabel("SCORE: 0"); // represents the score
		score.setBounds(50, 250, 100, 20);
		panel.add(score);

		poll = new JButton("Poll");  // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this);  // calls actionPerformed of this class
		panel.add(poll);
		
		submit = new JButton("Submit");  // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this);  // calls actionPerformed of this class
		panel.add(submit);

		reportScores(null);

		panel.repaint();
		
		window.setSize(800, 600);
		window.setBounds(50, 50, 800, 600);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);

		window.repaint();

		disableAllButtons();
	}

	public void startTimer(int duration){
		try{
			prevClock.cancel();
		}
		catch(NullPointerException e){
			System.out.println("no timer to cancel");
		}
		clock = new TimerCode(duration);  // represents clocked task that should run after X seconds
		prevClock = clock;
		Timer t = new Timer();  // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
	}

	public void reportScores(String scores){
		if(scorePanel != null){
			panel.remove(scorePanel);
		}
		scorePanel = new JPanel();
        scorePanel.setLayout(new BoxLayout(scorePanel, BoxLayout.Y_AXIS));
		scorePanel.setBounds(500, 0, 300, 600);
		if(scores != null){
			String[] lines = scores.split("\\$");
			scorePanel.add(new JLabel("SCORES:"));
			for(int i = 0; i < lines.length; i++){
				System.out.println(lines[i]);
				scorePanel.add(new JLabel(lines[i]));
			}
		}
		

		scorePanel.revalidate();
		scorePanel.repaint();

		panel.add(scorePanel);
		panel.revalidate();
		panel.repaint();

		window.revalidate();
		window.repaint();
	}
	

	public void lateTimer(){
		timer.setText("Time: joined late, polling still ongoing");
	}

	// for non-answering clients
	// all buttons are disabled
	public void disableAllButtons(){
		for(JRadioButton button : options){
			button.setEnabled(false);
		}
		optionGroup.clearSelection();
		poll.setEnabled(false);
		submit.setEnabled(false);
	}

	public void updateGameStateLabel(String gameState){
		this.gameState.setText(gameState);
	}

	// for answering client (first buzz)
	// all buttons are enabled except for poll
	public void answeringClientButtons(){
		for(JRadioButton button : options){
			button.setEnabled(true);
		}
		optionGroup.clearSelection();
		poll.setEnabled(false);
		submit.setEnabled(true);
	}

	public void pollingButtons(){
		for(JRadioButton button : options){
			button.setEnabled(false);
		}
		optionGroup.clearSelection();
		poll.setEnabled(true);
		submit.setEnabled(false);
	}

	public void updateAnswerFeedback(String text){
		this.answerFeedback.setText(text);
	}

	public void updateQuestionText(ClientQuestion currQuestion){
		question.setText(currQuestion.getQuestionText());
		answerFeedback.setText("");
		for(int i = 0; i < 4; i++){
			options[i].setText(currQuestion.getAnswers()[i]);
		}
	}

	public void updateScore(int clientScore){
		this.score.setText("SCORE: " + clientScore);
	}

	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll

	// TODO call appropriate method depending on button pressed
	@Override
	public void actionPerformed(ActionEvent e)
	{
		System.out.println("ACTION: You clicked " + e.getActionCommand());
		// input refers to the radio button you selected or button you clicked
		String input = e.getActionCommand();
		
		for(int i = 0; i < client.currQuestion.getAnswers().length; i++)
		{
			if(input.equals(client.currQuestion.getAnswers()[i]))
			{
				answerChoice = i;
				System.out.println("CURRENT SELECTED CHOICE index: " + client.currQuestion.getAnswers()[i]);
				break;
			}
		}


		switch(input)
		{
			case "Poll":
				// Your code here
				try {
					client.buzz();
				} catch (UnknownHostException e1) {
					System.out.println("Could not send buzz to host.");
				}
				break;
			case "Submit":
				// Your code here
				// call client.sendAnswer(answerChoice) or some adjacent method	
				System.out.println(answerChoice);
				client.sendAnswer(answerChoice);
				disableAllButtons();
				break;
			default:
				System.out.println("Incorrect Option");
		}
		
		// test code below to demo enable/disable components
		// DELETE THE CODE BELOW FROM HERE***

		// clicking submit or poll disables the clicked button and enables the other
		// commenting out because it is not needed

		// if(poll.isEnabled())
		// {
		// 	poll.setEnabled(false);
		// 	submit.setEnabled(true);
		// }
		// else
		// {
		// 	poll.setEnabled(true);
		// 	submit.setEnabled(false);
		// }
		
		// you can also enable disable radio buttons
//		options[random.nextInt(4)].setEnabled(false);
//		options[random.nextInt(4)].setEnabled(true);
		// TILL HERE ***
		
	}
	
	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask
	{
		private int duration;  // write setters and getters as you need
		public TimerCode(int duration)
		{
			this.duration = duration;
		}
		@Override
		public void run()
		{
			if(duration < 0)
			{
				timer.setText("Timer expired");
				window.repaint();
				//  disableAllButtons();
				this.cancel();  // cancel the timed task
				return;
				// you can enable/disable your buttons for poll/submit here as needed
			}
			
			if(duration < 6)
				timer.setForeground(Color.red);
			else
				timer.setForeground(Color.black);
			
			timer.setText("Time: " + duration);
			duration--;
			window.repaint();
		}
	}
	
}