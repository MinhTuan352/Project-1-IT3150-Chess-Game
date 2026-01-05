package main;

import piece.Piece;
import piece.Bishop;
import piece.Knight;
import piece.Queen;
import piece.Rook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * GameLogic - Chứa toàn bộ logic game cờ vua
 * Tách từ GamePanel để dễ quản lý
 */
public class GameLogic {
    // Color constants
    public static final int WHITE = 0;
    public static final int BLACK = 1;

    // Piece lists
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    public static ArrayList<Piece> capturedPieces = new ArrayList<>();
    public ArrayList<Piece> promoPieces = new ArrayList<>();

    // Active pieces
    public Piece activeP;
    public Piece checkingP;
    public static Piece castlingP;

    // Game state
    public int currentColor = WHITE;
    public boolean canMove;
    public boolean validSquare;
    public boolean promotion;
    public boolean gameOver;

    // Timer
    public GameSettings settings;
    public int whiteTime;
    public int blackTime;
    public long lastTimerTime;

    // History
    public ArrayList<String> moveList = new ArrayList<>();
    public ArrayList<HistoryMove> history = new ArrayList<>();
    public int halfmoveClock = 0;
    public HashMap<String, Integer> positionHistory = new HashMap<>();

    // Board reference for coordinate conversion
    private Board board = new Board();

    public GameLogic() {
        setPieces();
        copyPieces(pieces, simPieces);
    }

    // Thiết lập ván đấu
    public void setupGame(GameSettings settings) {
        this.settings = settings;
        setPieces();
        copyPieces(pieces, simPieces);
        capturedPieces.clear();
        moveList.clear();
        history.clear();

        whiteTime = settings.timeLimit;
        blackTime = settings.timeLimit;
        lastTimerTime = System.nanoTime();

        currentColor = WHITE;
        activeP = null;
        promotion = false;
        gameOver = false;
        halfmoveClock = 0;
        positionHistory.clear();
        updatePositionHistory();
    }

    public void setPieces() {
        pieces.clear();
        simPieces.clear();
        capturedPieces.clear();

        // Bên trắng
        pieces.add(new piece.Rook(0, 7, WHITE));
        pieces.add(new piece.Knight(1, 7, WHITE));
        pieces.add(new piece.Bishop(2, 7, WHITE));
        pieces.add(new piece.Queen(3, 7, WHITE));
        pieces.add(new piece.King(4, 7, WHITE));
        pieces.add(new piece.Bishop(5, 7, WHITE));
        pieces.add(new piece.Knight(6, 7, WHITE));
        pieces.add(new piece.Rook(7, 7, WHITE));
        for (int i = 0; i < 8; i++) {
            pieces.add(new piece.Pawn(i, 6, WHITE));
        }

        // Bên đen
        pieces.add(new piece.Rook(0, 0, BLACK));
        pieces.add(new piece.Knight(1, 0, BLACK));
        pieces.add(new piece.Bishop(2, 0, BLACK));
        pieces.add(new piece.Queen(3, 0, BLACK));
        pieces.add(new piece.King(4, 0, BLACK));
        pieces.add(new piece.Bishop(5, 0, BLACK));
        pieces.add(new piece.Knight(6, 0, BLACK));
        pieces.add(new piece.Rook(7, 0, BLACK));
        for (int i = 0; i < 8; i++) {
            pieces.add(new piece.Pawn(i, 1, BLACK));
        }
    }

