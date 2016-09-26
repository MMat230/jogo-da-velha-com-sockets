import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
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
import javax.swing.JFrame;
import javax.swing.JPanel;

public class JogoDaVelha implements Runnable {
	
	/*
	 * Variáveis usadas para o controle da conexão e dos dados.
	 */
	private String ip = "localhost";
	private int porta = 22222;
	private Socket socket;
	private Thread thread;
	private DataOutputStream dos;
	private DataInputStream dis;
	private ServerSocket serverSocket;

	/*
	 * Imagens usadas no jogo.
	 */
	private BufferedImage board;
	private BufferedImage grumpyCat;
	private BufferedImage doge;

	/*
	 * Variáveis que fazem o controle do Jogo.
	 */
	private boolean suaVez = false;
	private boolean circulo = true;
	private boolean conexaoAceita = false;
	private boolean conexaoBemSucedidaComOponente = true;
	private int erros = 0;
	private boolean voceVenceu = false;
	private boolean vocePerdeu = false;
	private boolean empate = false;

	/*
	 * Variáveis relacionadas ao controle dos "quadrados" no programa.
	 */
	private String[] quadrados = new String[9];
	private int tamanhoQuadrado = 160;
	private int primeiraPosicao = -1;
	private int segundaPosicao = -1;

	
	
	/*
	 * Configuração da janela e tipos de fonte usadas no programa.
	 */
	private JFrame frame;
	private Painter painter;
	private final int largura = 506;
	private final int altura = 527;
	private Font fonte = new Font("Verdana", Font.BOLD, 32);
	private Font fonteGrande = new Font("Verdana", Font.BOLD, 20);
	private Font fontePequena = new Font("Verdana", Font.BOLD, 50);

	/*
	 * Mensagens usadas no programa.
	 */
	private String esperandoString = "Esperando o outro jogador";
	private String naoFoiPossivelConectarComOponenteString = "Não foi possível conectar com o oponente.";
	private String vencedorString = "Você venceu!";
	private String perdedorString = "Você perdeu!";
	private String empateString = "Empate";

	/* 
	 * Sequência de combinações que correspondem a vitória.
	 */
	private int[][] vitorias = new int[][] {
		{0, 1, 2}, 
		{3, 4, 5}, 
		{6, 7, 8}, 
		{0, 3, 6}, 
		{1, 4, 7}, 
		{2, 5, 8}, 
		{0, 4, 8}, 
		{2, 4, 6}};

	/*
	 * Construtor da classe.
	 */
	public JogoDaVelha() {
		carregaImagens();
		painter = new Painter();
		painter.setPreferredSize(new Dimension(largura, altura));

		// Inicializa o servidor se ainda não estiver conectado
		if (!conectaNoServidor()) 
			inicializaServidor();

		// Inicializa janela
		frame = new JFrame();
		frame.setTitle("Jogo da Velha - Redes de Computadores 2016");
		frame.setContentPane(painter);
		frame.setSize(largura, altura);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		// Cria e inicia uma thread que controla a partida.
		thread = new Thread(this, "JogoDaVelha");
		thread.start();
	}

	public void run() {
		while (true) {
			marcaXouO();
			painter.repaint();

			if (!circulo && !conexaoAceita) {
				listenServerRequest();
			}

		}
	}

