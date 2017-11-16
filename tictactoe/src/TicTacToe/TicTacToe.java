package TicTacToe;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.*;

public class TicTacToe implements Runnable
{
    private Socket socket;
    private ServerSocket serverSocket;
    private DataOutputStream data_os;
    private DataInputStream data_is;
    private String ip = "localhost";
    private int port = 22222;

    private boolean circle = true;
    private boolean yourMove = false;
    private boolean accepted = false;
    private boolean win = false;
    private boolean draw = false;
    private boolean enemyWin = false;
    private boolean cannotComunicate = false;

    private int spaceLength = 160;
    private int first_Spot = -1;
    private int second_Spot = -1;
    private int errorsCounter = 0;

    private String[] spaces = new String[9];
    private JFrame frame;
    private final int WIDTH = 506;
    private final int HEIGHT = 530;
    private Painter painter;
    private Thread thread;

    private BufferedImage board;
    private BufferedImage blue_X;
    private BufferedImage red_X;
    private BufferedImage blue_Circle;
    private BufferedImage red_Circle;

    private Font font = new Font("Verdana", Font.BOLD, 32);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);

    private String waitText = "Oczekiwanie na gracza";
    private String cannotComunicateText = "Nie mozna polaczyc";
    private String wonText = "Wygrales!";
    private String enemyWonText = "Przegrales!";
    private String drawText = "Remis!";

    private int[][] winFields = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };


    public TicTacToe()
    {
        ip = JOptionPane.showInputDialog(null, "Wprowadź IP serwera :", "Kółko i Krzyżyk", JOptionPane.INFORMATION_MESSAGE);
        port = Integer.parseInt(JOptionPane.showInputDialog(null, "Wprowadź port serwera :", "Kółko i Krzyżyk", JOptionPane.INFORMATION_MESSAGE));
        while (port < 1 || port > 65535)
        {
            String error;
            error = "Wprowadzono zły port, Wprowadź inny";
            JOptionPane.showMessageDialog(null, error);
            port = Integer.parseInt(JOptionPane.showInputDialog(null, "Wprowadź port serwera :", "Kółko i Krzyżyk", JOptionPane.INFORMATION_MESSAGE));
        }

        loadTextures();

        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        if (!connect()) startServer();

        frame = new JFrame();
        frame.setTitle("Kółko i Krzyżyk");
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        thread = new Thread(this, "TicTacToe");
        thread.start();
    }

    public void run()
    {
        while (true)
        {
            checkForErrors();
            painter.repaint();

            if (!circle && !accepted)
            {
                waitForServerRequest();
            }

        }
    }

    private void rendering(Graphics g)
    {
        g.drawImage(board, 0, 0, null);
        if (cannotComunicate)
        {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(cannotComunicateText);
            g.drawString(cannotComunicateText, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            return;
        }

        if (accepted)
        {
            for (int i = 0; i < spaces.length; i++)
            {
                if (spaces[i] != null)
                {
                    if (spaces[i].equals("X"))
                    {
                        if (circle)
                        {
                            g.drawImage(red_X, (i % 3) * spaceLength + 10 * (i % 3), (int) (i / 3) * spaceLength + 10 * (int) (i / 3), null);
                        }
                        else
                        {
                            g.drawImage(blue_X, (i % 3) * spaceLength + 10 * (i % 3), (int) (i / 3) * spaceLength + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O"))
                    {
                        if (circle)
                        {
                            g.drawImage(blue_Circle, (i % 3) * spaceLength + 10 * (i % 3), (int) (i / 3) * spaceLength + 10 * (int) (i / 3), null);
                        }
                        else
                        {
                            g.drawImage(red_Circle, (i % 3) * spaceLength + 10 * (i % 3), (int) (i / 3) * spaceLength + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }
            if (win || enemyWin)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(first_Spot % 3 * spaceLength + 10 * first_Spot % 3 + spaceLength / 2, (int) (first_Spot / 3) * spaceLength + 10 * (int) (first_Spot / 3) + spaceLength / 2, second_Spot % 3 * spaceLength + 10 * second_Spot % 3 + spaceLength / 2, (int) (second_Spot / 3) * spaceLength + 10 * (int) (second_Spot / 3) + spaceLength / 2);

                g.setColor(Color.ORANGE);
                g.setFont(largerFont);
                if (win)
                {
                    int stringWidth = g2.getFontMetrics().stringWidth(wonText);
                    g.drawString(wonText, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);


                } else if (enemyWin)
                {
                    int stringWidth = g2.getFontMetrics().stringWidth(enemyWonText);
                    g.drawString(enemyWonText, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                }
            }
            if (draw)
            {
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(Color.ORANGE);
                g.setFont(largerFont);
                int stringWidth = g2.getFontMetrics().stringWidth(drawText);
                g.drawString(drawText, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            }
        } else
        {
            g.setColor(Color.ORANGE);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitText);
            g.drawString(waitText, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
        }

    }

    private void checkForWin()
    {
        for (int i = 0; i < winFields.length; i++)
        {
            if (circle)
            {
                if (spaces[winFields[i][0]] == "O" && spaces[winFields[i][1]] == "O" && spaces[winFields[i][2]] == "O")
                {
                    first_Spot = winFields[i][0];
                    second_Spot = winFields[i][2];
                    win = true;
                }
            }
            else
            {
                if (spaces[winFields[i][0]] == "X" && spaces[winFields[i][1]] == "X" && spaces[winFields[i][2]] == "X")
                {
                    first_Spot = winFields[i][0];
                    second_Spot = winFields[i][2];
                    win = true;
                }
            }
        }
    }

    private void checkForOpponentWin()
    {
        for (int i = 0; i < winFields.length; i++)
        {
            if (circle)
            {
                if (spaces[winFields[i][0]] == "X" && spaces[winFields[i][1]] == "X" && spaces[winFields[i][2]] == "X")
                {
                    first_Spot = winFields[i][0];
                    second_Spot = winFields[i][2];
                    enemyWin = true;
                }
            }
            else
            {
                if (spaces[winFields[i][0]] == "O" && spaces[winFields[i][1]] == "O" && spaces[winFields[i][2]] == "O")
                {
                    first_Spot = winFields[i][0];
                    second_Spot = winFields[i][2];
                    enemyWin = true;
                }
            }
        }
    }

    private void checkForDraw()
    {
        for (int i = 0; i < spaces.length; i++)
        {
            if (spaces[i] == null)
            {
                return;
            }
        }
        draw = true;
    }

    private void checkForErrors()
    {
        if (errorsCounter >= 5) cannotComunicate = true;

        if (!yourMove && !cannotComunicate)
        {
            try
            {
                int space = data_is.readInt();
                if (circle) spaces[space] = "X";
                else spaces[space] = "O";
                checkForOpponentWin();
                checkForDraw();
                yourMove = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                errorsCounter++;
            }
        }
    }

    private void waitForServerRequest()
    {
        Socket socket = null;
        try
        {
            socket = serverSocket.accept();
            data_os = new DataOutputStream(socket.getOutputStream());
            data_is = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("Nawiązano połączenie !");
            JOptionPane.showMessageDialog(null, "Nawiązano połączenie !", "Kółko i Krzyżyk", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private boolean connect()
    {
        try
        {
            socket = new Socket(ip, port);
            data_os = new DataOutputStream(socket.getOutputStream());
            data_is = new DataInputStream(socket.getInputStream());
            accepted = true;
        }
        catch (IOException e)
        {
            System.out.println("Proba polaczenia z " + ip + ":" + port);
            return false;
        }
        System.out.println("Nawiazano polaczenie!");
        JOptionPane.showMessageDialog( null, "Nawiązano połączenie !", "Kółko i Krzyżyk", JOptionPane.INFORMATION_MESSAGE);

        return true;
    }

    private void startServer()
    {
        try
        {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        yourMove = true;
        circle = false;
    }

    private void loadTextures()
    {
        try
        {
            board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
            blue_X = ImageIO.read(getClass().getResourceAsStream("/blueX.png"));
            blue_Circle = ImageIO.read(getClass().getResourceAsStream("/blueCircle.png"));
            red_X = ImageIO.read(getClass().getResourceAsStream("/redX.png"));
            red_Circle = ImageIO.read(getClass().getResourceAsStream("/redCircle.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public static void main(String[] args)
    {
        TicTacToe ticTacToe = new TicTacToe();
    }

    private class Painter extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;

        public Painter() {
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            rendering(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (accepted) {
                if (yourMove && !cannotComunicate && !win && !enemyWin) {
                    int x = e.getX() / spaceLength;
                    int y = e.getY() / spaceLength;
                    y *= 3;
                    int position = x + y;

                    if (spaces[position] == null) {
                        if (!circle) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourMove = false;
                        repaint();
                        Toolkit.getDefaultToolkit().sync();

                        try {
                            data_os.writeInt(position);
                            data_os.flush();
                        } catch (IOException e1) {
                            errorsCounter++;
                            e1.printStackTrace();
                        }

                        System.out.println("Przeciwnik wykonal ruch");
                        checkForWin();
                        checkForDraw();

                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

    }

}