    public void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for (int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    // Lưu trạng thái để undo
    public void saveState() {
        HistoryMove state = new HistoryMove();
        state.pieceList = new ArrayList<>();
        for (Piece piece : pieces) {
            Piece copy = piece.getCopy();
            if (piece == activeP) {
                copy.col = piece.preCol;
                copy.row = piece.preRow;
                copy.x = piece.getX(copy.col);
                copy.y = piece.getY(copy.row);
            }
            state.pieceList.add(copy);
        }

        state.capturedList = new ArrayList<>();
        for (Piece piece : capturedPieces) {
            state.capturedList.add(piece.getCopy());
        }

        state.wTime = whiteTime;
        state.bTime = blackTime;
        state.turn = currentColor;
        state.castlingP = (castlingP != null) ? castlingP.getCopy() : null;
        history.add(state);
    }

    public void undo() {
        if (history.isEmpty())
            return;

        int steps = 1;
        if (settings.isPvE && history.size() >= 2) {
            steps = 2;
        } else if (settings.isPvE && history.size() == 1) {
            steps = 1;
        }

        for (int i = 0; i < steps; i++) {
            if (history.isEmpty())
                break;

            HistoryMove prevState = history.remove(history.size() - 1);
            pieces = prevState.pieceList;
            capturedPieces = prevState.capturedList;
            whiteTime = prevState.wTime;
            blackTime = prevState.bTime;
            currentColor = prevState.turn;
            castlingP = prevState.castlingP;

            for (Piece piece : pieces) {
                piece.x = piece.getX(piece.col);
                piece.y = piece.getY(piece.row);
            }
            copyPieces(pieces, simPieces);

            if (!moveList.isEmpty()) {
                moveList.remove(moveList.size() - 1);
            }
        }

        activeP = null;
        promotion = false;
        gameOver = false;
        validSquare = false;
    }

    // Simulate move (called when dragging)
    public void simulate(int mouseX, int mouseY) {
        canMove = false;
        validSquare = false;
        copyPieces(pieces, simPieces);

        if (castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        activeP.x = mouseX - Board.HALF_SQUARE_SIZE;
        activeP.y = mouseY - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;
            if (activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }
            checkCastling();
            if (!isIllegal(activeP)) {
                validSquare = true;
            }
        }
    }

    // Timer update
    public void updateTimer() {
        if (settings == null || settings.timeLimit == -1)
            return;

        if (System.nanoTime() - lastTimerTime >= 1000000000) {
            if (currentColor == WHITE) {
                whiteTime--;
            } else {
                if (!settings.isPvE) {
                    blackTime--;
                }
            }
            lastTimerTime = System.nanoTime();

            if (whiteTime <= 0 || blackTime <= 0) {
                gameOver = true;
            }
        }
    }

    // AI Move
    public void performAIMove() {
        ArrayList<Piece> myPieces = new ArrayList<>();
        for (Piece piece : simPieces) {
            if (piece.color == currentColor) {
                myPieces.add(piece);
            }
        }

        class Move {
            Piece piece;
            int col, row;

            Move(Piece piece, int col, int row) {
                this.piece = piece;
                this.col = col;
                this.row = row;
            }
        }
        ArrayList<Move> validMoves = new ArrayList<>();

        for (Piece piece : myPieces) {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (piece.canMove(col, row)) {
                        int oldCol = piece.col;
                        int oldRow = piece.row;
                        Piece hit = piece.hittingP;

                        piece.col = col;
                        piece.row = row;
                        if (hit != null)
                            simPieces.remove(hit);

                        if (!isIllegal(piece)) {
                            validMoves.add(new Move(piece, col, row));
                        }

                        piece.col = oldCol;
                        piece.row = oldRow;
                        if (hit != null)
                            simPieces.add(hit);
                    }
                }
            }
        }

