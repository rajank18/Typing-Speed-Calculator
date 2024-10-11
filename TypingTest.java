import java.awt.GridLayout;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import javax.swing.*;


public class TypingTest extends JFrame {

    private static final String DB_URL = "jdbc:oracle:thin:@127.0.0.1:1521:xe";
    private static final String DB_USER = "system";
    private static final String DB_PASSWORD = "system";

    private Connection conn;
    private UserProfile currentUser = null;

    public TypingTest() {
     
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

       
        setTitle("Typing Speed Test");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

      
        showMainMenu();
    }

    private void showMainMenu() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1));

        JLabel welcomeLabel = new JLabel("Welcome to the Typing Speed Test!!", SwingConstants.CENTER);
        panel.add(welcomeLabel);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> handleLogin());
        panel.add(loginButton);

        JButton signUpButton = new JButton("Sign Up");
        signUpButton.addActionListener(e -> handleSignUp());
        panel.add(signUpButton);

        JButton guestButton = new JButton("Use as Guest");
        guestButton.addActionListener(e -> {
            currentUser = new UserProfile("Guest");
            JOptionPane.showMessageDialog(this, "You are using the application as a guest.");
            showUserMenu();
        });
        panel.add(guestButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        panel.add(exitButton);

        setContentPane(panel);
        revalidate();
    }

    private void handleLogin() {
        String username = JOptionPane.showInputDialog(this, "Enter Username:");
        String password = JOptionPane.showInputDialog(this, "Enter Password:");

        if (username != null && password != null) {
            try {
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            JOptionPane.showMessageDialog(this, "Login successful.");
                            currentUser = new UserProfile(rs.getString("username"), rs.getInt("id"));
                            showUserMenu();
                        } else {
                            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSignUp() {
        String username = JOptionPane.showInputDialog(this, "Enter Username:");
        String password = JOptionPane.showInputDialog(this, "Enter Password:");

        if (username != null && password != null) {
            try {
                String checkQuery = "SELECT * FROM users WHERE username = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, username);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            JOptionPane.showMessageDialog(this, "Username already exists. Try a different username.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }

                String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Sign up successful. You can now log in.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showUserMenu() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1));

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getUsername(), SwingConstants.CENTER);
        panel.add(welcomeLabel);

        JButton typingTestButton = new JButton("Run Typing Test");
        typingTestButton.addActionListener(e -> runTypingTest());
        panel.add(typingTestButton);

        JButton viewStatsButton = new JButton("View My Stats");
        viewStatsButton.addActionListener(e -> showUserStats());
        panel.add(viewStatsButton);

        JButton leaderboardButton = new JButton("View Leaderboard");
        leaderboardButton.addActionListener(e -> showLeaderboard());
        panel.add(leaderboardButton);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            currentUser = null;
            showMainMenu();
        });
        panel.add(logoutButton);

        setContentPane(panel);
        revalidate();
    }

  

    private void runTypingTest() {
        String[] longLines = {
            "The quick brown fox jumps over the lazy dog.",
            "She sells seashells by the seashore, the shells she sells are surely seashells.",
            "How much wood would a woodchuck chuck if a woodchuck could chuck wood?"
        };
    
        String[] mediumLines = {
            "A journey of a thousand miles begins with a single step.",
            "To be or not to be, that is the question.",
            "All that glitters is not gold."
        };
    
        String[] shortLines = {
            "To be or not to be.",
            "The early bird catches the worm.",
            "Time is money."
        };
    
        String[] options = {"Long", "Medium", "Short", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Choose the type of line to type:", "Typing Test",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    
        String lineToType = null;
        Random random = new Random();
    
        switch (choice) {
            case 0:
                lineToType = longLines[random.nextInt(longLines.length)];
                break;
            case 1:
                lineToType = mediumLines[random.nextInt(mediumLines.length)];
                break;
            case 2:
                lineToType = shortLines[random.nextInt(shortLines.length)];
                break;
            case 3:
                return;  // Cancel
        }
    
        if (lineToType != null) {
            Instant start = Instant.now();
            String userInput = JOptionPane.showInputDialog(this, "Type the following line:\n" + lineToType);
            Instant end = Instant.now();
    
            if (userInput != null && userInput.equals(lineToType)) {
                Duration duration = Duration.between(start, end);
                long elapsedTimeInSeconds = duration.toMillis() / 1000;
                int wordCount = lineToType.split("\\s+").length;
                double wpm = (wordCount / (double) elapsedTimeInSeconds) * 60;
    
                try {
                    String insertQuery = "INSERT INTO test_results (user_id, wpm) VALUES (?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                        stmt.setInt(1, currentUser.getUserId());
                        stmt.setDouble(2, wpm);
                        stmt.executeUpdate();
                    }
    
                    JOptionPane.showMessageDialog(this, String.format("Words Per Minute: %.2f", wpm));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "The text you typed does not match.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    

    private void showUserStats() {
        try {
            String query = "SELECT MAX(wpm) AS best_wpm, AVG(wpm) AS avg_wpm FROM test_results WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, currentUser.getUserId());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, String.format("Best WPM: %.2f\nAverage WPM: %.2f",
                                rs.getDouble("best_wpm"), rs.getDouble("avg_wpm")));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showLeaderboard() {
        try {
            String query = "SELECT u.username, MAX(tr.wpm) AS best_wpm FROM users u JOIN test_results tr ON u.id = tr.user_id GROUP BY u.username ORDER BY best_wpm DESC";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                StringBuilder leaderboard = new StringBuilder("Leaderboard:\n");
                int rank = 1;
                while (rs.next()) {
                    leaderboard.append(String.format("%d. %s - Best WPM: %.2f%n", rank++, rs.getString("username"), rs.getDouble("best_wpm")));
                }
                JOptionPane.showMessageDialog(this, leaderboard.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TypingTest app = new TypingTest();
            app.setVisible(true);
        });
    }
}

class UserProfile {
    private final String username;
    private final int userId;

    public UserProfile(String username) {
        this(username, -1);
    }

    public UserProfile(String username, int userId) {
        this.username = username;
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;

    }

}