	private void render(Graphics g) {
		g.drawImage(board, 0, 0, null);
		if (!conexaoBemSucedidaComOponente) {
			g.setColor(Color.RED);
			g.setFont(fonteGrande);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int larguraDaString = g2.getFontMetrics().stringWidth(naoFoiPossivelConectarComOponenteString);
			g.drawString(naoFoiPossivelConectarComOponenteString, largura / 2 - larguraDaString / 2, altura / 2);
			return;
		}

		if (conexaoAceita) {
			for (int i = 0; i < quadrados.length; i++) {
				if (quadrados[i] != null) {
					if (quadrados[i].equals("X")) {
							g.drawImage(grumpyCat, (i % 3) * tamanhoQuadrado + 10 * (i % 3), (int) (i / 3) * tamanhoQuadrado + 10 * (int) (i / 3), null);
						
					} else if (quadrados[i].equals("O")) {
							g.drawImage(doge, (i % 3) * tamanhoQuadrado + 10 * (i % 3), (int) (i / 3) * tamanhoQuadrado + 10 * (int) (i / 3), null);
						
					}
				}
			}
			if (voceVenceu || vocePerdeu) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(10));
				g.setColor(Color.BLACK);
				g.drawLine(primeiraPosicao % 3 * tamanhoQuadrado + 10 * primeiraPosicao % 3 + tamanhoQuadrado / 2, (int) (primeiraPosicao / 3) * tamanhoQuadrado + 10 * (int) (primeiraPosicao / 3) + tamanhoQuadrado / 2, segundaPosicao % 3 * tamanhoQuadrado + 10 * segundaPosicao % 3 + tamanhoQuadrado / 2, (int) (segundaPosicao / 3) * tamanhoQuadrado + 10 * (int) (segundaPosicao / 3) + tamanhoQuadrado / 2);

				g.setColor(Color.RED);
				g.setFont(fontePequena);
				if (voceVenceu) {
					int stringWidth = g2.getFontMetrics().stringWidth(vencedorString);
					g.drawString(vencedorString, largura / 2 - stringWidth / 2, altura / 2);
				} else if (vocePerdeu) {
					int stringWidth = g2.getFontMetrics().stringWidth(perdedorString);
					g.drawString(perdedorString, largura / 2 - stringWidth / 2, altura / 2);
				}
			}
			if (empate) {
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.BLACK);
				g.setFont(fontePequena);
				int stringWidth = g2.getFontMetrics().stringWidth(empateString);
				g.drawString(empateString, largura / 2 - stringWidth / 2, altura / 2);
			}
		} else {
			g.setColor(Color.RED);
			g.setFont(fonte);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(esperandoString);
			g.drawString(esperandoString, largura / 2 - stringWidth / 2, altura / 2);
		}

	}

	private void marcaXouO() {
		if (erros >= 10) 
			conexaoBemSucedidaComOponente = false;

		if (!suaVez && conexaoBemSucedidaComOponente) {
			try {
				int letra = dis.readInt();
				if (circulo) 
					quadrados[letra] = "X";
				else 
					quadrados[letra] = "O";
				checaSeOponenteVenceu();
				checaPorEmpate();
				suaVez = true;
			} catch (IOException e) {
				e.printStackTrace();
				erros++;
			}
		}
	}

	/*
	 * Percorre todas as células para identificar se o jogador atual venceu a partida.
	 */
	private void checaSeVenceu() {
		for (int i = 0; i < vitorias.length; i++) {
			if (circulo) {
				if (quadrados[vitorias[i][0]] == "O" && quadrados[vitorias[i][1]] == "O" && quadrados[vitorias[i][2]] == "O") {
					primeiraPosicao = vitorias[i][0];
					segundaPosicao = vitorias[i][2];
					voceVenceu = true;
				}
			} else {
				if (quadrados[vitorias[i][0]] == "X" && quadrados[vitorias[i][1]] == "X" && quadrados[vitorias[i][2]] == "X") {
					primeiraPosicao = vitorias[i][0];
					segundaPosicao = vitorias[i][2];
					voceVenceu = true;
				}
			}
		}
	}
	
	/*
	 * Percorre todas as células para identificar se o jogador atual venceu a partida.
	 */
	private void checaSeOponenteVenceu() {
		for (int i = 0; i < vitorias.length; i++) {
			if (circulo) {
				if (quadrados[vitorias[i][0]] == "X" && quadrados[vitorias[i][1]] == "X" && quadrados[vitorias[i][2]] == "X") {
					primeiraPosicao = vitorias[i][0];
					segundaPosicao = vitorias[i][2];
					vocePerdeu = true;
				}
			} else {
				if (quadrados[vitorias[i][0]] == "O" && quadrados[vitorias[i][1]] == "O" && quadrados[vitorias[i][2]] == "O") {
					primeiraPosicao = vitorias[i][0];
					segundaPosicao = vitorias[i][2];
					vocePerdeu = true;
				}
			}
		}
	}

	/*
	 * Checa se o jogo terminou empatado.
	 */
	private void checaPorEmpate() {
		for (int i = 0; i < quadrados.length; i++) {
			if (quadrados[i] == null) {
				return;
			}
		}
		empate = true;
	}

	/*
	 * Tenta conectar com o servidor.
	 */
	private void listenServerRequest() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			conexaoAceita = true;
			System.out.println("A conexão foi aceita com sucesso.");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Checa se uma conexão já foi estabelecida.
	 */
	private boolean conectaNoServidor() {
		try {
			socket = new Socket(ip, porta);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			conexaoAceita = true;
		} 
		catch (IOException e) {
			return false;
		}
		System.out.println("Conexão bem sucedida com o servidor.");
		return true;
	}

	/*
	 * Tenta inicializar o servidor.
	 */
	private void inicializaServidor() {
		try {
			serverSocket = new ServerSocket(porta, 8, InetAddress.getByName(ip));
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		suaVez = true;
		circulo = false;
	}

	/*
	 * Método para carregar as imagens exibidas no jogo.
	 */
	private void carregaImagens() {
		try {
			board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
			grumpyCat = ImageIO.read(getClass().getResourceAsStream("/grumpycat.jpg"));
			doge = ImageIO.read(getClass().getResourceAsStream("/doge2.jpg"));
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Método principal da classe. Apenas chama seu construtor.
	 */
	public static void main(String[] args) {
		JogoDaVelha jogoDaVelha = new JogoDaVelha();
	}

	
	/*
	 * A classe "Painter" é responsável por desenhar na tela e 
	 * identificar os cliques do mouse no jogo.
	 */
	private class Painter extends JPanel implements MouseListener {

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (conexaoAceita) {
				if (suaVez && conexaoBemSucedidaComOponente && !voceVenceu && !vocePerdeu) {
					int x = e.getX() / tamanhoQuadrado;
					int y = e.getY() / tamanhoQuadrado;
					y *= 3;
					int posicao = x + y;

					if (quadrados[posicao] == null) {
						if (!circulo) 
							quadrados[posicao] = "X";
						else 
							quadrados[posicao] = "O";
						
						suaVez = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							dos.writeInt(posicao);
							dos.flush();
						} catch (IOException e1) {
							erros++;
							e1.printStackTrace();
						}
						checaSeVenceu();
						checaPorEmpate();
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
