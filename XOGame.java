import java.io.Serializable;

/**
 * XO Game Logic
 * Supports board sizes from 3x3 to 21x21
 */
public class XOGame implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int boardSize;
    private String[][] board;
    private String currentPlayer; // "X" or "O"
    private boolean gameOver;
    private String winner; // "X", "O", or ""
    private int moveCount;
    
    public XOGame(int boardSize) {
        if (boardSize < 3 || boardSize > 21) {
            throw new IllegalArgumentException("Board size must be between 3 and 21");
        }
        this.boardSize = boardSize;
        this.board = new String[boardSize][boardSize];
        this.currentPlayer = "X";
        this.gameOver = false;
        this.winner = "";
        this.moveCount = 0;
        
        // Initialize board with empty spaces
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                this.board[i][j] = "";
            }
        }
    }
    
    public boolean makeMove(int row, int col) {
        if (gameOver || row < 0 || row >= boardSize || col < 0 || col >= boardSize 
            || !board[row][col].equals("")) {
            return false;
        }
        
        board[row][col] = currentPlayer;
        moveCount++;
        
        if (checkWinner(row, col, currentPlayer)) {
            winner = currentPlayer;
            gameOver = true;
        } else if (moveCount == boardSize * boardSize) {
            winner = "";
            gameOver = true;
        } else {
            currentPlayer = currentPlayer.equals("X") ? "O" : "X";
        }
        return true;
    }
    
    private boolean checkWinner(int row, int col, String player) {
        int target = boardSize < 5 ? boardSize : 5;
        
        // Check horizontal
        if (countDirection(row, col, 0, 1, player) + countDirection(row, col, 0, -1, player) + 1 >= target) {
            return true;
        }
        // Check vertical
        if (countDirection(row, col, 1, 0, player) + countDirection(row, col, -1, 0, player) + 1 >= target) {
            return true;
        }
        // Check diagonal \
        if (countDirection(row, col, 1, 1, player) + countDirection(row, col, -1, -1, player) + 1 >= target) {
            return true;
        }
        // Check diagonal /
        return countDirection(row, col, 1, -1, player) + countDirection(row, col, -1, 1, player) + 1 >= target;
    }
    
    private int countDirection(int row, int col, int dr, int dc, String player) {
        int count = 0;
        int nr = row + dr;
        int nc = col + dc;
        while (nr >= 0 && nr < boardSize && nc >= 0 && nc < boardSize && board[nr][nc].equals(player)) {
            count++;
            nr += dr;
            nc += dc;
        }
        return count;
    }
    
    public String[][] getBoard() { 
        return board; 
    }
    
    public int getBoardSize() { 
        return boardSize; 
    }
    
    public String getCurrentPlayer() { 
        return currentPlayer; 
    }
    
    public boolean isGameOver() { 
        return gameOver; 
    }
    
    public String getWinner() { 
        return winner; 
    }
    
    public int getMoveCount() { 
        return moveCount; 
    }
    
    public String getStatus() {
        if (gameOver) {
            if (winner.equals("")) {
                return "Game Over: Tie!";
            }
            return "Game Over: Player " + winner + " Wins!";
        }
        return "Current: Player " + currentPlayer;
    }
    
    public void reset() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j] = "";
            }
        }
        currentPlayer = "X";
        gameOver = false;
        winner = "";
        moveCount = 0;
    }
}