        if (validMoves.size() > 0) {
            Random rand = new Random();
            Move bestMove = validMoves.get(rand.nextInt(validMoves.size()));

            activeP = bestMove.piece;
            saveState();
            activeP.col = bestMove.col;
            activeP.row = bestMove.row;
            activeP.x = activeP.getX(activeP.col);
            activeP.y = activeP.getY(activeP.row);

            Piece capturedP = activeP.getHittingP(activeP.col, activeP.row);
            if (capturedP != null) {
                changedToGraveyard(capturedP);
                pieces.remove(capturedP);
                simPieces.remove(capturedP);
            }

            activeP.updatePosition();
            copyPieces(simPieces, pieces);

            if (canPromote()) {
                promoPieces.clear();
                promoPieces.add(new Queen(9, 9, currentColor));
                replacePawn(promoPieces.get(0));
                logMove(activeP, capturedP, promoPieces.get(0));
            } else {
                logMove(activeP, capturedP, null);
                changePlayer();
            }
        } else {
            gameOver = true;
        }
    }

    public void changedToGraveyard(Piece capturedP) {
        int count = 0;
        for (Piece piece : capturedPieces) {
            if (piece.color == capturedP.color) {
                count++;
            }
        }

        if (capturedP.color == WHITE) {
            capturedP.x = (8 * Board.SQUARE_SIZE) + (count % 2) * (Board.HALF_SQUARE_SIZE);
            capturedP.y = (0 * Board.SQUARE_SIZE) + (count / 2) * (Board.HALF_SQUARE_SIZE);
        } else {
            capturedP.x = (8 * Board.SQUARE_SIZE) + (count % 2) * (Board.HALF_SQUARE_SIZE);
            capturedP.y = (7 * Board.SQUARE_SIZE) - (count / 2) * (Board.HALF_SQUARE_SIZE);
        }
        capturedPieces.add(capturedP);
    }

    public boolean isIllegal(Piece activeP) {
        return isKingInCheck(currentColor);
    }

    public boolean isKingInCheck(int kingColor) {
        Piece King = getKing(kingColor);
        if (King == null)
            return false;

        for (Piece piece : simPieces) {
            if (piece.color != kingColor) {
                if (piece.canMove(King.col, King.row)) {
                    checkingP = piece;
                    return true;
                }
            }
        }
        checkingP = null;
        return false;
    }

    public Piece getKing(int color) {
        for (Piece piece : simPieces) {
            if (piece.type == Type.KING && piece.color == color) {
                return piece;
            }
        }
        return null;
    }

    public void checkCastling() {
        if (castlingP != null) {
            if (castlingP.col == 0) {
                castlingP.col += 3;
            } else if (castlingP.col == 7) {
                castlingP.col -= 2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    public void changePlayer() {
        if (currentColor == WHITE) {
            currentColor = BLACK;
            for (Piece piece : pieces) {
                if (piece.color == BLACK)
                    piece.twoStepped = false;
            }
        } else {
            currentColor = WHITE;
            for (Piece piece : pieces) {
                if (piece.color == WHITE)
                    piece.twoStepped = false;
            }
        }

        updatePositionHistory();

        if (halfmoveClock >= 100) {
            gameOver = true;
            return;
        }

        if (isThreefoldRepetition()) {
            gameOver = true;
            return;
        }

        if (isCheckmate()) {
            gameOver = true;
        } else if (isStalemate()) {
            gameOver = true;
        }

        activeP = null;
    }

    public boolean canPromote() {
        if (activeP.type == Type.PAWN) {
            if ((currentColor == WHITE && activeP.row == 0) || (currentColor == BLACK && activeP.row == 7)) {
                promoPieces.clear();
                promoPieces.add(new Bishop(9, 9, currentColor));
                promoPieces.add(new Knight(9, 9, currentColor));
                promoPieces.add(new Queen(9, 9, currentColor));
                promoPieces.add(new Rook(9, 9, currentColor));
                return true;
            }
        }
        return false;
    }

    public void replacePawn(Piece promoPiece) {
        promoPiece.col = activeP.col;
        promoPiece.row = activeP.row;
        promoPiece.x = activeP.x;
        promoPiece.y = activeP.y;
        promoPiece.preCol = activeP.col;
        promoPiece.preRow = activeP.row;

        Piece capturedRef = null;
        if (Math.abs(promoPiece.col - activeP.preCol) == 1) {
            capturedRef = new Piece(0, 0, 0);
        }

        int index = pieces.indexOf(activeP);
        if (index != -1) {
            pieces.set(index, promoPiece);
            simPieces.set(index, promoPiece);
        }

        logMove(activeP, capturedRef, promoPiece);
        promotion = false;
        activeP = null;
        changePlayer();
    }

    public boolean isCheckmate() {
        if (!isKingInCheck(currentColor))
            return false;

        ArrayList<Piece> testPieces = new ArrayList<>(simPieces);
        for (Piece piece : testPieces) {
            if (piece.color != currentColor)
                continue;

            int originalCol = piece.col;
            int originalRow = piece.row;

            for (int targetCol = 0; targetCol < 8; targetCol++) {
                for (int targetRow = 0; targetRow < 8; targetRow++) {
                    if (piece.canMove(targetCol, targetRow)) {
                        Piece capturedPiece = piece.hittingP;
                        int tempCol = piece.col;
                        int tempRow = piece.row;
                        piece.col = targetCol;
                        piece.row = targetRow;

                        ArrayList<Piece> tempList = new ArrayList<>();
                        for (Piece p : simPieces) {
                            if (p != capturedPiece)
                                tempList.add(p);
                        }
                        ArrayList<Piece> backupSimPieces = simPieces;
                        simPieces = tempList;

                        boolean stillInCheck = isKingInCheck(currentColor);
                        simPieces = backupSimPieces;
                        piece.col = tempCol;
                        piece.row = tempRow;

                        if (!stillInCheck)
                            return false;
                    }
                }
            }
            piece.col = originalCol;
            piece.row = originalRow;
        }
        return true;
    }

    public boolean isStalemate() {
        if (isKingInCheck(currentColor))
            return false;

        ArrayList<Piece> testPieces = new ArrayList<>(simPieces);
        for (Piece piece : testPieces) {
            if (piece.color != currentColor)
                continue;

            int originalCol = piece.col;
            int originalRow = piece.row;

            for (int targetCol = 0; targetCol < 8; targetCol++) {
                for (int targetRow = 0; targetRow < 8; targetRow++) {
                    if (piece.canMove(targetCol, targetRow)) {
                        Piece capturedPiece = piece.hittingP;
                        int tempCol = piece.col;
                        int tempRow = piece.row;
                        piece.col = targetCol;
                        piece.row = targetRow;

                        ArrayList<Piece> tempList = new ArrayList<>();
                        for (Piece p : simPieces) {
                            if (p != capturedPiece)
                                tempList.add(p);
                        }

                        ArrayList<Piece> backupSimPieces = simPieces;
                        simPieces = tempList;
                        boolean wouldBeInCheck = isKingInCheck(currentColor);
                        simPieces = backupSimPieces;

                        piece.col = tempCol;
                        piece.row = tempRow;

                        if (!wouldBeInCheck)
                            return false;
                    }
                }
            }
            piece.col = originalCol;
            piece.row = originalRow;
        }
        return true;
    }

    public void logMove(Piece p, Piece captured, Piece promoteTo) {
        String from = board.getSquareCoordinates(p.preCol, p.preRow).toUpperCase();
        String to = board.getSquareCoordinates(p.col, p.row).toUpperCase();
        String notation = from + to;

        if (promoteTo != null) {
            if (promoteTo.type == Type.QUEEN)
                notation += "Q";
            else if (promoteTo.type == Type.ROOK)
                notation += "R";
            else if (promoteTo.type == Type.BISHOP)
                notation += "B";
            else if (promoteTo.type == Type.KNIGHT)
                notation += "N";
        }

        moveList.add(notation);
    }

    private String getPositionHash() {
        StringBuilder hash = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = null;
                for (Piece piece : simPieces) {
                    if (piece.col == col && piece.row == row) {
                        p = piece;
                        break;
                    }
                }
                if (p == null) {
                    hash.append('.');
                } else {
                    char colorChar = (p.color == WHITE) ? 'w' : 'b';
                    char typeChar = ' ';
                    if (p.type == Type.PAWN)
                        typeChar = 'P';
                    else if (p.type == Type.ROOK)
                        typeChar = 'R';
                    else if (p.type == Type.KNIGHT)
                        typeChar = 'N';
                    else if (p.type == Type.BISHOP)
                        typeChar = 'B';
                    else if (p.type == Type.QUEEN)
                        typeChar = 'Q';
                    else if (p.type == Type.KING)
                        typeChar = 'K';
                    hash.append(colorChar).append(typeChar);
                }
            }
        }
        hash.append('|').append(currentColor);
        return hash.toString();
    }

    private void updatePositionHistory() {
        String posHash = getPositionHash();
        positionHistory.put(posHash, positionHistory.getOrDefault(posHash, 0) + 1);
    }

    private boolean isThreefoldRepetition() {
        String currentHash = getPositionHash();
        return positionHistory.getOrDefault(currentHash, 0) >= 3;
    }

    // Inner class for history
    public class HistoryMove {
        public ArrayList<Piece> pieceList;
        public ArrayList<Piece> capturedList;
        public int wTime, bTime;
        public int turn;
        public Piece castlingP;
    }
}
