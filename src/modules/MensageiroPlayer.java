package modules;

public class MensageiroPlayer {
	
	private static MensageiroPlayer mensageiro = null;
	
	private MensageiroPlayer( ) {
		
	}
	
	private String mensagem = "";
	private int destino = 99;
	private double posX = 0;
	private double posY = 0;
	
	public static MensageiroPlayer getInstance() {
		if(mensageiro == null) {
			mensageiro = new MensageiroPlayer();
		}
		return mensageiro;
	}
	
	public String getMensagem() {
		String msg = mensagem;
		mensagem = "";
		return msg;
	}
	
	public int getDestino() {
		int dest = destino;
		destino = 99;
		return dest;
	}
	
	public double getPosX() {
		double pX = posX;
		posX = 0;
		return pX;
	}
	
	public double getPosY() {
		double pY = posY;
		posY = 0;
		return pY;
	}
	
	public void postMensagem(String mensagem, int destino, double x, double y) {
		this.mensagem = mensagem;
		this.destino = destino;
		this.posX = x;
		this.posY = y;
	}
	

}
