import java.io.*;
import java.net.*;
import java.util.Date;
import javafx.application.Application;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class gameServer extends Application implements TicTacToeConstants {
	private int sessionNo = 1; // Number for first session
	private int port = 8022;

	@Override // Override the start method from the Application class
	public void start(Stage primaryStage) {
		TextArea taLog = new TextArea();

		// Creates a scene and places a stage within the scene
		Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
		primaryStage.setTitle("Server"); // Set the stage title to Server
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage a.k.a the UI

		new Thread(() -> {
			try {
				// Creating server socket
				@SuppressWarnings("resource") // Suppresses the resource leak warning
				ServerSocket serverSocket = new ServerSocket(port);

				// Print the date and time and socket number to the stage
				Platform.runLater(() -> taLog.appendText(new Date() + ": Server started at socket " + port + '\n' ));

				// Ready to create a session for every two players
				while (true) {
					Platform.runLater(() -> taLog.appendText(new Date() + ": Waiting for players to join session " + sessionNo + '\n'));

					// Connect to player 1, listening for connections to the socket and accepts it
					Socket player1 = serverSocket.accept();
					// Writes in the stage when a player joins the server and their session number
					Platform.runLater(() -> {
						taLog.appendText(new Date() + ": Player 1 joined session " + sessionNo + '\n');
						// Gets the player IP address and prints it in the server stage
						taLog.appendText("Player 1's IP address" + player1.getInetAddress().getHostAddress() + '\n');
					});

					// Notify that the player is Player 1
					new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);

					// Connect to player 2 again listening for connections to the socket and accepts
					// it
					Socket player2 = serverSocket.accept();
					// Writes in the stage when a player joins the server and their session number
					Platform.runLater(() -> {
						taLog.appendText(new Date() + ": Player 2 joined session " + sessionNo + '\n');
						// Gets the player IP address and prints it in the server stage
						taLog.appendText("Player 2's IP address" + player2.getInetAddress().getHostAddress() + '\n');
					});

					// Notify that the player is Player 2
					new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);

					// Display this session and increment session number
					Platform.runLater(
							() -> taLog.appendText(new Date() + ": Start a thread for session " + sessionNo++ + '\n'));

					// Starts a new thread for each session of two players
					new Thread(new HandleASession(player1, player2)).start();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}).start();
	}

	// Define the thread class for handling a new session for two players
	class HandleASession implements Runnable, TicTacToeConstants {
		// Defining socket variables
		private Socket player1;
		private Socket player2;

		// Create and initialise cells for the 3x3 grid
		private char[][] cell = new char[3][3];

		// Constructing thread
		public HandleASession(Socket player1, Socket player2) {
			this.player1 = player1;
			this.player2 = player2;

			// Initialise cells
			for (int i = 0; i < 3; i++)
				for (int j = 0; j < 3; j++)
					cell[i][j] = ' ';
		}

		// Implement the run() method for the thread
		public void run() {
			try {
				// Create data input and output streams
				DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
				DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
				DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
				DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());

				// This is just to let player 1 know to start
				toPlayer1.writeInt(1);

				// Continuously serve the players and determine and report
				// the game status to the players
				while (true) {
					// Receive a move from player 1 by reading values for the row and column
					int row = fromPlayer1.readInt();
					int column = fromPlayer1.readInt();
					cell[row][column] = 'X';

					// Check if Player 1 wins
					if (playerWins('X')) {
						toPlayer1.writeInt(PLAYER1_WON);
						toPlayer2.writeInt(PLAYER1_WON);
						sendMove(toPlayer2, row, column);
						break; // Break the loop
					} else if (gridFull()) { // Check if all cells are filled, which means it's a draw if no winner was
											// found
						toPlayer1.writeInt(DRAW);
						toPlayer2.writeInt(DRAW);
						sendMove(toPlayer2, row, column);
						break;
					} else {
						// Notify player 2 to take the turn
						toPlayer2.writeInt(CONTINUE);
						// Send player 1's selected row and column to player 2
						sendMove(toPlayer2, row, column);
					}

					// Receive a move from Player 2
					row = fromPlayer2.readInt();
					column = fromPlayer2.readInt();
					cell[row][column] = 'O';

					// Check if Player 2 wins, no need to check for draw since it is done above
					if (playerWins('O')) {
						toPlayer1.writeInt(PLAYER2_WON);
						toPlayer2.writeInt(PLAYER2_WON);
						sendMove(toPlayer1, row, column);
						break;
					} else {
						// Notify player 1 to take the turn
						toPlayer1.writeInt(CONTINUE);

						// Send player 2's selected row and column to player 1
						sendMove(toPlayer1, row, column);
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		// Send the move to other player
		private void sendMove(DataOutputStream out, int row, int column) throws IOException {
			out.writeInt(row); // Send row index
			out.writeInt(column); // Send column index
		}

		// Determine if the cells are all occupied which is determining if there is a
		// draw between the players
		private boolean gridFull() {
			for (int i = 0; i < 3; i++)
				for (int j = 0; j < 3; j++)
					if (cell[i][j] == ' ')
						return false; // At least one cell is not filled

			// All cells are filled
			return true;
		}

		// Determines if the player with the specified token wins
		private boolean playerWins(char token) {
			// Check all rows
			for (int i = 0; i < 3; i++)
				if ((cell[i][0] == token) && (cell[i][1] == token) && (cell[i][2] == token)) {
					return true;
				}

			// Check all columns
			for (int j = 0; j < 3; j++)
				if ((cell[0][j] == token) && (cell[1][j] == token) && (cell[2][j] == token)) {
					return true;
				}

			// Checking diagonal
			if ((cell[0][0] == token) && (cell[1][1] == token) && (cell[2][2] == token)) {
				return true;
			}

			// Checking other diagonal
			if ((cell[0][2] == token) && (cell[1][1] == token) && (cell[2][0] == token)) {
				return true;
			}

			// All checked, but no winner
			return false;
		}
	}
}